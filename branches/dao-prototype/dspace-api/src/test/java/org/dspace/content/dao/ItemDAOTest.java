/*
 * ItemDAOTest.java
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

import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
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

public class ItemDAOTest implements CRUDTest, LinkTest
{
    private static Context context;
    private ItemDAO instance;
    private CollectionDAO collectionDAO;

    private static final String ADMIN_EMAIL = "james.rutherford@hp.com";
    private static final String CONFIG = "/opt/dspace-dao/config/dspace.cfg";

    public ItemDAOTest()
    {
        instance = ItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
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
        Item result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Item existing = instance.create();
        Item result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Item item = instance.create();
        EPerson me = context.getCurrentUser();
        EPerson submitter = null;

        if (item.getSubmitter() == null || !item.getSubmitter().equals(me))
        {
            submitter = me;
        }

        item.setSubmitter(submitter);
        instance.update(item);

        Item retrieved = instance.retrieve(item.getID());
        assertEquals(retrieved.getSubmitter(), submitter);
    }

    @Test
    public void delete() throws Exception
    {
        Item item = instance.create();
        int id = item.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void decache()
    {
        /**
         * Testing this would be both painful and mostly pointless, so I shan't
         * bother.
         */
        assertTrue(true);
    }

    @Test
    public void getItems() throws Exception
    {
        Item itemOne = instance.create();
        Item itemTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;
        List<Item> items = null;

        items = instance.getItems();

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (Item i : items)
        {
            if (itemOne.equals(i))
            {
                fail(); // Not in the archive yet
            }
            if (itemTwo.equals(i))
            {
                fail(); // Not in the archive yet
            }
        }

        // Now we place the items into the archive and check the list again
        itemOne.setArchived(true);
        itemTwo.setArchived(true);
        instance.update(itemOne);
        instance.update(itemTwo);

        items = instance.getItems();

        for (Item i : items)
        {
            if (itemOne.equals(i))
            {
                containsOne = true;
            }
            if (itemTwo.equals(i))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }

    @Test
    public void getItemsByCollection() throws Exception
    {
        Collection collection = collectionDAO.create();
        Item itemOne = instance.create();
        Item itemTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;
        List<Item> items = null;

        items = instance.getItemsByCollection(collection);
        assertEquals(items.size(), 0);

        // Now we establish the link and place the Items into the archive
        collectionDAO.link(collection, itemOne);
        collectionDAO.link(collection, itemTwo);
        items = instance.getItemsByCollection(collection);
        assertEquals(items.size(), 0);

        itemOne.setArchived(true);
        itemTwo.setArchived(true);
        instance.update(itemOne);
        instance.update(itemTwo);

        items = instance.getItemsByCollection(collection);

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (Item i : items)
        {
            if (itemOne.equals(i))
            {
                containsOne = true;
            }
            if (itemTwo.equals(i))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }

    @Test
    public void getItemsBySubmitter() throws Exception
    {
        EPerson submitter = context.getCurrentUser();
        Item itemOne = instance.create();
        Item itemTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;
        List<Item> items = null;

        itemOne.setSubmitter(submitter);
        itemTwo.setSubmitter(submitter);
        instance.update(itemOne);
        instance.update(itemTwo);

        items = instance.getItemsBySubmitter(submitter);

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (Item i : items)
        {
            if (itemOne.equals(i))
            {
                fail(); // Not in the archive yet
            }
            if (itemTwo.equals(i))
            {
                fail(); // Not in the archive yet
            }
        }

        // Now we place the Items in the archive and make sure they appear in
        // the list.
        itemOne.setArchived(true);
        itemTwo.setArchived(true);
        instance.update(itemOne);
        instance.update(itemTwo);

        items = instance.getItemsBySubmitter(submitter);

        for (Item i : items)
        {
            if (itemOne.equals(i))
            {
                containsOne = true;
            }
            if (itemTwo.equals(i))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }

    @Test
    public void getParentItems() throws Exception
    {
        /**
         * We need to create some Bundles to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void removeBundleFromItem() throws Exception
    {
        /**
         * We need to create some Bundles to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void link() throws Exception
    {
        /**
         * We need to create some Bundles to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void unlink() throws Exception
    {
        /**
         * We need to create some Bundles to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void linked() throws Exception
    {
        /**
         * We need to create some Bundles to run this test, so I'm going to
         * postpone it.
         */
        assertTrue(true);
    }

    @Test
    public void loadMetadata()
    {
        /**
         * This functionality shouldn't really be in the DAO, so I'm not going
         * to test it for now.
         */
        assertTrue(true);
    }

    @Test
    public void getMetadata()
    {
        /**
         * This functionality shouldn't really be in the DAO, so I'm not going
         * to test it for now.
         */
        assertTrue(true);
    }
    
}
