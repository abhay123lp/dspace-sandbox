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
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
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
    }

    public WorkspaceItem create() throws AuthorizeException
    {
        try
        {
            UUID uuid = UUID.randomUUID();

            TableRow row = DatabaseManager.create(context, "item");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("item_id");

            return super.create(id, uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public WorkspaceItem retrieve(int id)
    {
        return null;
    }

    public void update(WorkspaceItem wsi) throws AuthorizeException
    {
    }

    public void delete(int id) throws AuthorizeException
    {
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateWorkSpaceItemFromTableRow(WorkspaceItem wsi,
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
    }
}
