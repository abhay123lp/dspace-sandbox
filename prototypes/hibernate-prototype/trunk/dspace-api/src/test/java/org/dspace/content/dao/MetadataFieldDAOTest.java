/*
 * MetadataFieldDAOTest.java
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

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataFieldDAOTest extends DAOTest implements CRUDTest
{
    private MetadataFieldDAO instance;
    private MetadataSchemaDAO schemaDAO;

    public MetadataFieldDAOTest()
    {
        instance = MetadataFieldDAOFactory.getInstance(context);
        schemaDAO = MetadataSchemaDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        MetadataField result = instance.create();

        int id = result.getId();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        MetadataField existing = instance.create();
        MetadataField result = instance.retrieve(existing.getId());

        assertEquals(existing.getId(), result.getId());
    }

    @Test
    public void update() throws Exception
    {
        MetadataField field = instance.create();
        MetadataSchema schema = schemaDAO.create();

        field.setSchemaID(schema.getId());
        field.setElement("MetadataField Test");
        instance.update(field);
        
        MetadataField result = instance.retrieve(field.getId());
        assertEquals("MetadataField Test", result.getElement());
    }

    @Test
    public void delete() throws Exception
    {
        MetadataField field = instance.create();
        MetadataSchema schema = schemaDAO.create();

        field.setSchemaID(schema.getId());
        field.setElement("MetadataField Test");
        instance.update(field);
        int id = field.getId();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void schemaChanged() throws Exception
    {
        MetadataField field = instance.create();
        MetadataSchema schemaOne = schemaDAO.create();
        MetadataSchema schemaTwo = schemaDAO.create();

        assertTrue(!instance.schemaChanged(field));

        field.setSchemaID(schemaOne.getId());
        field.setElement("MetadataField Test");
        assertTrue(instance.schemaChanged(field));
        
        instance.update(field);
        assertTrue(!instance.schemaChanged(field));

        field.setSchemaID(schemaTwo.getId());
        assertTrue(instance.schemaChanged(field));
    }

    @Test
    public void unique() throws Exception
    {
        MetadataField field = instance.create();
        MetadataSchema schema = schemaDAO.create();

        // This may be excessive, but it does guarantee that the field really
        // should show up as being unique until we force a duplicate.
        String element = UUID.randomUUID().toString();
        String qualifier = UUID.randomUUID().toString();

        int id = field.getId();
        field.setSchemaID(schema.getId());
        field.setElement(element);
        field.setQualifier(qualifier);
        instance.update(field);

        assertTrue(instance.unique(id, schema.getId(), element, qualifier));
        assertTrue(!instance.unique(-1, schema.getId(), element, qualifier));
    }

    @Test
    public void getMetadataFields() throws Exception
    {
        MetadataField fieldOne = instance.create();
        MetadataField fieldTwo = instance.create();
        MetadataSchema schema = schemaDAO.create();
        boolean containsOne = false;
        boolean containsTwo = false;

        // Set up the fields so that they appear
        fieldOne.setSchemaID(schema.getId());
        fieldOne.setElement("Element One");
        fieldTwo.setSchemaID(schema.getId());
        fieldTwo.setElement("Element Two");
        instance.update(fieldOne);
        instance.update(fieldTwo);

        List<MetadataField> fields = instance.getMetadataFields();

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (MetadataField field : fields)
        {
            if (fieldOne.equals(field))
            {
                containsOne = true;
            }
            if (fieldTwo.equals(field))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }
}
