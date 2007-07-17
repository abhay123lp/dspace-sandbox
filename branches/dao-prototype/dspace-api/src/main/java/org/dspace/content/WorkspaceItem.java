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
public class WorkspaceItem extends DSpaceObject implements InProgressSubmission
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    private Context context;
    private WorkspaceItemDAO dao;
    private ItemDAO itemDAO;
    private CollectionDAO collectionDAO;

//    private int id;
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
    
//    public int getID()
//    {
//        return id;
//    }
    public int getType()
    {
        return -1;
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
        DatabaseManager.delete(context, "workspaceitem", getID());

        // Delete item
        itemDAO.delete(item.getID());
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
        if (getID() == wsi.getID())
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
    WorkspaceItem(Context context, TableRow row)
    {
        this(context, row.getIntColumn("workspace_item_id"));
    }

    @Deprecated
    public static WorkspaceItem create(Context context, Collection collection,
            boolean template)
        throws AuthorizeException, IOException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.create(collection, template);
    }

    @Deprecated
    public static WorkspaceItem find(Context context, int id)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.retrieve(id);
    }

    @Deprecated
    public void update() throws AuthorizeException, IOException
    {
        dao.update(this);
    }

    @Deprecated
    public void deleteWrapper() throws SQLException, AuthorizeException,
           IOException
    {
        dao.delete(getID());
    }

    @Deprecated
    public static WorkspaceItem[] findAll(Context context)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems();

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(ep);

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(c);

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }
}
