/*
 * ResourcePolicyDAOTest.java
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
package org.dspace.authorize.dao;

import java.util.List;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResourcePolicyDAOTest extends DAOTest implements CRUDTest
{
    private ResourcePolicyDAO instance;
    private ItemDAO itemDAO;
    private GroupDAO groupDAO;
    
    public ResourcePolicyDAOTest()
    {
        instance = ResourcePolicyDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);
    }

    @Test
    public void create()
    {
        ResourcePolicy result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve()
    {
        ResourcePolicy existing = instance.create();
        ResourcePolicy result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update()
    {
        ResourcePolicy rp = instance.create();
        rp.setResourceType(Constants.ITEM);
        instance.update(rp);
        
        ResourcePolicy result = instance.retrieve(rp.getID());
        assertEquals(Constants.ITEM, result.getResourceType());
    }

    @Test
    public void delete()
    {
        ResourcePolicy result = instance.create();
        int id = result.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getPolicies() throws Exception
    {
        // Here, we test for the following arguments:
        //  * DSpaceObject
        //  * Group
        //  * DSpaceObject + Group
        //  * DSpaceObject + actionID

        ResourcePolicy rpOne = instance.create();
        ResourcePolicy rpTwo = instance.create();
        Item item = itemDAO.create();
        Group group = groupDAO.create();
        List<ResourcePolicy> rps = null;
        boolean containsOne = false;
        boolean containsTwo = false;
        
        // First, associate the Item with one, and the Group with the other.
        rpOne.setResource(item);
        rpOne.setAction(Constants.ADD);
        rpTwo.setGroup(group);
        rpTwo.setAction(Constants.DELETE);
        instance.update(rpOne);
        instance.update(rpTwo);

        // Only rpOne is associated with the Item
        rps = instance.getPolicies(item);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        // Only rpTwo is associated with the Group
        rps = instance.getPolicies(group);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                fail();
            }
            if (rp.equals(rpTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsTwo);
        containsTwo = false;

        // Neither rpOne nor rpTwo is associated with both the Item and the
        // Group
        rps = instance.getPolicies(item, group);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne) || rp.equals(rpTwo))
            {
                fail();
            }
        }

        // Only rpOne is allowed to ADD
        rps = instance.getPolicies(item, Constants.ADD);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        // rpTwo is allowed to DELETE, but not on this Item
        rps = instance.getPolicies(item, Constants.DELETE);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne) || rp.equals(rpTwo))
            {
                fail();
            }
        }
        
        // Now we associate the Item and Group with both
        rpOne.setGroup(group);
        rpTwo.setResource(item);
        instance.update(rpOne);
        instance.update(rpTwo);

        rps = instance.getPolicies(item);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        rps = instance.getPolicies(item);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        rps = instance.getPolicies(item, group);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        // Only rpOne is allowed to ADD
        rps = instance.getPolicies(item, Constants.ADD);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                containsOne = true;
            }
            if (rp.equals(rpTwo))
            {
                fail();
            }
        }
        assertTrue(containsOne);
        containsOne = false;

        // Only rpTwo is allowed to DELETE
        rps = instance.getPolicies(item, Constants.DELETE);
        for (ResourcePolicy rp : rps)
        {
            if (rp.equals(rpOne))
            {
                fail();
            }
            if (rp.equals(rpTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsTwo);
        containsTwo = false;
    }
}
