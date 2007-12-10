/*
 * WorkflowItemDAOTest.java
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
package org.dspace.workflow.dao;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;
import org.dspace.workflow.TaskListItem;
import org.dspace.workflow.WorkflowItem;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkflowItemDAOTest extends DAOTest implements CRUDTest
{
    private WorkflowItemDAO instance;
    private CollectionDAO collectionDAO;
    private ItemDAO itemDAO;
    private EPersonDAO epersonDAO;
    
    public WorkflowItemDAOTest()
    {
        instance = WorkflowItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        WorkflowItem result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        WorkflowItem existing = instance.create();
        WorkflowItem result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        WorkflowItem wfi = createWorkflowItem();
        EPerson owner = context.getCurrentUser();

        wfi.setOwner(owner);
        instance.update(wfi);

        WorkflowItem result = instance.retrieve(wfi.getID());
        assertEquals(result.getOwner(), owner);
    }

    @Test
    public void delete() throws Exception
    {
        WorkflowItem wfi = createWorkflowItem();
        int id = wfi.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void createTask() throws Exception
    {
        WorkflowItem wfi = createWorkflowItem();
        EPerson eperson = epersonDAO.create();
        TaskListItem tli = instance.createTask(wfi, eperson);

        int id = tli.getID();

        assertTrue(id > 0);
    }

    @Test
    public void deleteTasks() throws Exception
    {
        WorkflowItem wfi = createWorkflowItem();
        EPerson eperson = epersonDAO.create();
        TaskListItem tli = instance.createTask(wfi, eperson);

        int id = tli.getID();

        assertTrue(id > 0);
    }

    @Test
    public void getWorkflowItems() throws Exception
    {
        WorkflowItem itemOne = createWorkflowItem();
        WorkflowItem itemTwo = createWorkflowItem();
        Collection collection = collectionDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (WorkflowItem wfi : instance.getWorkflowItems())
        {
            if (wfi.equals(itemOne))
            {
                containsOne = true;
            }
            if (wfi.equals(itemTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        for (WorkflowItem wfi : instance.getWorkflowItems(collection))
        {
            if (wfi.equals(itemOne))
            {
                fail();
            }
            if (wfi.equals(itemTwo))
            {
                fail();
            }
        }

        itemOne.setCollection(collection);
        itemTwo.setCollection(collection);
        instance.update(itemOne);
        instance.update(itemTwo);

        for (WorkflowItem wfi : instance.getWorkflowItems(collection))
        {
            if (wfi.equals(itemOne))
            {
                containsOne = true;
            }
            if (wfi.equals(itemTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;
    }

    @Test
    public void getWorkflowItemsBySubmitter() throws Exception
    {
        WorkflowItem itemOne = createWorkflowItem();
        WorkflowItem itemTwo = createWorkflowItem();
        EPerson eperson = epersonDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (WorkflowItem wfi : instance.getWorkflowItemsBySubmitter(eperson))
        {
            if (wfi.equals(itemOne))
            {
                fail();
            }
            if (wfi.equals(itemTwo))
            {
                fail();
            }
        }

        itemOne.getItem().setSubmitter(eperson);
        itemTwo.getItem().setSubmitter(eperson);
        itemDAO.update(itemOne.getItem());
        itemDAO.update(itemTwo.getItem());

        for (WorkflowItem wfi : instance.getWorkflowItemsBySubmitter(eperson))
        {
            if (wfi.equals(itemOne))
            {
                containsOne = true;
            }
            if (wfi.equals(itemTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;
    }

    @Test
    public void getWorkflowItemsByOwner() throws Exception
    {
        WorkflowItem itemOne = createWorkflowItem();
        WorkflowItem itemTwo = createWorkflowItem();
        EPerson owner = epersonDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (WorkflowItem wfi : instance.getWorkflowItemsByOwner(owner))
        {
            if (wfi.equals(itemOne))
            {
                fail();
            }
            if (wfi.equals(itemTwo))
            {
                fail();
            }
        }

        itemOne.setOwner(owner);
        itemTwo.setOwner(owner);
        instance.update(itemOne);
        instance.update(itemTwo);

        for (WorkflowItem wfi : instance.getWorkflowItemsByOwner(owner))
        {
            if (wfi.equals(itemOne))
            {
                containsOne = true;
            }
            if (wfi.equals(itemTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;
    }

    @Test
    public void getTaskListItems() throws Exception
    {
        WorkflowItem wfi = createWorkflowItem();
        EPerson eperson = context.getCurrentUser();
        EPerson epersonOne = epersonDAO.create();
        EPerson epersonTwo = epersonDAO.create();
        TaskListItem tliOne = instance.createTask(wfi, epersonOne);
        TaskListItem tliTwo = instance.createTask(wfi, epersonTwo);
        boolean containsOne = false;
        boolean containsTwo = false;

        for (TaskListItem tli : instance.getTaskListItems(eperson))
        {
            if (tli.equals(tliOne))
            {
                fail();
            }
            if (tli.equals(tliTwo))
            {
                fail();
            }
        }

        for (TaskListItem tli : instance.getTaskListItems(epersonOne))
        {
            if (tli.equals(tliOne))
            {
                containsOne = true;
            }
            if (tli.equals(tliTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        for (TaskListItem tli : instance.getTaskListItems(epersonTwo))
        {
            if (tli.equals(tliOne))
            {
                fail();
            }
            if (tli.equals(tliTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsTwo);
        containsTwo = false;
    }

    private WorkflowItem createWorkflowItem() throws Exception
    {
        WorkflowItem wfi = instance.create();
        Collection collection = collectionDAO.create();
        EPerson submitter = epersonDAO.create();
        Item item = itemDAO.create();

        item.setSubmitter(submitter);
        itemDAO.update(item);

        wfi.setCollection(collection);
        wfi.setItem(item);
        instance.update(wfi);

        return wfi;
    }
}
