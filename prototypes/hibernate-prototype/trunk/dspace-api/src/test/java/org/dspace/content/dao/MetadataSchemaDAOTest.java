/*
 * MetadataSchemaDAOTest.java
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

import org.dspace.content.MetadataSchema;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataSchemaDAOTest extends DAOTest implements CRUDTest
{
    private MetadataSchemaDAO instance;

    public MetadataSchemaDAOTest()
    {
        instance = MetadataSchemaDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        MetadataSchema result = instance.create();

        int id = result.getId();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        MetadataSchema existing = instance.create();
        MetadataSchema result = instance.retrieve(existing.getId());

        assertEquals(existing.getId(), result.getId());
    }

    @Test
    public void retrieveByName() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        MetadataSchema result = instance.retrieveByName(name);
        assertEquals(schema.getId(), result.getId());
    }

    @Test
    public void retrieveByNamespace() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        MetadataSchema result = instance.retrieveByNamespace(namespace);
        assertEquals(schema.getId(), result.getId());
    }

    @Test
    public void update() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        // That we can retrieve it by name implies that the update operation
        // was successful.
        MetadataSchema result = instance.retrieveByName(name);
        assertEquals(schema.getName(), result.getName());
    }

    @Test
    public void delete() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        instance.delete(schema.getId());

        // That we can retrieve it by name implies that the update operation
        // was successful.
        MetadataSchema result = instance.retrieveByName(name);
        assertNull(result);
    }

    @Test
    public void uniqueNamespace() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        assertTrue(instance.uniqueNamespace(schema.getId(), namespace));
        assertTrue(!instance.uniqueNamespace(-1, namespace));
    }

    @Test
    public void uniqueShortName() throws Exception
    {
        MetadataSchema schema = instance.create();
        
        // This is perhaps excessive
        String name = UUID.randomUUID().toString().substring(32);
        String namespace = UUID.randomUUID().toString();

        schema.setName(name);
        schema.setNamespace(namespace);
        instance.update(schema);

        assertTrue(instance.uniqueShortName(schema.getId(), name));
        assertTrue(!instance.uniqueShortName(-1, name));
    }

    @Test
    public void getMetadataSchemas() throws Exception
    {
        MetadataSchema schemaOne = instance.create();
        MetadataSchema schemaTwo = instance.create();
        boolean containsOne = false;
        boolean containsTwo = false;
        
        // This is perhaps excessive
        String nameOne = UUID.randomUUID().toString().substring(32);
        String nameTwo = UUID.randomUUID().toString().substring(32);
        String namespaceOne = UUID.randomUUID().toString();
        String namespaceTwo = UUID.randomUUID().toString();

        schemaOne.setName(nameOne);
        schemaTwo.setName(nameTwo);
        schemaOne.setNamespace(namespaceOne);
        schemaTwo.setNamespace(namespaceTwo);
        instance.update(schemaOne);
        instance.update(schemaTwo);

        List<MetadataSchema> schemas = instance.getMetadataSchemas();

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (MetadataSchema schema : schemas)
        {
            if (schemaOne.equals(schema))
            {
                containsOne = true;
            }
            if (schemaTwo.equals(schema))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }
}
