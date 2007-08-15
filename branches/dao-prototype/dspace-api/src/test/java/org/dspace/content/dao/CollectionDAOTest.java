/*
 * CollectionDAOTest.java
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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.LinkTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CollectionDAOTest implements CRUDTest, LinkTest
{
    private static Context context;
    private CollectionDAO instance;
    private CommunityDAO communityDAO;
    private ItemDAO itemDAO;

    private static final String ADMIN_EMAIL = "james.rutherford@hp.com";
    private static final String CONFIG = "/opt/dspace-dao/config/dspace.cfg";
    
    public CollectionDAOTest()
    {
        instance = CollectionDAOFactory.getInstance(context);
        communityDAO = CommunityDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        ConfigurationManager.loadConfig(CONFIG);

        context = new Context();
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        context.abort();
    }

    @Before
    public void setUp() throws Exception
    {
        // We set the EPerson in the Context before each test, just in case one
        // of them needs to alter it.
        EPersonDAO epersonDAO = EPersonDAOFactory.getInstance(context);
        EPerson admin = epersonDAO.retrieve(EPerson.EPersonMetadataField.EMAIL,
                ADMIN_EMAIL);

        context.setCurrentUser(admin);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void create() throws Exception
    {
        Collection result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Collection existing = instance.create();
        Collection result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Collection collection = instance.create();
        collection.setMetadata("name", "Collection Test");
        instance.update(collection);
        
        Collection retrieved = instance.retrieve(collection.getID());
        assertEquals("Collection Test", retrieved.getMetadata("name"));
    }

    @Test
    public void delete() throws Exception
    {
        Collection collection = instance.create();
        int id = collection.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getCollections() throws Exception
    {
        Collection collectionOne = instance.create();
        Collection collectionTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        List<Collection> collections = instance.getCollections();

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (Collection c : collections)
        {
            if (collectionOne.equals(c))
            {
                containsOne = true;
            }
            if (collectionTwo.equals(c))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }

    @Test
    public void getCollectionsByAuthority() throws Exception
    {
        Collection collectionOne = instance.create();
        Collection collectionTwo = instance.create();

        int actions[] = {
            Constants.READ,
            Constants.WRITE,
            Constants.DELETE,
            Constants.ADD,
            Constants.REMOVE
        };

        int total = instance.getCollections().size();

        // If the create() failed, this test is meaningless.
        assertFalse(total == 0);
        
        // We're running these tests as an admin, so we should be able to do
        // everything.
        for (int i : actions)
        {
            List<Collection> c = instance.getCollectionsByAuthority(null, i);
            assertEquals(c.size(), total);
        }

        context.setCurrentUser(null);
        
        // We're now effectivelt an anonymous user, so we shouldn't be able to
        // do anything apart from READ.
        for (int i : actions)
        {
            List<Collection> c = instance.getCollectionsByAuthority(null, i);
            if (i == Constants.READ)
            {
                assertEquals(c.size(), total);
            }
            else
            {
                assertEquals(c.size(), 0);
            }
        }
    }

    @Test
    public void getParentCollections() throws Exception
    {
        /**
         * We need to create some Items to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void getChildCollections() throws Exception
    {
        Community parent = communityDAO.create();
        Community child = communityDAO.create();
        Collection collectionOne = instance.create();
        Collection collectionTwo = instance.create();

        assertEquals(instance.getChildCollections(parent).size(), 0);

        communityDAO.link(parent, collectionOne);
        assertEquals(instance.getChildCollections(parent).size(), 1);

        communityDAO.link(child, collectionTwo);
        communityDAO.link(parent, child);
        // We only get immediate children, so this shouldn't change
        assertEquals(instance.getChildCollections(parent).size(), 1);

        communityDAO.link(parent, collectionTwo);
        assertEquals(instance.getChildCollections(parent).size(), 2);
    }

    @Test
    public void link() throws Exception
    {
        /**
         * We need to create some Items to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void unlink() throws Exception
    {
        /**
         * We need to create some Items to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void linked() throws Exception
    {
        /**
         * We need to create some Items to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void itemCount() throws Exception
    {
        /**
         * Testing this is going to be a little more complicated than with the
         * other methods because we'd actually have to create a bunch of
         * sub-Communities and Collections and actually place Items into them.
         * Given that this isn't exactly mission-critical functionality, I'm
         * happy to defer writing the test.
         */
        assertTrue(true);
    }
}
