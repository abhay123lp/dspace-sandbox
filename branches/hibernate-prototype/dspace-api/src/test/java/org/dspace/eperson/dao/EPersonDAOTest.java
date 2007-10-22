/*
 * EPersonDAOTest.java
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
import java.util.UUID;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class EPersonDAOTest extends DAOTest implements CRUDTest
{
    private EPersonDAO instance;
    private GroupDAO groupDAO;
    
    public EPersonDAOTest()
    {
        instance = EPersonDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        EPerson result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        EPerson existing = instance.create();
        EPerson result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        EPerson eperson = instance.create();

        String email = UUID.randomUUID().toString();
        eperson.setEmail(email);
        instance.update(eperson);

        EPerson result = instance.retrieve(eperson.getID());
        assertEquals(result.getEmail(), email);
    }

    @Test
    public void delete() throws Exception
    {
        EPerson eperson = instance.create();
        int id = eperson.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getEPeople() throws Exception
    {
        EPerson epersonOne = instance.create();
        EPerson epersonTwo = instance.create();
        Group groupOne = groupDAO.create();
        Group groupTwo = groupDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (EPerson eperson : instance.getEPeople())
        {
            if (eperson.equals(epersonOne))
            {
                containsOne = true;
            }
            if (eperson.equals(epersonTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        for (EPerson eperson : instance.getEPeople(groupOne))
        {
            if (eperson.equals(epersonOne))
            {
                fail();
            }
            if (eperson.equals(epersonTwo))
            {
                fail();
            }
        }

        groupDAO.link(groupOne, epersonOne);
        groupDAO.link(groupTwo, epersonTwo);

        for (EPerson eperson : instance.getEPeople(groupOne))
        {
            if (eperson.equals(epersonOne))
            {
                containsOne = true;
            }
            if (eperson.equals(epersonTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        for (EPerson eperson : instance.getEPeople(groupTwo))
        {
            if (eperson.equals(epersonOne))
            {
                fail();
            }
            if (eperson.equals(epersonTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsTwo);
        containsTwo = false;
    }

    @Test
    public void getAllEPeople() throws Exception
    {
        EPerson epersonOne = instance.create();
        EPerson epersonTwo = instance.create();
        Group groupOne = groupDAO.create();
        Group groupTwo = groupDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (EPerson eperson : instance.getAllEPeople(groupOne))
        {
            if (eperson.equals(epersonOne))
            {
                fail();
            }
            if (eperson.equals(epersonTwo))
            {
                fail();
            }
        }

        groupDAO.link(groupOne, epersonOne);
        groupDAO.link(groupTwo, epersonTwo);

        for (EPerson eperson : instance.getAllEPeople(groupOne))
        {
            if (eperson.equals(epersonOne))
            {
                containsOne = true;
            }
            if (eperson.equals(epersonTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        for (EPerson eperson : instance.getAllEPeople(groupTwo))
        {
            if (eperson.equals(epersonOne))
            {
                fail();
            }
            if (eperson.equals(epersonTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsTwo);
        containsTwo = false;

        // Now make groupTwo a subgroup of groupOne
        groupDAO.link(groupOne, groupTwo);

        for (EPerson eperson : instance.getAllEPeople(groupOne))
        {
            if (eperson.equals(epersonOne))
            {
                containsOne = true;
            }
            if (eperson.equals(epersonTwo))
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
    public void search() throws Exception
    {
        // Search is used to query the first name, last name, and email
        // address of EPeople.
        EPerson eperson = instance.create();
        String firstName = UUID.randomUUID().toString();
        String lastName = UUID.randomUUID().toString();
        String email = UUID.randomUUID().toString();
        boolean success = false;

        String queryOne = firstName.substring(3,14);
        String queryTwo = lastName.substring(4,10);
        String queryThree = email.substring(2, 20);

        // First, we test everything to make sure that nothing matches our
        // EPerson yet.
        String queries[] = { queryOne, queryTwo, queryThree };
        for (String query : queries)
        {
            for (EPerson result : instance.search(query))
            {
                if (result.equals(eperson))
                {
                    fail();
                }
            }
        }

        eperson.setFirstName(firstName);
        instance.update(eperson);
        for (EPerson result : instance.search(queryOne))
        {
            if (result.equals(eperson))
            {
                success = true;
            }
        }
        assertTrue(success);
        success = false;

        eperson.setLastName(lastName);
        instance.update(eperson);
        for (EPerson result : instance.search(queryTwo))
        {
            if (result.equals(eperson))
            {
                success = true;
            }
        }
        assertTrue(success);
        success = false;

        eperson.setEmail(email);
        instance.update(eperson);
        for (EPerson result : instance.search(queryThree))
        {
            if (result.equals(eperson))
            {
                success = true;
            }
        }
        assertTrue(success);
        success = false;
    }
}
