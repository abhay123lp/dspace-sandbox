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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Bitstream;
import org.dspace.content.proxy.BitstreamProxy;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;

/**
 * @author James Rutherford
 */
public class BitstreamDAOPostgres extends BitstreamDAO
{
    protected Context context;

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
     * @throws IOException
     * @throws SQLException
     */
    public abstract Bitstream create() throws AuthorizeException
    {
        // Store the bits
        int bitstreamID = BitstreamStorageManager.store(context, is);

        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + bitstreamID));

        // Set the format to "unknown"
        Bitstream bitstream = retrieve(bitstreamID);
        bitstream.setFormat(null);
        update(bitstream);

        return bitstream;
    }

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
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream register(int assetstore, String path)
        throws AuthorizeException
    {
        // Store the bits
        int bitstreamID =
            BitstreamStorageManager.register(context, assetstore, path);


        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + bitstreamID));

        // Set the format to "unknown"
        Bitstream bitstream = retrieve(bitstreamID);
        bitstream.setFormat(null);
        update(bitstream);

        return bitstream;
    }

    public Bitstream retrieve(int id)
    {
        Bitstream bitstream = super.retrieve(id);

        if (bitstream != null)
        {
            return bitstream;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "bitstream", id);

            if (row == null)
            {
                log.warn("bitstream " + id + " not found");
                return null;
            }

            bitstream = new Bitstream(context, id);
            populateBitstreamFromTableRow(bitstream, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<PersistentIdentifier> identifiers =
                identifierDAO.getPersistentIdentifiers(bitstream);
            bitstream.setPersistentIdentifiers(identifiers);

            context.cache(bitstream, id);

            return bitstream;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public abstract Bitstream retrieve(UUID uuid);

    public void update(Bitstream bitstream) throws AuthorizeException
    {
        super.update(bitstream);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "bitstream", bitstream.getID());

            if (row != null)
            {
                update(bitstream, row);
            }
            else
            {
                throw new RuntimeException("Didn't find bitstream " +
                        bitstream.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Bitstream bitstream, TableRow row)
        throws AuthorizeException
    {
        try
        {
            populateTableRowFromBitstream(bitstream, row);

            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void delete(int id) throws AuthorizeException
    {
        boolean oracle = false;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            oracle = true;
        }

        try
        {
            // Remove references to primary bitstreams in bundle
            String query = "update bundle set primary_bitstream_id = ";
            query += (oracle ? "''" : "Null") + " where primary_bitstream_id = ? ";
            DatabaseManager.updateQuery(context, query, id);

            // Remove bitstream itself
            BitstreamStorageManager.delete(context, id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public abstract List<Bitstream> getBitstreamsByBundle(Bundle bundle);
    public abstract List<Bitstream> getBitstreamsByItem(Item bitstream);

    public void link(Bitstream bitstream, Bundle bundle) throws AuthorizeException
    {
    }

    public void unlink(Bitstream bitstream, Bundle bundle) throws AuthorizeException
    {
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateBitstreamFromTableRow(Bitstream bitstream,
            TableRow row)
    {
    }

    private void populateTableRowFromBitstream(Bitstream bitstream,
            TableRow row)
    {
    }
}
