/*
 * SupervisedItemDAOTest.java
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

import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SupervisedItemDAOTest
{
    private static Context context;
    private SupervisedItemDAO instance;
    private WorkspaceItemDAO wsiDAO;
    private GroupDAO groupDAO;
    private EPersonDAO epersonDAO;

    private static final String ADMIN_EMAIL = "james.rutherford@hp.com";
    private static final String CONFIG = "/opt/dspace-dao/config/dspace.cfg";
    
    public SupervisedItemDAOTest()
    {
        instance = SupervisedItemDAOFactory.getInstance(context);
        wsiDAO = WorkspaceItemDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
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
        context.setIgnoreAuthorization(false);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void getSupervisedItems() throws Exception
    {
        WorkspaceItem wsi = wsiDAO.create();
        Group group = groupDAO.create();
        EPerson eperson = epersonDAO.create();
        List<SupervisedItem> items = null;

        // Put the eperson into the supervising group
        groupDAO.link(group, eperson);

        // No supervisions defined yet
        items = instance.getSupervisedItems();
        for (SupervisedItem item : items)
        {
            if (item.getID() == wsi.getID())
            {
                fail();
            }
        }
        items = instance.getSupervisedItems(eperson);
        for (SupervisedItem item : items)
        {
            if (item.getID() == wsi.getID())
            {
                fail();
            }
        }

        // Now establish the supervision of the workspace item by the above
        // group
        groupDAO.link(group, wsi);

        boolean success = false;
        items = instance.getSupervisedItems();

        for (SupervisedItem item : items)
        {
            if (item.getID() == wsi.getID())
            {
                success = true;
            }
        }

        assertTrue(success);

        success = false;
        items = instance.getSupervisedItems(eperson);

        for (SupervisedItem item : items)
        {
            if (item.getID() == wsi.getID())
            {
                success = true;
            }
        }

        assertTrue(success);
    }
}
