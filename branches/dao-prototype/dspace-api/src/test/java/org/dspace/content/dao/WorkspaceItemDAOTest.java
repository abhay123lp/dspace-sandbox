/*
 * WorkspaceItemDAOTest.java
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

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkspaceItemDAOTest extends ContentDAOTest
{
    private WorkspaceItemDAO instance;
    private EPersonDAO epersonDAO;
    private ItemDAO itemDAO;
    private CollectionDAO collectionDAO;
    
    public WorkspaceItemDAOTest()
    {
        instance = WorkspaceItemDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        WorkspaceItem result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        WorkspaceItem existing = instance.create();
        WorkspaceItem result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        WorkspaceItem wsi = instance.create();
        Item item = itemDAO.create();

        wsi.setItem(item);
        instance.update(wsi);

        WorkspaceItem result = instance.retrieve(wsi.getID());
        assertEquals(item, wsi.getItem());
    }

    @Test
    public void delete() throws Exception
    {
        WorkspaceItem result = instance.create();
        Item item = itemDAO.create();

        result.setItem(item);
        instance.update(result);

        int id = result.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
        assertNotNull(itemDAO.retrieve(item.getID()));
    }

    @Test
    public void deleteAll() throws Exception
    {
        WorkspaceItem result = instance.create();
        Item item = itemDAO.create();

        result.setItem(item);
        instance.update(result);

        int id = result.getID();
        int itemID = item.getID();

        instance.deleteAll(id);

        assertNull(instance.retrieve(id));
        assertNull(itemDAO.retrieve(itemID));
    }

    @Test
    public void getWorkspaceItems() throws Exception
    {
        WorkspaceItem wsi = instance.create();
        Collection collection = collectionDAO.create();
        Item item = itemDAO.create();
        EPerson eperson = epersonDAO.create();
        List<WorkspaceItem> items = null;

        item.setSubmitter(eperson);
        itemDAO.update(item);
        wsi.setItem(item);
        wsi.setCollection(collection);
        instance.update(wsi);

        boolean success = false;
        items = instance.getWorkspaceItems();

        for (WorkspaceItem i : items)
        {
            if (i.getID() == wsi.getID())
            {
                success = true;
            }
        }

        assertTrue(success);

        success = false;
        items = instance.getWorkspaceItems(eperson);

        for (WorkspaceItem i : items)
        {
            if (i.getID() == wsi.getID())
            {
                success = true;
            }
        }

        assertTrue(success);

        success = false;
        items = instance.getWorkspaceItems(collection);

        for (WorkspaceItem i : items)
        {
            if (i.getID() == wsi.getID())
            {
                success = true;
            }
        }

        assertTrue(success);
    }
}
