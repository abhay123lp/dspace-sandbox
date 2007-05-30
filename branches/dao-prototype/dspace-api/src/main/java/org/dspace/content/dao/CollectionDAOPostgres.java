/*
 * CollectionDAOPostgres.java
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.content.uri.dao.PersistentIdentifierDAO;
import org.dspace.content.uri.dao.PersistentIdentifierDAOFactory;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;

public class CollectionDAOPostgres extends ContentDAO implements CollectionDAO
{
    private static Logger log = Logger.getLogger(CollectionDAOPostgres.class);

    private Context context;
    private ItemDAO itemDAO;
    private PersistentIdentifierDAO identifierDAO;

    /**
     * The allowed metadata fields for Collections are defined in the following
     * enum. This should make reading / writing all metadatafields a lot less
     * error-prone, not to mention concise and tidy!
     *
     * FIXME: Do we want this exposed anywhere else? Probably not...
     */
    private enum CollectionMetadataField
    {
        NAME	                ("name"),
        SHORT_DESCRIPTION	    ("short_description"),
        PROVENANCE_DESCRIPTION	("provenance_description"),
        LICENSE	                ("license"),
        COPYRIGHT_TEXT	        ("copyright_text"),
        SIDE_BAR_TEXT	        ("side_bar_text");

        private String name;

        CollectionMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return this.name;
        }
    }

    public CollectionDAOPostgres(Context context)
    {
        if (context != null)
        {
            this.context = context;
            this.itemDAO = ItemDAOFactory.getInstance(context);
            this.identifierDAO =
                PersistentIdentifierDAOFactory.getInstance(context);
        }
    }

    public Collection create() throws AuthorizeException
    {
        try
        {
            Collection collection = null;
            TableRow row = DatabaseManager.create(context, "collection");
            int id = row.getIntColumn("collection_id");
            collection = new Collection(context, id);

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

    /**
     * Retrieve the Collection with the given ID from the database (or from the
     * Context cache, if it's there).
     */
    public Collection retrieve(int id)
    {
        // First check the cache
        Collection fromCache =
            (Collection) context.fromCache(Collection.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "collection", id);

            if (row == null)
            {
                log.warn("collection " + id + " not found");
                return null;
            }

            Collection collection = new Collection(context, id);
            populateCollectionFromTableRow(collection, row);

            List<PersistentIdentifier> identifiers =
                identifierDAO.getPersistentIdentifiers(collection);
            collection.setPersistentIdentifiers(identifiers);

            context.cache(collection, id);

            return collection;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void update(Collection collection) throws AuthorizeException
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "collection", collection.getID());

            if (row != null)
            {
                update(collection, row);
            }
            else
            {
                throw new RuntimeException("Didn't find collection " +
                        collection.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Collection collection, TableRow row)
        throws AuthorizeException
    {
        try
        {
            // Check authorisation
            collection.canEdit();

            HistoryManager.saveHistory(context, this, HistoryManager.MODIFY,
                    context.getCurrentUser(), context.getExtraLogInfo());

            log.info(LogManager.getHeader(context, "update_collection",
                    "collection_id=" + collection.getID()));

            populateTableRowFromCollection(collection, row);

            DatabaseManager.update(context, row);

            DSIndexer.reIndexContent(context, collection);

            ItemIterator iterator = collection.getItems();
            while (iterator.hasNext())
            {
                Item item = iterator.next();
                link(collection, item); // create mapping row in the db
                itemDAO.update(item);   // save changes to item
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

    /**
     * Delete the collection, including the metadata and logo. Items that are
     * then orphans are deleted. Groups associated with this collection
     * (workflow participants and submitters) are NOT deleted.
     */
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

            // remove subscriptions - hmm, should this be in Subscription.java?
            DatabaseManager.updateQuery(context,
                    "DELETE FROM subscription WHERE collection_id= ? ", 
                    collection.getID());

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

            // Delete collection row
            DatabaseManager.delete(context, "collection", id);

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

    /**
     * Returns a List containing all the Collections.
     */
    public List<Collection> getCollections()
    {
        try
        {
            List<Collection> collections = new ArrayList<Collection>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "collection",
                    "SELECT * FROM collection ORDER BY name");

            while (tri.hasNext())
            {
                TableRow row = tri.next();
                int id = row.getIntColumn("collection_id");

                Collection fromCache =
                    (Collection) context.fromCache(Collection.class, id);

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(retrieve(id));
                }
            }

            return collections;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Returns a List of collections that user has a given permission on.
     * Useful for trimming 'select to collection' list, or figuring out which
     * collections a person is an editor for.
     */
    public List<Collection> getCollectionsByAuthority(Community parent,
            int actionID)
    {
        List<Collection> results = new ArrayList<Collection>();

        Collection[] collections = null;

        if (parent != null)
        {
            collections = parent.getCollections();
        }
        else
        {
            collections =
                (Collection[]) getCollections().toArray(new Collection[0]);
        }

        for (int i = 0; i < collections.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    collections[i], actionID))
            {
                results.add(collections[i]);
            }
        }

        return results;
    }

    public List<Collection> getParentCollections(Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "collection",
                    "SELECT c.collection_id " +
                    "FROM collection c, collection2item c2i " +
                    "WHERE c2i.collection_id = c.collection_id " +
                    "AND c2i.item_id = ? ",
                    item.getID());

            List<Collection> collections = new ArrayList<Collection>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("collection_id");
                collections.add(retrieve(id));
            }

            return collections;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Collection> getChildCollections(Community community)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(
                    context,"collection",
                    "SELECT c.collection_id, c.name " +
                    "FROM collection c, community2collection c2c " +
                    "WHERE c2c.collection_id = c.collection_id " +
                    "AND c2c.community_id= ? " +
                    "ORDER BY c.name",
                    community.getID());

            List<Collection> collections = new ArrayList<Collection>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("collection_id");
                collections.add(retrieve(id));
            }

            return collections;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Straightforward utility method for counting the number of Items in the
     * given Collection. There is probably a way to be smart about this. Also,
     * this strikes me as the kind of method that shouldn't really be in here.
     */
    public int itemCount(Collection collection)
    {
        try
        {
            String query = "SELECT count(*) FROM collection2item, item WHERE "
                + "collection2item.collection_id =  ? "
                + "AND collection2item.item_id = item.item_id "
                + "AND in_archive ='1' AND item.withdrawn='0' ";

            PreparedStatement statement =
                context.getDBConnection().prepareStatement(query);
            statement.setInt(1, collection.getID());
            
            ResultSet rs = statement.executeQuery();
            
            rs.next();
            int itemcount = rs.getInt(1);

            statement.close();

            return itemcount;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Create a database layer association between the given Item and
     * Collection.
     */
    public void link(Collection collection, Item item)
        throws AuthorizeException
    {
        if (!linked(collection, item))
        {
            try
            {
                AuthorizeManager.authorizeAction(context, collection,
                        Constants.ADD);

                log.info(LogManager.getHeader(context, "add_item",
                            "collection_id=" + collection.getID() +
                            ",item_id=" + item.getID()));

                TableRow row =
                    DatabaseManager.create(context, "collection2item");

                row.setColumn("collection_id", collection.getID());
                row.setColumn("item_id", item.getID());

                DatabaseManager.update(context, row);

                // If we're adding the Item to the Collection, we bequeath the
                // policies unto it.
                AuthorizeManager.inheritPolicies(context, collection, item);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    /**
     * Remove any existing database layer association between the given Item
     * and Collection.
     */
    public void unlink(Collection collection, Item item)
        throws AuthorizeException
    {
        if (linked(collection, item))
        {
            try
            {
                AuthorizeManager.authorizeAction(context, collection,
                        Constants.REMOVE);

                log.info(LogManager.getHeader(context, "remove_item",
                        "collection_id=" + collection.getID() + 
                        ",item_id=" + item.getID()));

                DatabaseManager.updateQuery(context,
                        "DELETE FROM collection2item WHERE collection_id= ? " +
                        "AND item_id= ? ",
                        collection.getID(), item.getID());
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    /**
     * Determine whether or not there is an established link between the given
     * Item and Collection in the database.
     */
    private boolean linked(Collection collection, Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT id FROM collection2item " +
                    "WHERE collection_id = ? AND item_id = ? ",
                    collection.getID(), item.getID());

            return tri.hasNext();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateTableRowFromCollection(Collection collection,
            TableRow row)
    {
        int id = collection.getID();
        Bitstream logo = collection.getLogo();
        Item templateItem = collection.getTemplateItem();
        Group[] workflowGroups = collection.getWorkflowGroups();

        if (logo == null)
        {
            row.setColumnNull("logo_bitstream_id");
        }
        else
        {
            row.setColumn("logo_bitstream_id", logo.getID());
        }

        if (templateItem == null)
        {
            row.setColumnNull("template_item_id");
        }
        else
        {
            row.setColumn("template_item_id", templateItem.getID());
        }

        for (int i = 1; i <= workflowGroups.length; i++)
        {
            Group g = workflowGroups[i - 1];
            if (g == null)
            {
                row.setColumnNull("workflow_step_" + i);
            }
            else
            {
                row.setColumn("workflow_step_" + i, g.getID());
            }
        }

        // Now loop over all allowed metadata fields and set the value into the
        // TableRow.
        for (CollectionMetadataField field : CollectionMetadataField.values())
        {
            String value = collection.getMetadata(field.toString());
            if (value == null)
            {
                row.setColumnNull(field.toString());
            }
            else
            {
                row.setColumn(field.toString(), value);
            }
        }
    }

    private void populateCollectionFromTableRow(Collection c, TableRow row)
    {
        Bitstream logo = null;
        Item templateItem = null;

        // Get the logo bitstream
        if (!row.isColumnNull("logo_bitstream_id"))
        {
            try
            {
                logo = Bitstream.find(context,
                        row.getIntColumn("logo_bitstream_id"));
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }

        // Get the template item
        if (!row.isColumnNull("template_item_id"))
        {
            templateItem =
                itemDAO.retrieve(row.getIntColumn("template_item_id"));
        }

        c.setLogoBitstream(logo);
        c.setTemplateItem(templateItem);

        c.setWorkflowGroup(1, groupFromColumn(row, "workflow_step_1"));
        c.setWorkflowGroup(2, groupFromColumn(row, "workflow_step_2"));
        c.setWorkflowGroup(3, groupFromColumn(row, "workflow_step_3"));

        c.setSubmitters(groupFromColumn(row, "submitter"));
        c.setAdministrators(groupFromColumn(row, "admin"));

        for (CollectionMetadataField field : CollectionMetadataField.values())
        {
            String value = row.getStringColumn(field.toString());
            if (value == null)
            {
                c.setMetadata(field.toString(), "");
            }
            else
            {
                c.setMetadata(field.toString(), value);
            }
        }
    }

    /**
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     * 
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(TableRow row, String col)
    {
        if (row.isColumnNull(col))
        {
            return null;
        }

        try
        {
            return Group.find(context, row.getIntColumn(col));
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
