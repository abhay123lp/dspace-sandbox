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
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.content.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.search.DSIndexer;
import org.dspace.workflow.WorkflowItem;
import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;

/**
 * @author James Rutherford
 */
public abstract class CollectionDAO extends ContentDAO
    implements CRUD<Collection>, Link<Collection, Item>
{
    protected static Logger log = Logger.getLogger(CollectionDAO.class);

    protected Context context;
    protected BitstreamDAO bitstreamDAO;
    protected ItemDAO itemDAO;
    protected GroupDAO groupDAO;
    protected ExternalIdentifierDAO identifierDAO;

    /**
     * The allowed metadata fields for Collections are defined in the following
     * enum. This should make reading / writing all metadatafields a lot less
     * error-prone, not to mention concise and tidy!
     */
    protected enum CollectionMetadataField
    {
        NAME ("name"),
        SHORT_DESCRIPTION ("short_description"),
        PROVENANCE_DESCRIPTION ("provenance_description"),
        LICENSE ("license"),
        COPYRIGHT_TEXT ("copyright_text"),
        SIDE_BAR_TEXT ("side_bar_text");

        private String name;

        private CollectionMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public CollectionDAO(Context context)
    {
        this.context = context;

        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);
        identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
    }

    public abstract Collection create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final Collection create(Collection collection)
        throws AuthorizeException
    {
        // Create a default persistent identifier for this Collection, and
        // add it to the in-memory Colleciton object.
        ExternalIdentifier identifier = identifierDAO.create(collection);
        collection.addExternalIdentifier(identifier);

        // create the default authorization policy for collections
        // of 'anonymous' READ
        Group anonymousGroup = groupDAO.retrieve(0);

        int actions[] = {
            Constants.READ,
            Constants.DEFAULT_ITEM_READ,
            Constants.DEFAULT_BITSTREAM_READ
        };

        for (int action : actions)
        {
            ResourcePolicy policy = ResourcePolicy.create(context);
            policy.setResource(collection);
            policy.setAction(action);
            policy.setGroup(anonymousGroup);
            policy.update();
        }

        update(collection);

        log.info(LogManager.getHeader(context, "create_collection",
                "collection_id=" + collection.getId())
                + ",uri=" +
                collection.getIdentifier().getCanonicalForm());
        
        return collection;
    }

    public Collection retrieve(int id)
    {
        return (Collection) context.fromCache(Collection.class, id);
    }

    public Collection retrieve(UUID uuid)
    {
        return null;
    }

    public void update(Collection collection) throws AuthorizeException
    {
        // Check authorisation
        collection.canEdit();

        log.info(LogManager.getHeader(context, "update_collection",
                "collection_id=" + collection.getId()));

        try
        {
            DSIndexer.reIndexContent(context, collection);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
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
            update(collection); // Sync in-memory object before removal

            log.info(LogManager.getHeader(context, "delete_collection",
                    "collection_id=" + id));

            context.removeCached(collection, id);

            DSIndexer.unIndexContent(context, collection);

            // Remove Template Item
            collection.removeTemplateItem();
            
            // Remove items
            for (Item item : itemDAO.getItemsByCollection(collection))
            {
                if (item.isOwningCollection(collection))
                {
                    // the collection to be deleted is the owning collection,
                    // thus remove the item from all collections it belongs to
                    for (Collection c : getParentCollections(item))
                    {
                        // Move the item out of all parent collections
                        ArchiveManager.move(context, item, c, null);
                    }
                    //notify Browse of removing item.
                    IndexBrowse ib = new IndexBrowse(context);
                    ib.itemRemoved(item);
                    itemDAO.delete(item.getId());
                } 
                else
                {
                    // the item was only mapped to this collection, so just
                    // remove it
                    ArchiveManager.move(context, item, collection, null);

                    //notify Browse of removing item mapping. 
                    IndexBrowse ib = new IndexBrowse(context);
                    ib.indexItem(item);
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
                itemDAO.delete(wfi.getItem().getId());
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
        catch (BrowseException e)
        {
            throw new RuntimeException(e);
        }
    }

    public abstract List<Collection> getCollections();

    /**
     * Returns a List of collections that user has a given permission on.
     * Useful for trimming 'select to collection' list, or figuring out which
     * collections a person is an editor for.
     */
    public List<Collection> getCollectionsByAuthority(Community parent,
            int actionID)
    {
        List<Collection> results = new ArrayList<Collection>();
        List<Collection> collections = null;

        if (parent != null)
        {
            collections = getChildCollections(parent);
        }
        else
        {
            collections = getCollections();
        }

        for (Collection collection : collections)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    collection, actionID))
            {
                results.add(collection);
            }
        }

        return results;
    }

    public abstract List<Collection> getParentCollections(Item item);
    public abstract List<Collection> getChildCollections(Community community);

    /**
     * Returns a list of all the Collections that are *not* the parent of the
     * given Item. This would be unnecessary if we used Sets instead of Lists.
     *
     * @param item The Item
     * @return All Collections that are not parent to the given Item.
     */
    public List<Collection> getCollectionsNotLinked(Item item)
    {
        List<Collection> collections = new ArrayList<Collection>();

        for (Collection collection : getCollections())
        {
            if (!linked(collection, item))
            {
                collections.add(collection);
            }
        }
        
        return collections;
    }

    /**
     * Create a storage layer association between the given Item and
     * Collection.
     */
    public void link(Collection collection, Item item)
        throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, collection,
                Constants.ADD);

        log.info(LogManager.getHeader(context, "add_item",
                    "collection_id=" + collection.getId() +
                    ",item_id=" + item.getId()));

        // If we're adding the Item to the Collection, we bequeath the
        // policies unto it.
        AuthorizeManager.inheritPolicies(context, collection, item);
    }

    /**
     * Remove any existing storage layer association between the given Item and
     * Collection.
     */
    public void unlink(Collection collection, Item item)
        throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, collection,
                Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_item",
                "collection_id=" + collection.getId() + 
                ",item_id=" + item.getId()));

        if (getParentCollections(item).size() == 0)
        {
            // make the right to remove the item explicit because the implicit
            // relation has been removed. This only has to concern the
            // currentUser because he started the removal process and he will
            // end it too. also add right to remove from the item to remove
            // it's bundles.
            AuthorizeManager.addPolicy(context, item, Constants.DELETE,
                    context.getCurrentUser());
            AuthorizeManager.addPolicy(context, item, Constants.REMOVE,
                    context.getCurrentUser());

            itemDAO.delete(item.getId());
        }
    }

    /**
     * Determine whether or not there is an established link between the given
     * Item and Collection in the storage layer.
     */
    public abstract boolean linked(Collection collection, Item item);

    // Everything below this line is debatable & needs rethinking

    public abstract int itemCount(Collection collection);
}
