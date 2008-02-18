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

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.dao.CRUD;
import org.dspace.dao.Link;

/**
 * @author James Rutherford
 */
public abstract class BundleDAO extends ContentDAO
    implements CRUD<Bundle>, Link<Bundle, Bitstream>
{
    protected Logger log = Logger.getLogger(BundleDAO.class);

    protected Context context;
    protected BitstreamDAO bitstreamDAO;

    public BundleDAO(Context context)
    {
        this.context = context;

        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
    }
    
    public abstract Bundle findBundleByName(Item item, String name, EntityManager em);
    
//    public abstract Bundle create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
//    protected final Bundle create(Bundle bundle) throws AuthorizeException
//    {
//        log.info(LogManager.getHeader(context, "create_bundle", "bundle_id="
//                + bundle.getId()));
//
//        return bundle;
//    }
//
//    public Bundle retrieve(int id)
//    {
//        return (Bundle) context.fromCache(Bundle.class, id);
//    }
//
//    public Bundle retrieve(UUID uuid)
//    {
//        return null;
//    }
//
//    // FIXME: Check authorization? Or do we count on this only ever happening
//    // via an Item?
//    public void update(Bundle bundle) throws AuthorizeException
//    {
//        Bitstream[] bitstreams = bundle.getBitstreams();
//
//        // Delete any Bitstreams that were removed from the in-memory list
//        for (Bitstream dbBitstream : bitstreamDAO.getBitstreamsByBundle(bundle))
//        {
//            boolean deleted = true;
//
//            for (Bitstream bitstream : bitstreams)
//            {
//                if (bitstream.getId() == dbBitstream.getId())
//                {
//                    // If the bitstream still exists in memory, don't delete
//                    deleted = false;
//                    break;
//                }
//            }
//
//            if (deleted)
//            {
//                removeBitstreamFromBundle(bundle, dbBitstream);
//            }
//        }
//
//        // Now that we've cleared up the db, we make the Bundle <->
//        // Bitstream link concrete.
//        for (Bitstream bitstream : bitstreams)
//        {
//            link(bundle, bitstream);
//        }
//    }
//
//    public void delete(int id) throws AuthorizeException
//    {
//        Bundle bundle = retrieve(id);
//        update(bundle); // Sync in-memory object before removal
//
//        context.removeCached(bundle, id);
//
//        log.info(LogManager.getHeader(context, "delete_bundle", "bundle_id="
//                    + id));
//
//        for (Bitstream bitstream : bundle.getBitstreams())
//        {
//            removeBitstreamFromBundle(bundle, bitstream);
//        }
//
//        // remove our authorization policies
//        AuthorizeManager.removeAllPolicies(context, bundle);
//    }

//    public abstract List<Bundle> getBundles(Item item);
//    public abstract List<Bundle> getBundles(Bitstream bitstream);

//    private void removeBitstreamFromBundle(Bundle bundle,
//            Bitstream bitstream) throws AuthorizeException
//    {
//        unlink(bundle, bitstream);
//
//        // In the event that the bitstream to remove is actually
//        // the primary bitstream, be sure to unset the primary
//        // bitstream.
//        if (bitstream.getId() == bundle.getPrimaryBitstreamID())
//        {
//            bundle.unsetPrimaryBitstreamID();
//        }
//
//        if (getBundles(bitstream).size() == 0)
//        {
//            // The bitstream is an orphan, delete it
//            bitstreamDAO.delete(bitstream.getId());
//        }
//    }
//
    public void link(Bundle bundle, Bitstream bitstream) throws AuthorizeException
    {
        if (!linked(bundle, bitstream))
        {
            AuthorizeManager.authorizeAction(context, bundle, Constants.ADD);

            log.info(LogManager.getHeader(context, "add_bitstream",
                        "bundle_id=" + bundle.getId() +
                        ",bitstream_id=" + bitstream.getId()));

            //bundle.addBitstream(bitstream);
            
        }
    }

    public void unlink(Bundle bundle, Bitstream bitstream) throws AuthorizeException
    {
        if (linked(bundle, bitstream))
        {
            AuthorizeManager.authorizeAction(context, bundle,
                    Constants.REMOVE);

            log.info(LogManager.getHeader(context, "remove_bitstream",
                        "bundle_id=" + bundle.getId() +
                        ",bitstream_id=" + bitstream.getId()));

            //bundle.removeBitstream(bitstream);
        }
    }

    public boolean linked(Bundle bundle, Bitstream bitstream)
    {
        for (Bitstream b : bundle.getBitstreams())
        {
            if (b.equals(bitstream))
            {
                return true;
            }
        }

        return false;
    }
}
