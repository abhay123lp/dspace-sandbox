/*
 * BundleDAO.java
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
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;

/**
 * @author James Rutherford
 */
public abstract class BundleDAO extends ContentDAO
{
    protected Logger log = Logger.getLogger(BundleDAO.class);

    protected Context context;
    public abstract Bundle create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    public Bundle create(int id) throws AuthorizeException
    {
        Bundle bundle = new Bundle(context, id);

        log.info(LogManager.getHeader(context, "create_bundle", "bundle_id="
                + bundle.getID()));

        update(bundle);

        return bundle;
    }

    public Bundle retrieve(int id)
    {
        return (Bundle) context.fromCache(Bundle.class, id);
    }

    // FIXME: Check authorization? Or do we count on this only ever happening
    // via an Item?
    public void update(Bundle bundle) throws AuthorizeException
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
    }

    public void delete(int id) throws AuthorizeException
    {
        Bundle bundle = retrieve(id);
        this.update(bundle); // Sync in-memory object with db before removal

        // Remove from cache
        context.removeCached(bundle, id);

        log.info(LogManager.getHeader(context, "delete_bundle", "bundle_id="
                    + id));

        for (Bitstream bitstream : bundle.getBitstreams())
        {
            removeBitstreamFromBundle(bundle, bitstream);
        }

        // remove our authorization policies
        AuthorizeManager.removeAllPolicies(context, bundle);
    }

    public abstract List<Bundle> getBundles(Item item);

    // FIXME: This should really be in BitstreamDAO, but that hasn't been
    // implemented yet.
    public abstract List<Bitstream> getBitstreams(Bundle bundle);

    // FIXME: I'm not sure if i want this exposed, but we need it accessible in
    // here and in subclasses.
    protected void removeBitstreamFromBundle(Bundle bundle,
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

    public void link(Bundle bundle, Bitstream bitstream) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, bundle, Constants.ADD);
        try
        {
            AuthorizeManager.inheritPolicies(context, bundle, bitstream);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void unlink(Bundle bundle, Bitstream bitstream) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, bundle,
                Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_bitstream",
                    "bundle_id=" + bundle.getID() +
                    ",bitstream_id=" + bitstream.getID()));
    }
}
