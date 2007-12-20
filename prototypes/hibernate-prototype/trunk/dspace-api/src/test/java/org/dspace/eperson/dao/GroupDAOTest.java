/*
 * GroupDAOTest.java
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
package org.dspace.eperson.dao;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;
import org.dspace.storage.dao.LinkTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class GroupDAOTest extends DAOTest implements CRUDTest, LinkTest
{
    private GroupDAO instance;
    private EPersonDAO epersonDAO;
    private WorkspaceItemDAO wsiDAO;
    
    public GroupDAOTest()
    {
        instance = GroupDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
        wsiDAO = WorkspaceItemDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        Group result = instance.create();

        int id = result.getId();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Group existing = instance.create();
        Group result = instance.retrieve(existing.getId());

        assertEquals(existing.getId(), result.getId());
    }

    @Test
    public void update() throws Exception
    {
        Group group = instance.create();

        String name = UUID.randomUUID().toString();
        group.setName(name);
        instance.update(group);

        Group result = instance.retrieve(group.getId());
        assertEquals(result.getName(), name);
    }

    @Test
    public void delete() throws Exception
    {
        Group group = instance.create();
        int id = group.getId();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void link() throws Exception
    {
        Group parent = instance.create();
        Group child = instance.create();

        assertTrue(!instance.linked(parent, child));
        assertTrue(!instance.linked(child, parent));

        instance.link(parent, child);

        assertTrue(instance.linked(parent, child));
        assertTrue(!instance.linked(child, parent));
    }

    @Test
    public void unlink() throws Exception
    {
        Group parent = instance.create();
        Group child = instance.create();

        instance.link(parent, child);

        assertTrue(instance.linked(parent, child));
        assertTrue(!instance.linked(child, parent));

        instance.unlink(parent, child);

        assertTrue(!instance.linked(parent, child));
    }

    @Test
    public void linked() throws Exception
    {
        Group parent = instance.create();
        Group child = instance.create();

        assertTrue(!instance.linked(parent, child));
        assertTrue(!instance.linked(child, parent));

        instance.link(parent, child);

        assertTrue(instance.linked(parent, child));
        assertTrue(!instance.linked(child, parent));
    }

    @Test
    public void getGroups() throws Exception
    {
        Group groupOne = instance.create();
        Group groupTwo = instance.create();
        EPerson eperson = epersonDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (Group group : instance.getGroups())
        {
            if (groupOne.equals(group))
            {
                containsOne = true;
            }
            if (groupTwo.equals(group))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        instance.link(groupOne, eperson);
        instance.link(groupTwo, eperson);

        for (Group group : instance.getGroups(eperson))
        {
            if (groupOne.equals(group))
            {
                containsOne = true;
            }
            if (groupTwo.equals(group))
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
    public void getGroupIDs() throws Exception
    {
        Group groupOne = instance.create();
        Group groupTwo = instance.create();
        EPerson eperson = epersonDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        instance.link(groupOne, eperson);
        instance.link(groupTwo, eperson);

        for (Integer id : instance.getGroupIDs(eperson))
        {
            if (groupOne.getId() == id)
            {
                containsOne = true;
            }
            if (groupTwo.getId() == id)
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
    public void getSupervisorGroups() throws Exception
    {
        WorkspaceItem wsi = wsiDAO.create();
        Group group = instance.create();
        EPerson eperson = epersonDAO.create();

        // Put the eperson into the supervising group
        instance.link(group, eperson);

        // No supervisions defined yet
        for (Group g : instance.getSupervisorGroups(wsi))
        {
            if (g.equals(group))
            {
                fail();
            }
        }

        // Now establish the supervision of the workspace item by the above
        // group
        instance.link(group, wsi);

        boolean success = false;
        for (Group g : instance.getSupervisorGroups(wsi))
        {
            if (g.equals(group))
            {
                success = true;
            }
        }
        assertTrue(success);
    }

    @Test
    public void getMemberGroups() throws Exception
    {
        Group parentOne = instance.create();
        Group parentTwo = instance.create();
        Group childOne = instance.create();
        Group childTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        instance.link(parentOne, childOne);
        instance.link(parentTwo, childTwo);
        instance.link(parentOne, parentTwo);

        for (Group group : instance.getMemberGroups(parentOne))
        {
            if (childOne.equals(group))
            {
                containsOne = true;
            }
            if (childTwo.equals(group))
            {
                fail();
            }
        }

        assertTrue(containsOne);
        containsOne = false;
    }

    @Test
    public void search() throws Exception
    {
        // According to the API, search() should cover name & id. I think maybe
        // this should be binned in favour of retrieve(int id) and
        // retrieve(String name), although.
    }

    @Test
    public void currentUserInGroup() throws Exception
    {
        Group group = instance.create();

        if (instance.currentUserInGroup(group.getId()))
        {
            fail();
        }

        instance.link(group, context.getCurrentUser());

        if (!instance.currentUserInGroup(group.getId()))
        {
            fail();
        }
    }

    @Test
    public void cleanSupervisionOrders() throws Exception
    {
        // I'm not 100% sure this is testable. Really, this kind of thing
        // shouldn't be necessary.
    }
}
