/*
 * MetadataValueDAOTest.java
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
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataValueDAOTest extends DAOTest implements CRUDTest
{
    private MetadataValueDAO instance;
    private MetadataFieldDAO fieldDAO;
    private MetadataSchemaDAO schemaDAO;
    private ItemDAO itemDAO;
    
    public MetadataValueDAOTest()
    {
        instance = MetadataValueDAOFactory.getInstance(context);
        fieldDAO = MetadataFieldDAOFactory.getInstance(context);
        schemaDAO = MetadataSchemaDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        MetadataValue result = instance.create();

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        MetadataValue existing = instance.create();
        MetadataValue result = instance.retrieve(existing.getID());

        assertEquals(existing.getID(), result.getID());
    }

    @Test
    public void update() throws Exception
    {
        Item item = itemDAO.create();
        MetadataValue value = instance.create();
        MetadataField field = fieldDAO.create();
        MetadataSchema schema = schemaDAO.create();

        // Set up the field
        field.setSchemaID(schema.getID());
        field.setElement("Element One");
        fieldDAO.update(field);

        // All of this stuff has to be set
        value.setFieldID(field.getID());
        value.setItemID(item.getId());
        value.setValue("MetadataValue Test");
        instance.update(value);
        
        MetadataValue result = instance.retrieve(value.getID());
        assertEquals("MetadataValue Test", result.getValue());
    }

    @Test
    public void delete() throws Exception
    {
        MetadataValue value = instance.create();
        int id = value.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getMetadataValues() throws Exception
    {
        Item item = itemDAO.create();
        MetadataValue valueOne = instance.create();
        MetadataValue valueTwo = instance.create();
        MetadataField field = fieldDAO.create();
        MetadataSchema schema = schemaDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        // Set up the field
        field.setSchemaID(schema.getID());
        field.setElement("Element One");
        fieldDAO.update(field);

        // All of this stuff has to be set
        valueOne.setFieldID(field.getID());
        valueOne.setItemID(item.getId());
        valueOne.setValue(UUID.randomUUID().toString());
        valueTwo.setFieldID(field.getID());
        valueTwo.setItemID(item.getId());
        valueTwo.setValue(UUID.randomUUID().toString());
        instance.update(valueOne);
        instance.update(valueTwo);

        List<MetadataValue> values = instance.getMetadataValues(field.getID());

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (MetadataValue value : values)
        {
            if (valueOne.equals(value))
            {
                containsOne = true;
            }
            if (valueTwo.equals(value))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }
}
