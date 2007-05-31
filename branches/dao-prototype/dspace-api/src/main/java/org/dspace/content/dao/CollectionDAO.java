/*
 * CollectionDAO.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Browse;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.content.uri.dao.PersistentIdentifierDAO;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.workflow.WorkflowItem;

/**
 * @author James Rutherford
 */
public abstract class CollectionDAO extends ContentDAO
{
    protected static Logger log = Logger.getLogger(CollectionDAOPostgres.class);

    protected Context context;
    protected ItemDAO itemDAO;
    protected PersistentIdentifierDAO identifierDAO;

    public abstract Collection create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    public Collection create(int id) throws AuthorizeException
    {
        try
        {
            Collection collection = new Collection(context, id);

            // Create a default persistent identifier for this Collection, and
            // add it to the in-memory Colleciton object.
            PersistentIdentifier identifier = identifierDAO.create(collection);
            collection.addPersistentIdentifier(identifier);

            // create the default authorization policy for collections
            // of 'anonymous' READ
            Group anonymousGroup = Group.find(context, 0);

            ResourcePolicy policy = ResourcePolicy.create(context);
            policy.setResource(collection);
            policy.setAction(Constants.READ);
            policy.setGroup(anonymousGroup);
            policy.update();

            // now create the default policies for submitted items
            policy = ResourcePolicy.create(context);
            policy.setResource(collection);
            policy.setAction(Constants.DEFAULT_ITEM_READ);
            policy.setGroup(anonymousGroup);
            policy.update();

            policy = ResourcePolicy.create(context);
            policy.setResource(collection);
            policy.setAction(Constants.DEFAULT_BITSTREAM_READ);
            policy.setGroup(anonymousGroup);
            policy.update();

            update(collection);

            HistoryManager.saveHistory(context, collection,
                    HistoryManager.CREATE, context.getCurrentUser(),
                    context.getExtraLogInfo());

            log.info(LogManager.getHeader(context, "create_collection",
                    "collection_id=" + collection.getID())
                    + ",uri=" + collection.getPersistentIdentifier().getCanonicalForm());
            
            return collection;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Collection retrieve(int id)
    {
        return (Collection) context.fromCache(Collection.class, id);
    }

    public void update(Collection collection) throws AuthorizeException
    {
        // Check authorisation
        collection.canEdit();

        HistoryManager.saveHistory(context, this, HistoryManager.MODIFY,
                context.getCurrentUser(), context.getExtraLogInfo());

        log.info(LogManager.getHeader(context, "update_collection",
                "collection_id=" + collection.getID()));

        try
        {
            DSIndexer.reIndexContent(context, collection);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        ItemIterator iterator = collection.getItems();
        try
        {
            while (iterator.hasNext())
            {
                Item item = iterator.next();
                link(collection, item); // create mapping row in the db
                itemDAO.update(item);   // save changes to item
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void delete(int id) throws AuthorizeException
    {
        try
        {
            Collection collection = retrieve(id);
            this.update(collection); // Sync in-memory object before removal

            log.info(LogManager.getHeader(context, "delete_collection",
                    "collection_id=" + collection.getID()));

            // remove from index
            DSIndexer.unIndexContent(context, collection);

            // Remove from cache
            context.removeCached(collection, collection.getID());

            HistoryManager.saveHistory(context, collection,
                    HistoryManager.REMOVE, context.getCurrentUser(),
                    context.getExtraLogInfo());

            // Remove Template Item
            collection.removeTemplateItem();
            
            // Remove items
            ItemIterator items = collection.getAllItems();

            while (items.hasNext())
            {
                Item item = items.next();
                
                if (item.isOwningCollection(collection))
                {
                    // the collection to be deletd is the owning collection,
                    // thus remove the item from all collections it belongs to
                    int itemId = item.getID();
                    Collection[] collections = item.getCollections();
                    for (int i = 0; i < collections.length; i++)
                    {
                        ArchiveManager.move(context, item, collections[i],
                                null);

                        //notify Browse of removing item.
                        Browse.itemRemoved(context, itemId);
                    }
                } 
                else
                {
                    // the item was only mapped to this collection, so just
                    // remove it
                    ArchiveManager.move(context, item, collection, null);

                    //notify Browse of removing item mapping. 
                    Browse.itemChanged(context, item);
                }
            }

            // Delete bitstream logo
            collection.setLogo(null);

            // Remove all authorization policies
            AuthorizeManager.removeAllPolicies(context, collection);

            // Remove any WorkflowItems
            for (WorkflowItem wfi :
                    WorkflowItem.findByCollection(context, collection))
            {
                // remove the workflowitem first, then the item
                wfi.deleteWrapper();
                itemDAO.delete(wfi.getItem().getID());
            }

            // Remove any WorkspaceItems
            for (WorkspaceItem wsi :
                    WorkspaceItem.findByCollection(context, collection))
            {
                wsi.deleteAll();
            }

            // Remove any associated groups - must happen after deleting
            List<Group> groups = new ArrayList<Group>();
            for (Group g : collection.getWorkflowGroups())
            {
                groups.add(g);
            }
            groups.add(collection.getAdministrators());
            groups.add(collection.getSubmitters());

            for (Group g : groups)
            {
                if (g != null)
                {
                    g.delete();
                }
            }
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public abstract List<Collection> getCollections();
    public abstract List<Collection> getCollectionsByAuthority(Community parent,
            int actionID);
    public abstract List<Collection> getParentCollections(Item item);
    public abstract List<Collection> getChildCollections(Community community);

    public void link(Collection collection, Item item)
        throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, collection,
                Constants.ADD);

        log.info(LogManager.getHeader(context, "add_item",
                    "collection_id=" + collection.getID() +
                    ",item_id=" + item.getID()));

        // If we're adding the Item to the Collection, we bequeath the
        // policies unto it.
        try
        {
            AuthorizeManager.inheritPolicies(context, collection, item);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

    }

    public void unlink(Collection collection, Item item)
        throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, collection,
                Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_item",
                "collection_id=" + collection.getID() + 
                ",item_id=" + item.getID()));
    }

    // Everything below this line is debatable & needs rethinking

    public abstract int itemCount(Collection collection);
}

