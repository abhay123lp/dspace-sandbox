/*
 * BitstreamFormatDAO.java
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

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.BitstreamFormat;
import org.dspace.storage.dao.CRUD;

/**
 * @author James Rutherford
 */
public abstract class BitstreamFormatDAO extends ContentDAO<BitstreamFormatDAO>
    implements CRUD<BitstreamFormat>
{
    protected Logger log = Logger.getLogger(BitstreamFormatDAO.class);

    protected Context context;

    public BitstreamFormatDAO(Context context)
    {
        this.context = context;
    }

    public BitstreamFormat create() throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can create bitstream formats");
        }

        return null;
    }

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final BitstreamFormat create(BitstreamFormat bitstreamFormat)
        throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "create_bitstream_format",
                "bitstream_format_id=" + bitstreamFormat.getID()));

        return bitstreamFormat;
    }

    public BitstreamFormat retrieve(int id)
    {
        return (BitstreamFormat) context.fromCache(BitstreamFormat.class, id);
    }

    public BitstreamFormat retrieve(UUID uuid)
    {
        return null;
    }

    public BitstreamFormat retrieveByMimeType(String mimeType)
    {
        return null;
    }

    public BitstreamFormat retrieveByShortDescription(String desc)
    {
        return null;
    }

    public void update(BitstreamFormat bitstreamFormat)
        throws AuthorizeException
    {
        // Check authorisation - only administrators can change formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can modify bitstream formats");
        }

        log.info(LogManager.getHeader(context, "update_bitstream_format",
                "bitstream_format_id=" + bitstreamFormat.getID()));
    }

    /**
     * Delete this bitstream format. This converts the types of any bitstreams
     * that may have this type to "unknown". Use this with care!
     */
    public void delete(int id) throws AuthorizeException
    {
        // Check authorisation - only administrators can delete formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = BitstreamFormat.findUnknown(context);

        if (unknown.getID() == id)
        {
            throw new IllegalArgumentException(
                    "The Unknown bitstream format may not be deleted.");
        }

        BitstreamFormat bitstreamFormat = retrieve(id);
        update(bitstreamFormat); // Sync in-memory object before removal

        context.removeCached(bitstreamFormat, id);
    }

    public abstract List<BitstreamFormat> getBitstreamFormats();
    public abstract List<BitstreamFormat> getBitstreamFormats(String extension);

    /**
     * Retrieve all non-internal bitstream formats from the registry. The
     * "unknown" format is not included, and the formats are ordered by support
     * level (highest first) first then short description.
     */
    public abstract List<BitstreamFormat> getBitstreamFormats(boolean internal);
}
