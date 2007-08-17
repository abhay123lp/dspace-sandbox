/*
 * CommunityDAOTest.java
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
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.LinkTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommunityDAOTest extends ContentDAOTest
    implements CRUDTest, LinkTest
{
    private CommunityDAO instance;
    private CollectionDAO collectionDAO;
    private ItemDAO itemDAO;

    public CommunityDAOTest()
    {
        instance = CommunityDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
    }
    
    @Test
    public void create() throws Exception
    {
        Community result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Community existing = instance.create();
        Community result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Community community = instance.create();
        community.setMetadata("name", "Community Test");
        instance.update(community);
        
        Community result = instance.retrieve(community.getID());
        assertEquals("Community Test", result.getMetadata("name"));
    }

    @Test
    public void delete() throws Exception
    {
        Community result = instance.create();
        int id = result.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getCommunities() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        List<Community> communities = instance.getCommunities();

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (Community c : communities)
        {
            if (communityOne.equals(c))
            {
                containsOne = true;
            }
            if (communityTwo.equals(c))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }

    @Test
    public void getTopLevelCommunities() throws Exception
    {
        // First, we create a few Communities so that the call to the DAO has
        // something to return.
        Community communityOne = instance.create();
        Community communityTwo = instance.create();

        for (Community c : instance.getTopLevelCommunities())
        {
            assertTrue(instance.getParentCommunities(c).size() == 0);
        }

        // Now we move communityTwo to be a sub-community of communityOne and
        // then we make sure that communityTwo is no longer in the list.
        instance.link(communityOne, communityTwo);

        for (Community c : instance.getTopLevelCommunities())
        {
            if (c.equals(communityTwo))
            {
                fail();
            }
        }
    }

    @Test
    public void getChildCommunities() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();
        Community communityThree = instance.create();
        List<Community> children = null;

        children = instance.getChildCommunities(communityOne);
        assertTrue(children.size() == 0);

        instance.link(communityOne, communityTwo);
        children = instance.getChildCommunities(communityOne);
        assertTrue(children.size() == 1);

        instance.link(communityTwo, communityThree);
        children = instance.getChildCommunities(communityOne);
        assertTrue(children.size() == 1);

        boolean success = false;
        for (Community c : children)
        {
            if (c.equals(communityTwo))
            {
                success = true;
            }
            if (c.equals(communityThree))
            {
                fail();
            }
        }
        assertTrue(success);
    }

    @Test
    public void getParentCommunities() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();
        Community communityThree = instance.create();
        List<Community> parents = null;

        parents = instance.getParentCommunities(communityThree);
        assertTrue(parents.size() == 0);

        instance.link(communityTwo, communityThree);
        parents = instance.getParentCommunities(communityThree);
        assertTrue(parents.size() == 1);

        instance.link(communityOne, communityTwo);
        parents = instance.getParentCommunities(communityThree);
        assertTrue(parents.size() == 1);
    }

    @Test
    public void getAllParentCommunities() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();
        Community communityThree = instance.create();
        List<Community> parents = null;

        parents = instance.getAllParentCommunities(communityThree);
        assertTrue(parents.size() == 0);

        instance.link(communityTwo, communityThree);
        parents = instance.getAllParentCommunities(communityThree);
        assertTrue(parents.size() == 1);

        instance.link(communityOne, communityTwo);
        parents = instance.getAllParentCommunities(communityTwo);
        assertTrue(parents.size() == 1);
        parents = instance.getAllParentCommunities(communityThree);
        assertTrue(parents.size() == 2);
    }

    @Test
    public void link() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();

        instance.link(communityOne, communityTwo);
        assertTrue(instance.linked(communityOne, communityTwo));
        assertTrue(instance.getChildCommunities(communityOne).size() == 1);
        assertTrue(instance.getParentCommunities(communityTwo).size() == 1);
    }

    @Test
    public void unlink() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();

        instance.link(communityOne, communityTwo);
        assertTrue(instance.getChildCommunities(communityOne).size() == 1);
        assertTrue(instance.getParentCommunities(communityTwo).size() == 1);

        instance.unlink(communityOne, communityTwo);
        assertTrue(instance.getChildCommunities(communityOne).size() == 0);
        assertTrue(instance.getParentCommunities(communityTwo).size() == 0);
    }

    @Test
    public void linked() throws Exception
    {
        Community communityOne = instance.create();
        Community communityTwo = instance.create();

        assertFalse(instance.linked(communityOne, communityTwo));

        instance.link(communityOne, communityTwo);
        assertTrue(instance.linked(communityOne, communityTwo));

        instance.unlink(communityOne, communityTwo);
        assertFalse(instance.linked(communityOne, communityTwo));
    }

    @Test
    public void itemCount() throws Exception
    {
        Community community = instance.create();
        Collection collection = collectionDAO.create();
        Item item = itemDAO.create();

        assertEquals(instance.itemCount(community), 0);

        instance.link(community, collection);
        collectionDAO.link(collection, item);
        assertTrue(instance.linked(community, collection));
        assertEquals(instance.itemCount(community), 0);

        // Place the item into the archive so that it shows up in the count
        item.setArchived(true);
        item.setWithdrawn(false);
        itemDAO.update(item);
        assertEquals(instance.itemCount(community), 1);

        instance.unlink(community, collection);
        assertEquals(instance.itemCount(community), 0);
    }
}
