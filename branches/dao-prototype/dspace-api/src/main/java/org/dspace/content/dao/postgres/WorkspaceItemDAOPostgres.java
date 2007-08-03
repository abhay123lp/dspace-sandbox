/*
 * WorkspaceItemDAOPostgres.java
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
package org.dspace.content.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class WorkspaceItemDAOPostgres extends WorkspaceItemDAO
{
    public WorkspaceItemDAOPostgres(Context context)
    {
        this.context = context;

        itemDAO = ItemDAOFactory.getInstance(context);
    }

    public WorkspaceItem create(Collection collection, boolean template)
        throws AuthorizeException
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "workspaceitem");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("workspace_item_id");
            WorkspaceItem wsi = new WorkspaceItem(context, id);
            wsi.setIdentifier(new ObjectIdentifier(uuid));

            return super.create(wsi, collection, template);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public WorkspaceItem retrieve(int id)
    {
        WorkspaceItem wsi = super.retrieve(id);

        if (wsi != null)
        {
            return wsi;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "workspaceitem", id);

            if (row == null)
            {
                log.warn("workspace item " + id + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public WorkspaceItem retrieve(UUID uuid)
    {
        WorkspaceItem wsi = super.retrieve(uuid);

        if (wsi != null)
        {
            return wsi;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "workspaceitem", "uuid", uuid.toString());

            if (row == null)
            {
                log.warn("workspace item " + uuid + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private WorkspaceItem retrieve(TableRow row)
    {
        int id = row.getIntColumn("workspace_item_id");
        WorkspaceItem wsi = new WorkspaceItem(context, id);
        populateWorkspaceItemFromTableRow(wsi, row);

        return wsi;
    }

    public void update(WorkspaceItem wsi) throws AuthorizeException
    {
        super.update(wsi);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "workspaceitem", wsi.getID());

            if (row != null)
            {
                update(wsi, row);
            }
            else
            {
                throw new RuntimeException("Didn't find workspace item " +
                        wsi.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(WorkspaceItem wsi, TableRow row)
    {
        try
        {
            populateTableRowFromWorkspaceItem(wsi, row);
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void delete(int id) throws AuthorizeException
    {
        super.delete(id);

        try
        {
            DatabaseManager.delete(context, "workspaceitem", id);
            DatabaseManager.updateQuery(context,
                "DELETE FROM epersongroup2workspaceitem " +
                "WHERE workspace_item_id = ?", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<WorkspaceItem> getWorkspaceItems()
    {
        try
        {
            List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "workspaceitem",
                    "SELECT workspace_item_id FROM workspaceitem " +
                    "ORDER BY item_id");

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("workspace_item_id");
                wsItems.add(retrieve(id));
            }

            return wsItems;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<WorkspaceItem> getWorkspaceItems(EPerson eperson)
    {
        try
        {
            List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "workspaceitem",
                    "SELECT wsi.workspace_item_id " +
                    "FROM workspaceitem wsi, item " +
                    "WHERE wsi.item_id = item.item_id " +
                    "AND item.submitter_id = ? " +
                    "ORDER BY wsi.workspace_item_id", 
                    eperson.getID());

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("workspace_item_id");
                wsItems.add(retrieve(id));
            }

            return wsItems;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<WorkspaceItem> getWorkspaceItems(Collection collection)
    {
        try
        {
            List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

            TableRowIterator tri = DatabaseManager.queryTable(context, "workspaceitem",
                    "SELECT workspace_item_id FROM workspaceitem WHERE " +
                    "collection_id = ? ", collection.getID());

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("workspace_item_id");
                wsItems.add(retrieve(id));
            }

            return wsItems;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateWorkspaceItemFromTableRow(WorkspaceItem wsi,
            TableRow row)
    {
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        CollectionDAO collectionDAO =
            CollectionDAOFactory.getInstance(context);

        Item item = itemDAO.retrieve(row.getIntColumn("item_id"));
        Collection collection =
            collectionDAO.retrieve(row.getIntColumn("collection_id"));

        wsi.setItem(item);
        wsi.setCollection(collection);
        wsi.setMultipleFiles(row.getBooleanColumn("multiple_files"));
        wsi.setMultipleTitles(row.getBooleanColumn("multiple_titles"));
        wsi.setPublishedBefore(row.getBooleanColumn("published_before"));
        wsi.setStageReached(row.getIntColumn("stage_reached"));
    }

    private void populateTableRowFromWorkspaceItem(WorkspaceItem wsi,
            TableRow row)
    {
        row.setColumn("item_id", wsi.getItem().getID());
        Collection collection  = wsi.getCollection();
        if (collection != null)
        {
            row.setColumn("collection_id", collection.getID());
        }
        else
        {
            row.setColumnNull("collection_id");
        }
        row.setColumn("multiple_titles", wsi.hasMultipleTitles());
        row.setColumn("multiple_files", wsi.hasMultipleFiles());
        row.setColumn("published_before", wsi.isPublishedBefore());
        row.setColumn("stage_reached", wsi.getStageReached());
    }
}
