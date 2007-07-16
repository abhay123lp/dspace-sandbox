/*
 * WorkspaceItem.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing an item in the process of being submitted by a user.
 *
 * FIXME: this class could benefit from a proxy so the Collection and Item
 * aren't fully instantiated unless explicitly required. Could be wasted
 * effort, however, as the number of workspace items in memory at any given
 * time will typically be very low.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class WorkspaceItem implements InProgressSubmission
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    private Context context;
    private WorkspaceItemDAO dao;
    private ItemDAO itemDAO;
    private CollectionDAO collectionDAO;

    private TableRow wiRow;

    private int id;
    private boolean hasMultipleFiles;
    private boolean hasMultipleTitles;
    private boolean publishedBefore;
    private int stageReached;

    private Item item;
    private Collection collection;

    public WorkspaceItem(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = WorkspaceItemDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);

        context.cache(this, id);
    }
    
    public int getID()
    {
        return id;
    }

    public int getStageReached()
    {
        return stageReached;
    }

    public void setStageReached(int stageReached)
    {
        this.stageReached = stageReached;
    }

    // InProgressSubmission methods
    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Collection getCollection()
    {
        return collection;
    }

    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return hasMultipleFiles;
    }

    public void setMultipleFiles(boolean hasMultipleFiles)
    {
        this.hasMultipleFiles = hasMultipleFiles;
    }

    public boolean hasMultipleTitles()
    {
        return hasMultipleTitles;
    }

    public void setMultipleTitles(boolean hasMultipleTitles)
    {
        this.hasMultipleFiles = hasMultipleTitles;
    }

    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    public void setPublishedBefore(boolean publishedBefore)
    {
        this.publishedBefore = publishedBefore;
    }

    /**
     * Create a new workspace item, with a new ID. An Item is also created. The
     * submitter is the current user in the context.
     * 
     * @param c
     *            DSpace context object
     * @param coll
     *            Collection being submitted to
     * @param template
     *            if <code>true</code>, the workspace item starts as a copy
     *            of the collection's template item
     * 
     * @return the newly created workspace item
     */
    public static WorkspaceItem create(Context c, Collection collection,
            boolean template)
        throws AuthorizeException, SQLException, IOException
    {
        // Check the user has permission to ADD to the collection
        AuthorizeManager.authorizeAction(c, collection, Constants.ADD);

        // Create an item
        ItemDAO itemDAO = ItemDAOFactory.getInstance(c);
        Item item = itemDAO.create();
        item.setSubmitter(c.getCurrentUser());

        // Now create the policies for the submitter and workflow
        // users to modify item and contents
        // contents = bitstreams, bundles
        // FIXME: icky hardcoded workflow steps
        Group step1group = collection.getWorkflowGroup(1);
        Group step2group = collection.getWorkflowGroup(2);
        Group step3group = collection.getWorkflowGroup(3);

        EPerson e = c.getCurrentUser();

        // read permission
        AuthorizeManager.addPolicy(c, item, Constants.READ, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.READ, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.READ, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.READ, step3group);
        }

        // write permission
        AuthorizeManager.addPolicy(c, item, Constants.WRITE, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.WRITE, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.WRITE, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.WRITE, step3group);
        }

        // add permission
        AuthorizeManager.addPolicy(c, item, Constants.ADD, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.ADD, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.ADD, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.ADD, step3group);
        }

        // remove contents permission
        AuthorizeManager.addPolicy(c, item, Constants.REMOVE, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step3group);
        }

        // Copy template if appropriate
        Item templateItem = collection.getTemplateItem();

        if (template && (templateItem != null))
        {
            DCValue[] md = templateItem.getMetadata(
                    Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (int n = 0; n < md.length; n++)
            {
                item.addMetadata(md[n].schema, md[n].element, md[n].qualifier,
                        md[n].language, md[n].value);
            }
        }

        itemDAO.update(item);

        // Create the workspace item row
        TableRow row = DatabaseManager.create(c, "workspaceitem");

        int id = row.getIntColumn("workspace_item_id");
        WorkspaceItem wsi = new WorkspaceItem(c, id);

        wsi.setItem(item);
        wsi.setCollection(collection);

        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(c);
        dao.update(wsi);

        log.info(LogManager.getHeader(c, "create_workspace_item",
                "workspace_item_id=" + row.getIntColumn("workspace_item_id")
                        + "item_id=" + item.getID() + "collection_id="
                        + collection.getID()));

        HistoryManager.saveHistory(c, wsi, HistoryManager.CREATE,
                c.getCurrentUser(), c.getExtraLogInfo());

        return wsi;
    }

    /**
     * Get all workspace items for a particular e-person. These are ordered by
     * workspace item ID, since this should likely keep them in the order in
     * which they were created.
     * 
     * @param context
     *            the context object
     * @param ep
     *            the eperson
     * 
     * @return the corresponding workspace items
     */
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
            throws SQLException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context,
                "workspaceitem",
                "SELECT workspaceitem.* FROM workspaceitem, item WHERE " +
                "workspaceitem.item_id=item.item_id AND " +
                "item.submitter_id= ? " +
                "ORDER BY workspaceitem.workspace_item_id", 
                ep.getID());

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("workspace_item_id");
            wsItems.add(dao.retrieve(id));
        }

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    /**
     * Get all workspace items for a particular collection.
     * 
     * @param context
     *            the context object
     * @param c
     *            the collection
     * 
     * @return the corresponding workspace items
     */
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
            throws SQLException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context, "workspaceitem",
                "SELECT workspace_item_id FROM workspaceitem WHERE " +
                "collection_id = ? ", c.getID());

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("workspace_item_id");
            wsItems.add(dao.retrieve(id));
        }

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    /**
     * Get all workspace items in the whole system
     *
     * @param   context     the context object
     *
     * @return      all workspace items
     */
    public static WorkspaceItem[] findAll(Context context)
        throws SQLException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context,
                "workspaceitem",
                "SELECT workspace_item_id FROM workspaceitem ORDER BY item_id");

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("workspace_item_id");
            wsItems.add(dao.retrieve(id));
        }

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    /**
     * Delete the workspace item. The entry in workspaceitem, the unarchived
     * item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     */
    public void deleteAll() throws SQLException, AuthorizeException,
            IOException
    {
        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.
         */
        if (!AuthorizeManager.isAdmin(context) &&
                ((context.getCurrentUser() == null) ||
                 (context.getCurrentUser().getID() !=
                  item.getSubmitter().getID())))
        {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                    + "original submitter to delete a workspace item");
        }

        HistoryManager.saveHistory(context, this, HistoryManager.REMOVE,
                context.getCurrentUser(), context.getExtraLogInfo());

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //deleteSubmitPermissions();
        // Remove from cache
        context.removeCached(this, getID());

        // Need to delete the epersongroup2workspaceitem row first since it refers
        // to workspaceitem ID
        String query =
            "DELETE FROM epersongroup2workspaceitem " +
            "WHERE workspace_item_id = ?";
        DatabaseManager.updateQuery(context, query, getID());

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(context, wiRow);

        // Delete item
        itemDAO.delete(item.getID());
    }

    public void deleteWrapper() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation. We check permissions on the enclosed item.
        AuthorizeManager.authorizeAction(context, item, Constants.WRITE);

        HistoryManager.saveHistory(context, this, HistoryManager.REMOVE,
                context.getCurrentUser(), context.getExtraLogInfo());

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //        deleteSubmitPermissions();
        // Remove from cache
        context.removeCached(this, getID());

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(context, wiRow);
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public boolean equals(WorkspaceItem wsi)
    {
        if (this.getID() == wsi.getID())
        {
            return true;
        }

        return false;
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /** Deprecated by the introduction of DAOs */
    @Deprecated
    WorkspaceItem(Context context, TableRow row) throws SQLException
    {
        this(context, row.getIntColumn("workspace_item_id"));
        log.info("calling deprecated constructor");
    }

    @Deprecated
    public static WorkspaceItem find(Context context, int id)
            throws SQLException
    {
        log.info("calling deprecated find() method");
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        log.info(dao.retrieve(id));
        return dao.retrieve(id);
    }

    @Deprecated
    public void update() throws SQLException, AuthorizeException, IOException
    {
        dao.update(this);
    }
}
