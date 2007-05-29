/*
 * BundleDAOPostgres.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class BundleDAOPostgres extends ContentDAO implements BundleDAO
{
    private Logger log = Logger.getLogger(BundleDAOPostgres.class);
    private Context context;

    public BundleDAOPostgres(Context context)
    {
        if (context != null)
        {
            this.context = context;
        }
    }

    public Bundle create() throws AuthorizeException
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "bundle");
            int id = row.getIntColumn("bundle_id");
            Bundle bundle = new Bundle(context, id);

            update(bundle, row);

            log.info(LogManager.getHeader(context, "create_bundle", "bundle_id="
                    + id));

            return bundle;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Bundle retrieve(int id)
    {
        // First check the cache
        Bundle fromCache = (Bundle) context.fromCache(Bundle.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "bundle", id);

            if (row == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(LogManager.getHeader(context, "find_bundle",
                            "not_found,bundle_id=" + id));
                }

                return null;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(LogManager.getHeader(context, "find_bundle",
                            "bundle_id=" + id));
                }

                Bundle bundle = new Bundle(context, id);
                populateBundleFromTableRow(bundle, row);

                context.cache(bundle, id);

                return bundle;
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void update(Bundle bundle) throws AuthorizeException
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "bundle", bundle.getID());

            if (row == null)
            {
                row = DatabaseManager.create(context, "bundle");
                int id = row.getIntColumn("bundle_id");
                bundle.setID(id);

                log.warn("Didn't find bundle with id " + bundle.getID() +
                        " so I made one with id " + id);

                log.info(LogManager.getHeader(context, "create_bundle",
                        "bundle_id=" + id));
            }

            update(bundle, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Bundle bundle, TableRow bundleRow)
        throws AuthorizeException
    {
        try
        {
            Bitstream[] bitstreams = bundle.getBitstreams();

            // Delete any Bitstreams that were removed from the in-memory list
            for (Bitstream dbBitstream : getBitstreams(bundle))
            {
                boolean deleted = true;

                for (Bitstream bitstream : bitstreams)
                {
                    if (bitstream.getID() == dbBitstream.getID())
                    {
                        // If the bitstream still exists in memory, don't delete
                        deleted = false;
                        break;
                    }
                }

                if (deleted)
                {
                    removeBitstreamFromBundle(bundle, dbBitstream);
                }
            }

            // Now that we've cleared up the db, we make the Bundle <->
            // Bitstream link concrete.
            for (Bitstream bitstream : bitstreams)
            {
                link(bundle, bitstream);
            }

            bundleRow.setColumn("name", bundle.getName());

            if (bundle.getPrimaryBitstreamID() > 0)
            {
                bundleRow.setColumn("primary_bitstream_id",
                        bundle.getPrimaryBitstreamID());
            }
            else
            {
                bundleRow.setColumnNull("primary_bitstream_id");
            }

            DatabaseManager.update(context, bundleRow);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void delete(int id) throws AuthorizeException
    {
        try
        {
            Bundle bundle = retrieve(id);
            this.update(bundle); // Sync in-memory object with db before removal

            log.info(LogManager.getHeader(context, "delete_bundle", "bundle_id="
                    + id));

            context.removeCached(bundle, id);

            for (Bitstream bitstream : bundle.getBitstreams())
            {
                removeBitstreamFromBundle(bundle, bitstream);
            }

            // remove our authorization policies
            AuthorizeManager.removeAllPolicies(context, bundle);

            DatabaseManager.delete(context, "bundle", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    public List<Bundle> getBundles(Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bundle_id FROM item2bundle " +
                    "WHERE item_id = " + item.getID());

            List<Bundle> bundles = new ArrayList<Bundle>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("bundle_id");
                bundles.add(retrieve(id));
            }

            return bundles;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateBundleFromTableRow(Bundle bundle, TableRow bundleRow)
    {
        try
        {
            // Get bitstreams
            TableRowIterator tri = DatabaseManager.queryTable(
                    context, "bitstream", "SELECT b.* " +
                    "FROM bitstream b, bundle2bitstream b2b " +
                    "WHERE b2b.bitstream_id = b.bitstream_id " +
                    "AND b2b.bundle_id= ? ",
                    bundle.getID());

            List <Bitstream> bitstreams = new ArrayList<Bitstream>();

            for (TableRow row : tri.toList())
            {
                // FIXME: I'd like to do BitstreamDAO.retrieve(id);
                Bitstream fromCache = (Bitstream) context.fromCache(
                        Bitstream.class, row.getIntColumn("bitstream_id"));

                if (fromCache != null)
                {
                    bitstreams.add(fromCache);
                }
                else
                {
                    bitstreams.add(new Bitstream(context, row));
                }
            }

            bundle.setID(bundleRow.getIntColumn("bundle_id"));
            bundle.setName(bundleRow.getStringColumn("name"));
            bundle.setBitstreams(bitstreams);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private List<Bitstream> getBitstreams(Bundle bundle)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context, 
                    "SELECT bitstream_id FROM bundle2bitstream " +
                    " WHERE bundle_id = " + bundle.getID());

            List<Bitstream> bitstreams = new ArrayList<Bitstream>();

            // FIXME: This is slightly inconsistent with the other DAOs
            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("bitstream_id");
                bitstreams.add(Bitstream.find(context, id));
            }

            return bitstreams;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void link(Bundle bundle, Bitstream bitstream)
        throws AuthorizeException
    {
        if (!linked(bundle, bitstream))
        {
            try
            {
                AuthorizeManager.authorizeAction(context, bundle, Constants.ADD);

                TableRow row = DatabaseManager.create(context,
                        "bundle2bitstream");
                row.setColumn("bundle_id", bundle.getID());
                row.setColumn("bitstream_id", bitstream.getID());
                DatabaseManager.update(context, row);

                AuthorizeManager.inheritPolicies(context, bundle, bitstream);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    private void unlink(Bundle bundle, Bitstream bitstream)
        throws AuthorizeException
    {
        if (linked(bundle, bitstream))
        {
            try
            {
                // Check authorisation
                AuthorizeManager.authorizeAction(context, bundle,
                        Constants.REMOVE);

                log.info(LogManager.getHeader(context, "remove_bitstream",
                            "bundle_id=" + bundle.getID() +
                            ",bitstream_id=" + bitstream.getID()));

                // Delete the mapping row
                DatabaseManager.updateQuery(context,
                        "DELETE FROM bundle2bitstream " +
                        "WHERE bundle_id = ? AND bitstream_id = ? ",
                        bundle.getID(), bitstream.getID());
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    private boolean linked(Bundle bundle, Bitstream bitstream)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT id FROM bundle2bitstream " +
                    "WHERE bundle_id = ? " + 
                    "AND bitstream_id = ? ",
                    bundle.getID(), bitstream.getID());

            return tri.hasNext();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void removeBitstreamFromBundle(Bundle bundle,
            Bitstream bitstream) throws AuthorizeException
    {
        try
        {
            unlink(bundle, bitstream);

            // In the event that the bitstream to remove is actually
            // the primary bitstream, be sure to unset the primary
            // bitstream.
            if (bitstream.getID() == bundle.getPrimaryBitstreamID())
            {
                bundle.unsetPrimaryBitstreamID();
            }

            // FIXME: This is slightly inconsistent with the other DAOs
            if (bitstream.getBundles().length == 0)
            {
                // The bitstream is an orphan, delete it
                bitstream.delete();
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
