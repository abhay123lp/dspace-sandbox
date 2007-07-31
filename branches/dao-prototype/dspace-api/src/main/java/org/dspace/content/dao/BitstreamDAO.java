/*
 * BitstreamDAO.java
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

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;

/**
 * @author James Rutherford
 */
public abstract class BitstreamDAO extends ContentDAO
{
    protected Logger log = Logger.getLogger(BitstreamDAO.class);

    protected Context context;
    protected ExternalIdentifierDAO identifierDAO;

    public abstract Bitstream create();

    /**
     * Create a new bitstream, with a new ID. The checksum and file size are
     * calculated. This method does not check authorisation; other methods such
     * as Bundle.createBitstream() will check authorisation. The newly created
     * bitstream has the "unknown" format.
     * 
     * @param context DSpace context object
     * @param is the bits to put in the bitstream
     * 
     * @return the newly created bitstream
     * @throws AuthorizeException
     */
    public abstract Bitstream store(InputStream is)
        throws AuthorizeException, IOException;

    /**
     * Register a new bitstream, with a new ID. The checksum and file size are
     * calculated. This method does not check authorisation; other methods such
     * as Bundle.createBitstream() will check authorisation. The newly
     * registered bitstream has the "unknown" format.
     * 
     * @param context DSpace context object
     * @param is the bits to put in the bitstream
     * 
     * @return the newly created bitstream
     * @throws AuthorizeException
     */
    public abstract Bitstream register(int assetstore, String path)
        throws AuthorizeException, IOException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final Bitstream create(Bitstream bitstream)
        throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + bitstream.getID()));

        return bitstream;
    }

    public Bitstream retrieve(int id)
    {
        return (Bitstream) context.fromCache(Bitstream.class, id);
    }

    public Bitstream retrieve(UUID uuid)
    {
        return null;
    }

    public void update(Bitstream bitstream) throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, bitstream, Constants.WRITE);

        log.info(LogManager.getHeader(context, "update_bitstream",
                "bitstream_id=" + bitstream.getID()));
    }

    /**
     * Mark the bitstream as deleted. Actual removal doesn't happen until a
     * cleanup happens, and remove() is called.
     */
    public void delete(int id) throws AuthorizeException
    {
        Bitstream bitstream = retrieve(id);

        log.info(LogManager.getHeader(context, "delete_bitstream",
                "bitstream_id=" + id));

        context.removeCached(bitstream, id);

        AuthorizeManager.removeAllPolicies(context, bitstream);
    }
    
    /**
     * Actually remove the reference to the bitstream. Note that this doesn't
     * do anything to the actual files, just their representation in the
     * system.
     */
    public void remove(int id) throws AuthorizeException
    {
        Bitstream bitstream = retrieve(id);
        update(bitstream); // Sync in-memory object before removal

        AuthorizeManager.authorizeAction(context, bitstream, Constants.DELETE);
    }

    public abstract List<Bitstream> getBitstreamsByBundle(Bundle bundle);
    public abstract List<Bitstream> getDeletedBitstreams();
}
