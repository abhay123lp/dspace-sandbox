/*
 * BitstreamFormatDAOTest.java
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

import java.util.UUID;

import org.dspace.content.BitstreamFormat;
import org.dspace.storage.dao.CRUDTest;
import org.dspace.storage.dao.DAOTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class BitstreamFormatDAOTest extends DAOTest
    implements CRUDTest
{
    private BitstreamFormatDAO instance;
    
    public BitstreamFormatDAOTest()
    {
        instance = BitstreamFormatDAOFactory.getInstance(context);
    }

    @Test
    public void create() throws Exception
    {
        BitstreamFormat result = instance.create();
        result.setSupportLevel(BitstreamFormat.SUPPORTED);
        instance.update(result);

        int id = result.getID();

        assertTrue(id > 0);
    }

    @Test
    public void retrieve() throws Exception
    {
        BitstreamFormat existing = instance.create();
        existing.setSupportLevel(BitstreamFormat.SUPPORTED);
        instance.update(existing);

        BitstreamFormat result = instance.retrieve(existing.getID());

        assertEquals(existing, result);
    }

    @Test
    public void retrieveByMimeType() throws Exception
    {
        BitstreamFormat existing = instance.create();
        String mime = UUID.randomUUID().toString();
        existing.setSupportLevel(BitstreamFormat.SUPPORTED);
        existing.setMIMEType(mime);
        instance.update(existing);

        BitstreamFormat result = instance.retrieveByMimeType(mime);

        assertEquals(existing, result);
    }

    @Test
    public void retrieveByShortDescription() throws Exception
    {
        BitstreamFormat existing = instance.create();
        String desc = UUID.randomUUID().toString();
        existing.setSupportLevel(BitstreamFormat.SUPPORTED);
        existing.setShortDescription(desc);
        instance.update(existing);

        BitstreamFormat result = instance.retrieveByShortDescription(desc);

        assertEquals(existing, result);
    }

    @Test
    public void update() throws Exception
    {
        BitstreamFormat existing = instance.create();
        String desc = UUID.randomUUID().toString();
        existing.setSupportLevel(BitstreamFormat.SUPPORTED);
        existing.setShortDescription(desc);
        instance.update(existing);

        BitstreamFormat result = instance.retrieve(existing.getID());

        assertEquals(desc, result.getShortDescription());
    }

    @Test
    public void delete() throws Exception
    {
        BitstreamFormat format = instance.create();
        format.setSupportLevel(BitstreamFormat.SUPPORTED);
        instance.update(format);

        int id = format.getID();

        instance.delete(id);

        assertNull(instance.retrieve(id));
    }

    @Test
    public void getBitstreamFormats() throws Exception
    {
        // We test getBitstreamFormats() with the following arguments:
        //  * null (get all)
        //  * String extension
        //  * boolean internal

        BitstreamFormat formatOne = instance.create();
        BitstreamFormat formatTwo = instance.create();
        formatOne.setSupportLevel(BitstreamFormat.SUPPORTED);
        formatOne.setShortDescription("BitstreamFormat Test One");
        formatTwo.setSupportLevel(BitstreamFormat.SUPPORTED);
        formatTwo.setShortDescription("BitstreamFormat Test Two");
        instance.update(formatOne);
        instance.update(formatTwo);

        boolean containsOne = false;
        boolean containsTwo = false;

        // We have to do it this way because even though we have a type-safe
        // List, Java insists on using Object.equals() which will fail, even
        // though the objects are actually equal.
        for (BitstreamFormat format : instance.getBitstreamFormats())
        {
            if (formatOne.equals(format))
            {
                containsOne = true;
            }
            if (formatTwo.equals(format))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);

        containsOne = false;
        containsTwo = false;

        String ext[] = { UUID.randomUUID().toString().substring(0, 8) };
        formatOne.setExtensions(ext);
        formatTwo.setExtensions(ext);
        instance.update(formatOne);
        instance.update(formatTwo);

        for (BitstreamFormat format : instance.getBitstreamFormats(ext[0]))
        {
            if (formatOne.equals(format))
            {
                containsOne = true;
            }
            if (formatTwo.equals(format))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);

        containsOne = false;
        containsTwo = false;

        formatOne.setInternal(true);
        formatTwo.setInternal(true);
        instance.update(formatOne);
        instance.update(formatTwo);

        for (BitstreamFormat format : instance.getBitstreamFormats(false))
        {
            if (formatOne.equals(format))
            {
                fail();
            }
            if (formatTwo.equals(format))
            {
                fail();
            }
        }

        for (BitstreamFormat format : instance.getBitstreamFormats(true))
        {
            if (formatOne.equals(format))
            {
                containsOne = true;
            }
            if (formatTwo.equals(format))
            {
                containsTwo = true;
            }
        }

        assertTrue(containsOne);
        assertTrue(containsTwo);
    }
}
