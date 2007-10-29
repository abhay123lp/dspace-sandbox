/*
 * InternalAIP.java
 *
 * Version: $Revision: 1.19 $
 *
 * Date: $Date: 2006/03/30 02:46:42 $
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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.sql.SQLException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.AIPIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageException;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * An instance of InternalAIP represents an "internal" Archival
 * Information Package (AIP), which is a complete and self-contained
 * record of a DSpace Object kept in the asset store as an independent
 * bitstream.  It is implemented as a METS manifest, which refers to
 * other bitstreams by reference to their asset-store locations.  Other
 * attributes of the DSpace Object are recorded in the AIP as metadata,
 * some inline and some in external bitstreams also referenced by direct
 * asset-store locations.  This lets the meaning of the AIP remain
 * completely independent of the RDBMS, although it does rely on other
 * files in the asset store.
 * <p>
 * Although AIPs themselves are independent of the database, a table is
 * still needed to maintain AIPs while the DSpace archive is operating.
 * An InternalAIP corresponds to a row in the DB table used to keep track
 * of the internal AIP bitstream(s) that corresponds to each DSpace Object.
 * When an AIP is updated (e.g. after the object changes) it has to
 * be able to find the old AIP (and any auxiliary files) to delete it.
 * <p>
 * Since the whole purpose of Internal AIPs is to recover an archive
 * from the asset store alone, without the RDBMS, this class includes
 * methods to rebuild the InternalAIP tables in an empty database.  It
 * requires the Bitstream table, but the the Bitstream implementation
 * can also rebuild its RDBMS tables starting from the asset store.  The
 * AIPManager application will crawl over all Bitstreams, calling
 * checkAIPBitstream() to re-create information in the InternalAIP table
 * from assets.  Given the InternalAIP table, it can reconstruct the
 * rest of object model from the AIPs using restoreFromAIP().
 * <p>
 * Configuration:<br>
 *   aip.packager = plugin-name of ingest/dissemination packager for AIPs.
 *
 * @author Larry Stone
 * @version $Revision: 1.12 $
 */
public class InternalAIP
    implements Runnable
{
    // ---------- Statics and Constants

    /** log4j category */
    private static Logger log = Logger.getLogger(InternalAIP.class);

    // AIP packager's name, configurable but "AIP" is the default.
    private static final String PACKAGER_NAME =
        (ConfigurationManager.getProperty("aip.packager") == null) ?
          "AIP" : ConfigurationManager.getProperty("aip.packager");

    // fixed params required to drive the internal-AIP packager.
    private static PackageParameters probePkgParams = new PackageParameters();
    static
    {
        probePkgParams.addProperty("internal", "true");
    };

    // ---------- Instance variables

    // remember context it was created with
    private Context aipContext;

    // row for this object
    private TableRow aipRow;

    // for run()
    private InputStream aipInputStream;
    private Bitstream aipBitstream;

    private InternalAIP(Context context, TableRow row)
        throws SQLException
    {
        aipContext = context;
        aipRow = row;
        context.cache(this, row.getIntColumn("internalaip_id"));
    }

    /**
     * Finds the AIP record for an existing object in the data model.
     * null if none found
     */
    public static InternalAIP find(Context context, DSpaceObject dso)
        throws SQLException
    {
        // NOTE: Can't use context's cache because we don't know primary key
        // until we find the row, and it's indexed by DSO type + id.  d'oh.

        int hid = HandleManager.getID(context, dso.getHandle());
        if (hid < 0)
            return null;
        else
            return findByHandleID(context, hid);
    }

    /**
     * Finds the AIP record with the Handle ID (index into handle table)
     * See HandleManager.getID().
     * @return InternalAIP found, or null if none matched.
     */
    public static InternalAIP findByHandleID(Context context, int hid)
        throws SQLException
    {
        TableRow row = DatabaseManager.querySingleTable(context,
            "InternalAIP",
            "SELECT * FROM InternalAIP WHERE handle_id = ? ;",
            hid);
        if (row == null)
        {
            if (log.isDebugEnabled())
                log.debug(LogManager.getHeader(context, "find_internalaip",
                            "not_found,resource_handle_id="+String.valueOf(hid)));
            return null;
        }

        if (log.isDebugEnabled())
            log.debug(LogManager.getHeader(context, "find_internalaip",
                            "internalaip_id="+String.valueOf(row.getIntColumn("internalaip_id"))));
        return new InternalAIP(context, row);
    }

    /**
     * find AIP record for given internal table ID
     * @return InternalAIP found, or null if none matched.
     */
    public static InternalAIP find(Context context, int id)
        throws SQLException
    {
        // First check the cache
        InternalAIP fromCache = (InternalAIP) context
                .fromCache(InternalAIP.class, id);
        if (fromCache != null)
            return fromCache;

        TableRow row = DatabaseManager.find(context, "InternalAIP", id);
        if (row == null)
        {
            if (log.isDebugEnabled())
                log.debug(LogManager.getHeader(context, "find_internalAIP",
                        "not_found,internalAIP_id=" + id));
            return null;
        }

        if (log.isDebugEnabled())
            log.debug(LogManager.getHeader(context, "find_internalAIP",
                    "internalIP_id=" + id));
        return new InternalAIP(context, row);
    }

    /**
     * Creates a new internal AIP entry for an existing DSpace object.
     * @return internalAIP
     */
    public static InternalAIP create(Context context, DSpaceObject dso)
        throws SQLException
    {
        TableRow row = DatabaseManager.create(context, "InternalAIP");
        row.setColumn("handle_id", HandleManager.getID(context, dso.getHandle()));
        DatabaseManager.update(context, row);
        return new InternalAIP(context, row);
    }

    /**
     * Get iterator over every internal AIP in the table.
     * @return iterator that returns InternalAIPs
     */
    public static Iterator findAll(Context context)
        throws SQLException
    {
        class InternalAIPIterator
            implements Iterator
        {
            private TableRowIterator rows;
            private Context cachedContext;

            InternalAIPIterator(Context context, TableRowIterator rows)
            {
                cachedContext = context;
                this.rows = rows;
            }

            public boolean hasNext()
            {
                try
                {
                    return rows.hasNext();
                }
                catch (SQLException e)
                {
                    throw new RuntimeException("Got SQLException error in InternalAIP iterator: ", e);
                }
            }

            public Object next()
            {
                try
                {
                    if (rows.hasNext())
                    {
                        TableRow row = rows.next();

                        // Check cache
                        InternalAIP fromCache = (InternalAIP) cachedContext.fromCache(
                                InternalAIP.class, row.getIntColumn("internalaip_id"));
                        if (fromCache != null)
                            return fromCache;
                        else
                            return new InternalAIP(cachedContext, row);
                    }
                    else
                        return null;
                }
                catch (SQLException e)
                {
                    throw new RuntimeException("Got SQLException error in InternalAIP iterator: ", e);
                }
            }

            public void remove()
            {
            }
        }

        String myQuery = "SELECT * FROM InternalAIP;";
        TableRowIterator rows = DatabaseManager.queryTable(context, "InternalAIP", myQuery);

        return new InternalAIPIterator(context, rows);
    }

    /**
     * Update the entry:  if Internal AIP is out of date or missing,
     * record a new one.  NOT related to the DB-level update() method.
     * @return true if it needed updating.
     */
    public boolean updateAIP(Context context, boolean force)
        throws SQLException, IOException, PackageException,
               CrosswalkException, AuthorizeException
    {
        int bsID = aipRow.getIntColumn("aip_bitstream_id");
        Date lm = aipRow.getDateColumn("updated");
        DSpaceObject dso = getDSpaceObjectInternal();

        // if it's an Item that is already up-to-date, skip it.
        if (!force && bsID > -1 && dso.getType() == Constants.ITEM &&
            lm != null && !((Item)dso).getLastModified().after(lm))
            return false;

        // get ready to write the bitstream
        PackageDisseminator dip = (PackageDisseminator) PluginManager
                .getNamedPlugin(PackageDisseminator.class, PACKAGER_NAME);
        if (dip == null)
        {
            log.error("Error, Cannot find PackageDisseminator type: " +  PACKAGER_NAME);
            throw new PackageException("Cannot find PackageDisseminator type: "+  PACKAGER_NAME);
        }
        PackageParameters pkgParams = new PackageParameters();
        pkgParams.addProperty("internal", "true");

        // set up a Stream pipeline
        PipedOutputStream dest = new PipedOutputStream();
        aipInputStream = new PipedInputStream(dest);

        // put bitstream reader in subthread
        Thread reader = new Thread(this);
        reader.start();

        // write manifest with blocking I/O
        dip.disseminate(context, dso, pkgParams, dest);
        dest.close();
        try
        {
            reader.join();
        }
        catch (InterruptedException e)
        {
            throw new IOException("Failed copying AIP bitstream: "+e.toString());
        }

        if (aipBitstream == null)
            throw new IOException("Writing to AIP bitstream failed, see log.");

        // set format to XML in new bitstream; mostly cosmetic for
        // web-browser inspection with /retrieve servlet.
        BitstreamFormat xmlf = BitstreamFormat.findByShortDescription(context, "XML");
        if (xmlf != null)
        {
            aipBitstream.setFormat(xmlf);
            aipBitstream.update();
        }

        // save new AIP bitstream
        aipRow.setColumn("aip_bitstream_id", aipBitstream.getID());

        // get Created date from the AIP so date in DB will match it exactly
        // not all DSpaceObjects have a last-modified date, anyway.
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);
        aipRow.setColumn("updated", sip.getCreateDate(context, aipBitstream.retrieve(), probePkgParams));
        update();

        deleteMetadataBitstreams(context, true);

        String bsURIs[] = pkgParams.getProperties("additionalBitstreamURIs");
        if (bsURIs != null && bsURIs.length > 0)
        {
            for (String bu : bsURIs)
            {
                try
                {
                    Bitstream bs = Bitstream.dereferenceAbsoluteURI(context, new URI(bu));
                    if (bs == null)
                        System.err.println("Cannot find AIP's metadata bitstream for URI="+bu);
                    else
                        addMetadataBitstream(context, bs);
                }
                catch (URISyntaxException e)
                {
                    throw new PackageException(e);
                }
            }
        }

        // get rid of old bitstream after assigning new one.
        if (bsID > -1)
            Bitstream.find(context, bsID).delete();

        return true;
    }

    // for Runnable: pipe the AIP into a bitstream in separate thread.
    public void run()
    {
        try
        {
            aipBitstream = Bitstream.create(aipContext, aipInputStream);
        }
        catch (IOException e)
        {
            log.error("run(): Got IOException: ",e);
            aipBitstream = null;
        }
        catch (SQLException e)
        {
            log.error("run(): Got SQLException: ",e);
            aipBitstream = null;
        }
    }

    /**
     * Check whether the given bitstream is registered as an internal AIP, and
     * if not, register it as such if "force" is true.
     * ASSUME bitstream IS actually an AIP, i.e. it's passed the probe() test.
     * Beware of edge cases:
     *  - even if bitstream isn't registered, check if it is a more recent version
     *    of the AIP for a registered object. replace entry if force==true.
     *  - if it is registered, make sure its handle agrees with registry.
     *  - if unregistered and force==true, add an entry.
     * Also: if there is an entry that points to an AIP with an OLDER
     *  last-modified date than this one, replace it with the newer one.
     * @return the InternalAIP corresponding to the bitstream or null if none.
     */
    public static InternalAIP checkAIPBitstream(Context context,
                                                Bitstream bitstream,
                                                boolean force)
        throws PackageException, CrosswalkException, IOException,
               SQLException, AuthorizeException
    {
        // first find the internalAIP entry, if any, for this bitstream:
        TableRow row = DatabaseManager.querySingleTable(context,
            "InternalAIP",
            "SELECT * FROM InternalAIP WHERE aip_bitstream_id = ? ;",
            bitstream.getID());

        // get object Handle from the AIP
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);
        String aipHandle = sip.getHandle(context, bitstream.retrieve(), probePkgParams);

        // if AIP has no handle, don't try anything more, there's no point.
        if (aipHandle == null)
            log.warn("Got AIP with no Persistent Identifier, bitstream="+String.valueOf(bitstream.getID()));

        // otherwise look for other entry for its Handle.
        else
        {
            TableRow rowForHandle = null;
            int aipHandleID = HandleManager.getID(context, aipHandle);

            // sanity check: is the entry we found for the same Handle?
            if (row != null && aipHandleID != row.getIntColumn("handle_id"))
            {
                log.error("Handle in AIP does not match handle in InternalAIP record: bitstream="+String.valueOf(bitstream.getID())+", internalaip_id="+String.valueOf(row.getIntColumn("internalaip_id")));
                throw new PackageException("Handle in AIP does not match handle in InternalAIP record: bitstream="+String.valueOf(bitstream.getID())+", internalaip_id="+String.valueOf(row.getIntColumn("internalaip_id")));
            }

            // need to look for the entry by handle?  there may be multiple
            // internal AIP bitstreams for the same handle, so another
            // one might have created an entry.  We need to choose the latest.
            if (row == null)
            {
                rowForHandle = DatabaseManager.querySingleTable(context,
                    "InternalAIP",
                    "SELECT * FROM InternalAIP WHERE handle_id = ? ;", aipHandleID);
            }

            // force: reconcile this AIP and the internalAIP table.
            if (force)
            {
                Date aipCreateDate = sip.getCreateDate(context, bitstream.retrieve(), probePkgParams);

                // No entry or entry for AIP with this handle, so create one:
                if (row == null && rowForHandle == null)
                {
                    // make sure handle is in table
                    if (aipHandleID < 0)
                    {
                        HandleManager.createHandle(context, null, aipHandle);
                        aipHandleID = HandleManager.getID(context, aipHandle);
                    }
                    row = DatabaseManager.create(context, "InternalAIP");
                    row.setColumn("aip_bitstream_id", bitstream.getID());
                    row.setColumn("updated", aipCreateDate);
                    row.setColumn("handle_id", aipHandleID);
                    DatabaseManager.update(context, row);
                    InternalAIP newia = new InternalAIP(context, row);
                    for (Bitstream mbs : sip.getMetadataBitstreams(context, bitstream.retrieve(), probePkgParams))
                        newia.addMetadataBitstream(context, mbs);
                }

                // Is this AIP newer than existing entry for same handle?
                // Replace bitstream with newer one, also its MD streams.
                else if (rowForHandle != null &&
                    aipCreateDate.after(rowForHandle.getDateColumn("updated")))
                {
                    row = rowForHandle;
                    InternalAIP oldia = new InternalAIP(context, row);
                    oldia.deleteMetadataBitstreams(context, true);
                    row.setColumn("aip_bitstream_id", bitstream.getID());
                    row.setColumn("updated", aipCreateDate);
                    for (Bitstream mbs : sip.getMetadataBitstreams(context, bitstream.retrieve(), probePkgParams))
                        oldia.addMetadataBitstream(context, mbs);
                    DatabaseManager.update(context, row);
                }
            }

            // not force: warn if other AIP entry for Handle is out of date.
            else if (rowForHandle != null)
            {
                Date aipCreateDate = sip.getCreateDate(context, bitstream.retrieve(), probePkgParams);
                Date entryCreateDate = rowForHandle.getDateColumn("updated");
                if (aipCreateDate.after(entryCreateDate))
                {
                    log.warn("THIS AIP SHOULD SUPERCEDE THE ONE IN THE TABLE: No InternalAIP record found for bitstream="+String.valueOf(bitstream.getID())+
                     ", but another OLDER entry matches the Handle "+
                     HandleManager.find(context, aipHandleID)+
                     ", internalaip_id="+String.valueOf(rowForHandle.getIntColumn("internalaip_id")));
                    if (log.isDebugEnabled())
                        log.debug("AIP(bitstream="+String.valueOf(bitstream.getID())+
                            ") date="+Utils.formatISO8601Date(aipCreateDate)+
                            ", getTime="+String.valueOf(aipCreateDate.getTime())+
                            ", IS LATER THAN: InternalAIP entry ("+String.valueOf(rowForHandle.getIntColumn("internalaip_id"))+
                            ") date="+Utils.formatISO8601Date(entryCreateDate)+
                            ", getTime="+String.valueOf(entryCreateDate.getTime()));
                }
                else
                {
                    log.warn("IGNORE THIS AIP: No InternalAIP record found for bitstream="+String.valueOf(bitstream.getID())+
                     ", but another NEWER (or concurrent) entry (internalaip_id="+
                     String.valueOf(rowForHandle.getIntColumn("internalaip_id"))+
                     ") matches the Handle "+
                     HandleManager.find(context, aipHandleID)+
                     ", internalaip_id="+String.valueOf(rowForHandle.getIntColumn("internalaip_id")));
                    if (log.isDebugEnabled())
                        log.debug("AIP(bitstream="+String.valueOf(bitstream.getID())+
                            ") date="+Utils.formatISO8601Date(aipCreateDate)+
                            ", getTime="+String.valueOf(aipCreateDate.getTime())+
                            ", IS EARLIER/SAME THAN: InternalAIP entry ("+String.valueOf(rowForHandle.getIntColumn("internalaip_id"))+
                            ") date="+Utils.formatISO8601Date(entryCreateDate)+
                            ", getTime="+String.valueOf(entryCreateDate.getTime()));
                }
            }
        }

        // return something if we found or created a row.
        if (row != null)
        {
            if (log.isDebugEnabled())
                log.debug(LogManager.getHeader(context, "find_internalaip_by_aip_bitstream_id",
                                "internalaip_id="+String.valueOf(row.getIntColumn("internalaip_id"))));
            return new InternalAIP(context, row);
        }
        return null;
    }


    /**
     * Rebuild a DSpaceObject from an internal AIP bitstream.
     * If the DSO already exists, it is an error.
     *
     * @return the IngestionWrapper around the new object created by
     *    ingesting (restoring) the AIP.
     */
    public IngestionWrapper restoreFromAIP(Context context, DSpaceObject defaultParent)
        throws PackageException, CrosswalkException, IOException,
               SQLException, AuthorizeException
    {
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);

        IngestionWrapper iw = sip.ingest(context, defaultParent,
                                         getAIP().retrieve(), probePkgParams, null);

        if (iw.getType() == Constants.INGESTION_ITEM)
        {
            WorkspaceItem wi = (WorkspaceItem)iw;
            InstallItem.replaceItem(context, wi, wi.getHandle());

            // get new copy of item to reread from RDBMS:
            Item item = wi.getItem();
            if (wi.getWithdrawn())
            {
                if (log.isDebugEnabled())
                    log.debug("Marking item Withdrawn, item="+item.toString());
                item.withdraw();
            }
        }
        return iw;
    }

    // accessor - get object associated with this InternalAIP record.
    private DSpaceObject getDSpaceObjectInternal()
        throws SQLException
    {
        return HandleManager.resolveToObject(aipContext,
                                 HandleManager.find(aipContext, aipRow.getIntColumn("handle_id")));
    }

    /**
     * @return object associated with this InternalAIP record.
     */
    public DSpaceObject getDSpaceObject()
    {
        try
        {
            return getDSpaceObjectInternal();
        }
        catch (SQLException e)
        {
            log.error("Got SQLException: ",e);
            return null;
        }
    }

    /**
     * @return database index of this record.
     */
    public int getID()
    {
        return aipRow.getIntColumn("internalaip_id");
    }

    /**
     * @return bitstream holding actual internal AIP manifest (package).
     */
    public Bitstream getAIP()
    {
        try
        {
            return Bitstream.find(aipContext,
                                  aipRow.getIntColumn("aip_bitstream_id"));
        }
        catch (SQLException e)
        {
            log.error("Got SQLException: ",e);
            return null;
        }
    }

    /**
     * @return the timestamp on which the internal AIP was created
     *   (has nothing to do with when the target object was last modified)
     */
    public Date getLastModified()
    {
        return aipRow.getDateColumn("updated");
    }

    /**
     * @return the Handle corresponding to the target object, or null if it has none.
     */
    public String getHandle()
        throws SQLException
    {
        return HandleManager.find(aipContext, aipRow.getIntColumn("handle_id"));
    }

    /**
     * @return the Handle of the parent of the target object, as found in the AIP itself.
     */
    public String getParentHandle()
        throws PackageException, CrosswalkException, IOException, SQLException, AuthorizeException
    {
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);

        return sip.getParentHandle(aipContext, getAIP().retrieve(), probePkgParams);
    }

    /**
     * Synchronize the DB internalaip table with the contents of this object.
     */
    public void update()
        throws SQLException
    {
        DatabaseManager.update(aipContext, aipRow);
    }

    /**
     * @return true if bitstream houses an AIP internal package.
     */
    static public boolean probe(Context context, Bitstream bs)
        throws PackageException, IOException, SQLException, AuthorizeException
    {
        return probe(context, bs.retrieve());
    }

    /**
     *  Side effect, at least some of the stream is read.
     * @return true if the InputStream is an AIP internal package.
     */
    static public boolean probe(Context context, InputStream is)
        throws PackageException, IOException, SQLException, AuthorizeException
    {
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);

        return sip.probe(context, is, probePkgParams);
    }

    /**
     * This fetches the Handle from the AIP manifest itself; it may
     * not be the same as the InternalAIP's handle, though it should be.
     * @return the Handle of the object in the AIP manifest itself
     */
    public static String getHandleOfAIPBitstream(Context context,
                                                Bitstream bitstream)
        throws PackageException, CrosswalkException, IOException,
               SQLException, AuthorizeException
    {
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);
        return sip.getHandle(context, bitstream.retrieve(), probePkgParams);
    }

    /**
     * @return the "creation date" timestamp from the AIP manifest.
     */
    public static Date getCreateDateOfAIPBitstream(Context context,
                                                Bitstream bitstream)
        throws PackageException, CrosswalkException, IOException,
               SQLException, AuthorizeException
    {
        AIPIngester sip = (AIPIngester) PluginManager
                .getNamedPlugin(AIPIngester.class, PACKAGER_NAME);
        if (sip == null)
            throw new PackageException("Cannot find PackageIngester type: "+  PACKAGER_NAME);
        return sip.getCreateDate(context, bitstream.retrieve(), probePkgParams);
    }

    /** --------------------------------------------------------------------
     *   Methods to manage the list of "auxiliary" Metadata Bitstreams
     *
     * Some AIPs require extra Bitstreams to store metadata from the
     * RDBMS or another repository -- e.g. History data from an online
     * triplestore is written into a separate file, not part of the METS
     * "manifest" (i.e. the internal AIP).  This means the internal AIP
     * actually consists of multiple bitstreams.
     *
     * We have to keep track of these extra metadata Bitstreams so we can
     * delete them if they get superceded, since there is NO other record
     * or link to them (aside from references inconveniently buried in the
     * AIP).
     *
     * So, we keep track of these extra bitstreams (extras ONLY) in the
     * InternalAip2Bitstream table.  When an AIP is updated, we must
     * delete all the old metadata bitstreams first, then add the ones
     * from the new package.
     ** --------------------------------------------------------------------
     */

    // add an entry for this AIP
    private void addMetadataBitstream(Context context, Bitstream bitstream)
        throws IOException, SQLException, AuthorizeException
    {
        TableRow row = DatabaseManager.create(context, "InternalAIP2Bitstream");
        row.setColumn("internalaip_id", getID());
        row.setColumn("bitstream_id", bitstream.getID());
        DatabaseManager.update(context, row);
    }

    /**
     * @return all bitstreams related to (i.e. referenced by this
     *  internal AIP
     */
    public Bitstream[] getMetadataBitstreams(Context context)
        throws IOException, SQLException, AuthorizeException
    {
        TableRowIterator rows = DatabaseManager.queryTable(context,
          "InternalAIP2Bitstream",
          "SELECT bitstream_id FROM InternalAIP2Bitstream "+
            "WHERE internalaip_id = "+String.valueOf(getID()));
        List<Bitstream> result = new ArrayList<Bitstream>();
        while (rows.hasNext())
        {
            TableRow row = rows.next();
            Bitstream bs = Bitstream.find(context, row.getIntColumn("bitstream_id"));
            if (bs == null)
                log.error("Cannot find bitstream ID="+String.valueOf(row.getIntColumn("bitstream_id"))+" to delete.");
            else
                result.add(bs);
        }
        return result.toArray(new Bitstream[result.size()]);
    }

    // Delete bitstreams and all aux table entries for this AIP
    // NOTE: as a side-effect it deletes the bitstreams too!
    private void deleteMetadataBitstreams(Context context, boolean deleteBitstreams)
        throws IOException, SQLException, AuthorizeException
    {
        Bitstream[] toDelete = getMetadataBitstreams(context);

        // get rid of references from aip tables first:
        int del = DatabaseManager.deleteByValue(context,
                    "InternalAIP2Bitstream",
                    "internalaip_id",
                    String.valueOf(getID()));
        if (log.isDebugEnabled())
            log.debug("deleteMetadataBitstreams deleted "+String.valueOf(del)+" rows from internalaip_id.");

        if (deleteBitstreams)
            for (Bitstream db : toDelete)
                db.delete();
    }

    /**
     * Delete database presence and bitstreams.
     */
    public void delete()
        throws IOException, SQLException, AuthorizeException
    {
        deleteMetadataBitstreams(aipContext, true);
        Bitstream aipBs = getAIP();
        if (aipBs != null)
            aipBs.delete();
        DatabaseManager.delete(aipContext, "InternalAIP", getID());
    }
}
