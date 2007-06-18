/*
 * Bitstream.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.content.uri.dao.PersistentIdentifierDAO;
import org.dspace.content.uri.dao.PersistentIdentifierDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Class representing bitstreams stored in the DSpace system.
 * <P>
 * When modifying the bitstream metadata, changes are not reflected in the
 * database until <code>update</code> is called. Note that you cannot alter
 * the contents of a bitstream; you need to create a new bitstream.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Bitstream extends DSpaceObject
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Bitstream.class);

    private int sequenceID;
    private String name;
    private String source;
    private String description;
    private String checksum;
    private String checksumAlgorithm;
    private Long sizeBytes;
    private String userFormatDescription;
    private BitstreamFormat bitstreamFormat;
    private int storeNumber;
    private int internalID;

    public Bitstream(Context context, int id)
    {
        this.id = id;
        this.context = context;
        this.dao = BitstreamDAOFactory.getInstance(context);
    }

    public int getSequenceID()
    {
        return sequenceID;
    }

    public void setSequenceID(int sequenceID)
    {
        this.sequenceID = sequenceID;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the source of this bitstream - typically the filename with path
     * information (if originally provided) or the name of the tool that
     * generated this bitstream
     * 
     * @return the source of the bitstream
     */
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get the checksum of the content of the bitstream.
     * 
     * @return the checksum
     */
    public String getChecksum()
    {
        return checksum;
    }

    /**
     * Get the algorithm used to calculate the checksum
     * 
     * @return the algorithm, e.g. "MD5"
     */
    public String getChecksumAlgorithm()
    {
        return checksumAlgorithm;
    }

    /**
     * Get the size of the bitstream
     * 
     * @return the size in bytes
     */
    public long getSize()
    {
        return sizeBytes;
    }

    /**
     * Set the user's format description. This implies that the format of the
     * bitstream is uncertain, and the format is set to "unknown."
     * 
     * @param desc the user's description of the format
     * @throws SQLException
     */
    public void setUserFormatDescription(String desc)
    {
        setFormat(null);
        this.userFormatDescription = desc;
    }

    /**
     * Get the user's format description. Returns null if the format is known by
     * the system.
     * 
     * @return the user's format description.
     */
    public String getUserFormatDescription()
    {
        return userFormatDescription;
    }

    /**
     * Get the description of the format - either the user's or the description
     * of the format defined by the system.
     * 
     * @return a description of the format.
     */
    public String getFormatDescription()
    {
        if (bitstreamFormat.getShortDescription().equals("Unknown"))
        {
            // Get user description if there is one
            if (userFormatDescription == null)
            {
                return "Unknown";
            }

            return userFormatDescription;
        }

        // not null or Unknown
        return bitstreamFormat.getShortDescription();
    }

    /**
     * Get the format of the bitstream
     * 
     * @return the format of this bitstream
     */
    public BitstreamFormat getFormat()
    {
        return bitstreamFormat;
    }

    /**
     * Set the format of the bitstream. If the user has supplied a type
     * description, it is cleared. Passing in <code>null</code> sets the type
     * of this bitstream to "unknown".
     * 
     * @param f
     *            the format of this bitstream, or <code>null</code> for
     *            unknown
     * @throws SQLException
     */
    public void setFormat(BitstreamFormat f) throws SQLException
    {
        // FIXME: Would be better if this didn't throw an SQLException,
        // but we need to find the unknown format!
        if (f == null)
        {
            // Use "Unknown" format
            bitstreamFormat = BitstreamFormat.findUnknown(context);
        }
        else
        {
            bitstreamFormat = f;
        }

        // Remove user type description
        userFormatDescription = null;

        // Update the ID in the table row
        bitstreamFormatID = bitstreamFormat.getID();
    }

    /**
     * Retrieve the contents of the bitstream
     * 
     * @return a stream from which the bitstream can be read.
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public InputStream retrieve() throws IOException, SQLException,
            AuthorizeException
    {
        // Maybe should return AuthorizeException??
        AuthorizeManager.authorizeAction(context, this, Constants.READ);

        return BitstreamStorageManager.retrieve(context, bRow
                .getIntColumn("bitstream_id"));
    }

    /**
     * Get the bundles this bitstream appears in
     * 
     * @return array of <code>Bundle</code> s this bitstream appears in
     * @throws SQLException
     */
    public Bundle[] getBundles() throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "bundle",
                "SELECT bundle.* FROM bundle, bundle2bitstream WHERE " + 
                "bundle.bundle_id=bundle2bitstream.bundle_id AND " +
                "bundle2bitstream.bitstream_id= ? ",
                 bRow.getIntColumn("bitstream_id"));

        // Build a list of Bundle objects
        List bundles = new ArrayList();

        while (tri.hasNext())
        {
            TableRow r = tri.next();

            // First check the cache
            Bundle fromCache = (Bundle) context.fromCache(Bundle.class, r
                    .getIntColumn("bundle_id"));

            if (fromCache != null)
            {
                bundles.add(fromCache);
            }
            else
            {
                bundles.add(new Bundle(context, r));
            }
        }

        // close the TableRowIterator to free up resources
        tri.close();

        Bundle[] bundleArray = new Bundle[bundles.size()];
        bundleArray = (Bundle[]) bundles.toArray(bundleArray);

        return bundleArray;
    }
    
    /**
     * Determine if this bitstream is registered
     * 
     * @return true if the bitstream is registered, false otherwise
     */
    public boolean isRegisteredBitstream()
    {
        return BitstreamStorageManager.isRegisteredBitstream(internalID);
    }
    
    /**
     * Get the asset store number where this bitstream is stored
     * 
     * @return the asset store number of the bitstream
     */
    public int getStoreNumber()
    {
        return storeNumber;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public int getType()
    {
        return Constants.BITSTREAM;
    }

    /** Deprecated by the introduction of DAOs */
    @Deprecated
    public Bitstream(Context context, TableRow row) throws SQLException
    {
        this(context, row.getIntColumn("bitstream_id"));
    }

    @Deprecated
    public static Bitstream find(Context context, int id) throws SQLException
    {
        return BitstreamDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    public static Bitstream find(Context context, UUID uuid) throws SQLException
    {
        return BitstreamDAOFactory.getInstance(context).retrieve(uuid);
    }

    @Deprecated
    static Bitstream create(Context context, InputStream is)
            throws IOException, SQLException
    {
        return BitstreamDAOFactory.getInstance(context).create();
    }

    @Deprecated
    static Bitstream register(Context context, int assetstore,
            String bitstreamPath) throws IOException, SQLException
    {
        return BitstreamDAOFactory.getInstance(context).register(assetstore,
                bitstreamPath);
    }

    @Deprecated
    public void update() throws SQLException, AuthorizeException
    {
        dao.update(this);
    }

    @Deprecated
//    void delete() throws SQLException
    public void delete() throws SQLException
    {
        dao.delete(this.getID());
    }
}
