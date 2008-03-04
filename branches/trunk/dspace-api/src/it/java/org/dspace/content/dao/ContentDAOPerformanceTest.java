/*
 * ContentDAOPerformanceTest.java
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
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;

public class ContentDAOPerformanceTest extends DAOTest
{
    private BitstreamDAO bitstreamDAO;
    private BundleDAO bundleDAO;
    private ItemDAO itemDAO;
    private CollectionDAO collectionDAO;
    private CommunityDAO communityDAO;

    public ContentDAOPerformanceTest()
    {
        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        bundleDAO = BundleDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
        communityDAO = CommunityDAOFactory.getInstance(context);
    }

    @Test
    public void createAndRetrieve() throws Exception
    {
        long start = 0;
        long checkpoint = 0;
        int n = 10;

        System.out.print("creating " + n + " items... ");
        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
        {
            Item item = itemDAO.create();
            item.setArchived(true);
            item.setWithdrawn(false);
            addRandomDC(item);
            itemDAO.update(item);
        }
        checkpoint = System.currentTimeMillis();
        System.out.println("[" + ((checkpoint - start) / 1000f) + " secs]");

        System.out.print("retrieving all items... ");
        start = System.currentTimeMillis();

        List<Item> items = itemDAO.getItems();
        
        checkpoint = System.currentTimeMillis();
        System.out.println("[" + ((checkpoint - start) / 1000f) + " secs]");
        System.out.println("total items: " + items.size());
    }

    private void addRandomDC(Item item)
    {
        String elements[] = { "title", "contributor", "subject", "identifier" };
        String qualifiers[] = { null, "author", null, "uri" };

        for (int i = 0; i < elements.length; i++)
        {
            item.addMetadata("dc", elements[i], qualifiers[i], "en",
                    UUID.randomUUID().toString());
        }
    }
}
