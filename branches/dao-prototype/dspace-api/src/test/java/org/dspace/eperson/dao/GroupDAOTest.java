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
    
    public GroupDAOTest()
    {
        instance = GroupDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        Group result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Group existing = instance.create();
        Group result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Group group = instance.create();

        String name = UUID.randomUUID().toString();
        group.setName(name);
        instance.update(group);

        Group result = instance.retrieve(group.getID());
        assertEquals(result.getName(), name);
    }

    @Test
    public void delete() throws Exception
    {
        Group group = instance.create();
        int id = group.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getGroups() throws Exception
    {
    }

    @Test
    public void getGroupIDs() throws Exception
    {
    }

    @Test
    public void getSupervisorGroups() throws Exception
    {
    }

    @Test
    public void getMemberGroups() throws Exception
    {
    }

    @Test
    public void search() throws Exception
    {
    }

    @Test
    public void currentUserInGroup() throws Exception
    {
    }

    @Test
    public void link() throws Exception
    {
    }

    @Test
    public void unlink() throws Exception
    {
    }

    @Test
    public void linked() throws Exception
    {
    }

    @Test
    public void cleanSupervisionOrders() throws Exception
    {
    }
}
