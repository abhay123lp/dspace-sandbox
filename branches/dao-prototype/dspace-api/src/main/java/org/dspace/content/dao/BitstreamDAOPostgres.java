/*
 * BitstreamDAOPostgres.java
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.content.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.bitstore.BitstreamStorageManager;

/**
 * @author James Rutherford
 */
public class BitstreamDAOPostgres extends BitstreamDAO
{
    public BitstreamDAOPostgres(Context context)
    {
        this.context = context;
        this.identifierDAO =
            ExternalIdentifierDAOFactory.getInstance(context);
    }

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
    @Override
    public Bitstream create(InputStream is) throws AuthorizeException
    {
        // Store the bits
        TableRow row = null;
        try
        {
            row = BitstreamStorageManager.store(context, is);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        return create(row);
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
    @Override
    public Bitstream register(int assetstore, String path)
        throws AuthorizeException
    {
        // Store the bits
        TableRow row = null;
        try
        {
            row = BitstreamStorageManager.register(context, assetstore, path);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        return create(row);
    }

    /**
     * Because Bitstreams are created in an inherantly different way to the
     * other DSpaceObjects, we need a kind of "post create update" which is
     * what this method does.
     */
    private Bitstream create(TableRow row) throws AuthorizeException
    {
        int id = row.getIntColumn("bitstream_id");
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));

        Bitstream bitstream = new Bitstream(context, id);
        populateBitstreamFromTableRow(bitstream, row);
        bitstream.setFormat(null);
        bitstream.setIdentifier(new ObjectIdentifier(uuid));

        update(bitstream);

        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + id));

        return bitstream;
    }

    @Override
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
            List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(bitstream);
            bitstream.setExternalIdentifiers(identifiers);

            context.cache(bitstream, id);

            return bitstream;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Bitstream retrieve(UUID uuid)
    {
        Bitstream bitstream = super.retrieve(uuid);

        if (bitstream != null)
        {
            return bitstream;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "bitstream",
                    "uuid", uuid.toString());

            if (row == null)
            {
                log.warn("bitstream " + uuid + " not found");
                return null;
            }

            int id = row.getIntColumn("bitstream_id");
            bitstream = new Bitstream(context, id);
            populateBitstreamFromTableRow(bitstream, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(bitstream);
            bitstream.setExternalIdentifiers(identifiers);

            context.cache(bitstream, id);

            return bitstream;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
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

    @Override
    public void delete(int id) throws AuthorizeException
    {
        super.delete(id);

        boolean oracle = false;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            oracle = true;
        }

        try
        {
            // Remove references to primary bitstreams in bundle
            String query1 =
                "UPDATE bundle SET primary_bitstream_id = " +
                (oracle ? "''" : "Null") +
                " WHERE primary_bitstream_id = ? ";

            // Mark this bitstream as "deleted" (we don't actually remove the
            // row until a cleanup is performed).
            String query2 =
                "UPDATE Bitstream SET deleted = '1' " +
                "WHERE bitstream_id = ? ";

            DatabaseManager.updateQuery(context, query1, id);
            DatabaseManager.updateQuery(context, query2, id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME: This BADLY needs some sanity checking.
     */
    @Override
    public void remove(int id) throws AuthorizeException
    {
        super.remove(id);

        try
        {
            DatabaseManager.delete(context, "bitstream", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Bitstream> getBitstreamsByBundle(Bundle bundle)
    {
        try
        {
            // Get bitstreams
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bitstream_id FROM bundle2bitstream " +
                    "WHERE bundle_id = ? ", bundle.getID());

            List <Bitstream> bitstreams = new ArrayList<Bitstream>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("bitstream_id");
                bitstreams.add(retrieve(id));
            }

            return bitstreams;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Bitstream> getDeletedBitstreams()
    {
        try
        {
            // Get bitstreams
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bitstream_id FROM bitstream " +
                    "WHERE deleted = '1'");

            List <Bitstream> bitstreams = new ArrayList<Bitstream>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("bitstream_id");
                bitstreams.add(retrieve(id));
            }

            return bitstreams;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateBitstreamFromTableRow(Bitstream bitstream,
            TableRow row)
    {
        int sequenceID = row.getIntColumn("sequence_id");
        int storeNumber = row.getIntColumn("store_number");
        String internalID = row.getStringColumn("internal_id");

        String name = row.getStringColumn("name");
        String source = row.getStringColumn("source");
        String description = row.getStringColumn("description");
        String checksum = row.getStringColumn("checksum");
        String checksumAlgorithm = row.getStringColumn("checksum_algorithm");
        String userFormatDescription =
            row.getStringColumn("user_format_description");

        long sizeBytes = -1l;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            sizeBytes = new Long(row.getIntColumn("size_bytes"));
        }
        else
        {
            sizeBytes = row.getLongColumn("size_bytes");
        }

        int bitstreamFormatID = row.getIntColumn("bitstream_format_id");
        BitstreamFormat bitstreamFormat = null;
        try
        {
            bitstreamFormat =
                BitstreamFormat.find(context, bitstreamFormatID);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        bitstream.setSequenceID(sequenceID);
        bitstream.setName(name);
        bitstream.setSource(source);
        bitstream.setDescription(description);
        bitstream.setChecksum(checksum);
        bitstream.setChecksumAlgorithm(checksumAlgorithm);
        bitstream.setSize(sizeBytes);
        bitstream.setUserFormatDescription(userFormatDescription);
        bitstream.setFormat(bitstreamFormat);
        bitstream.setStoreNumber(storeNumber);
        bitstream.setInternalID(internalID);

        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        bitstream.setIdentifier(new ObjectIdentifier(uuid));
    }

    private void populateTableRowFromBitstream(Bitstream bitstream,
            TableRow row)
    {
        BitstreamFormat bitstreamFormat = bitstream.getFormat();
        int sequenceID = bitstream.getSequenceID();
        int storeNumber = bitstream.getStoreNumber();
        int bitstreamFormatID = bitstreamFormat.getID();
        long sizeBytes = bitstream.getSize();

        String name = bitstream.getName();
        String source = bitstream.getSource();
        String description = bitstream.getDescription();
        String checksum = bitstream.getChecksum();
        String checksumAlgorithm = bitstream.getChecksumAlgorithm();
        String userFormatDescription = bitstream.getUserFormatDescription();
        String internalID = bitstream.getInternalID();

        row.setColumn("sequence_id", sequenceID);
        row.setColumn("store_number", storeNumber);
        row.setColumn("bitstream_format_id", bitstreamFormatID);
        row.setColumn("size_bytes", sizeBytes);

        row.setColumn("name", name);
        row.setColumn("source", source);
        row.setColumn("internal_id", internalID);

        if (description == null)
        {
            row.setColumnNull("description");
        }
        else
        {
            row.setColumn("description", description);
        }

        if (checksum == null)
        {
            row.setColumnNull("checksum");
        }
        else
        {
            row.setColumn("checksum", checksum);
        }

        if (checksumAlgorithm == null)
        {
            row.setColumnNull("checksum_algorithm");
        }
        else
        {
            row.setColumn("checksum_algorithm", checksumAlgorithm);
        }

        if (userFormatDescription == null)
        {
            row.setColumnNull("user_format_description");
        }
        else
        {
            row.setColumn("user_format_description", userFormatDescription);
        }

        // FIXME: We should be setting this
//        row.setColumn("deleted", deleted);
    }
}
