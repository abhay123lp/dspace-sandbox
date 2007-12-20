/*
 * SubscriptionDAOTest.java
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

import org.dspace.content.Collection;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class SubscriptionDAOTest extends DAOTest implements CRUDTest
{
    private SubscriptionDAO instance;
    private EPersonDAO epersonDAO;
    private CollectionDAO collectionDAO;
    
    public SubscriptionDAOTest()
    {
        instance = SubscriptionDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        Subscription result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        Subscription existing = instance.create();
        EPerson eperson = epersonDAO.create();
        Collection collection = collectionDAO.create();
        existing.setEPersonID(eperson.getId());
        existing.setCollectionID(collection.getId());
        instance.update(existing);

        Subscription result = instance.retrieve(existing.getID());
        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Subscription sub = instance.create();
        EPerson eperson = epersonDAO.create();
        Collection collection = collectionDAO.create();
        sub.setEPersonID(eperson.getId());
        sub.setCollectionID(collection.getId());
        instance.update(sub);

        Subscription result = instance.retrieve(sub.getID());
        assertEquals(result.getEPersonID(), eperson.getId());
        assertEquals(result.getCollectionID(), collection.getId());
    }

    @Test
    public void delete() throws Exception
    {
        Subscription sub = instance.create();
        int id = sub.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void isSubscribed() throws Exception
    {
        Subscription sub = instance.create();
        EPerson eperson = epersonDAO.create();
        Collection collection = collectionDAO.create();

        assertTrue(!instance.isSubscribed(eperson, collection));
        
        sub.setEPersonID(eperson.getId());
        sub.setCollectionID(collection.getId());
        instance.update(sub);

        assertTrue(instance.isSubscribed(eperson, collection));
    }

    @Test
    public void getSubscriptions() throws Exception
    {
        Subscription subOne = instance.create();
        Subscription subTwo = instance.create();
        EPerson eperson = epersonDAO.create();
        Collection collection = collectionDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        for (Subscription sub : instance.getSubscriptions())
        {
            if (sub.equals(subOne))
            {
                containsOne = true;
            }
            if (sub.equals(subTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        for (Subscription sub : instance.getSubscriptions(eperson))
        {
            if (sub.equals(subOne) || sub.equals(subTwo))
            {
                fail();
            }
        }
        
        // Now we associate the epeople and collections to the subscriptions
        subOne.setEPersonID(eperson.getId());
        subTwo.setEPersonID(eperson.getId());
        subOne.setCollectionID(collection.getId());
        subTwo.setCollectionID(collection.getId());
        instance.update(subOne);
        instance.update(subTwo);

        for (Subscription sub : instance.getSubscriptions())
        {
            if (sub.equals(subOne))
            {
                containsOne = true;
            }
            if (sub.equals(subTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;

        for (Subscription sub : instance.getSubscriptions(eperson))
        {
            if (sub.equals(subOne))
            {
                containsOne = true;
            }
            if (sub.equals(subTwo))
            {
                containsTwo = true;
            }
        }
        assertTrue(containsOne);
        assertTrue(containsTwo);
        containsOne = false;
        containsTwo = false;
    }
}
