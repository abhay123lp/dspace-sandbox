/*
 * ItemDAO.java
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
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.search.DSIndexer;

/**
 * @author James Rutherford
 */
public abstract class ItemDAO extends ContentDAO
{
    protected Logger log = Logger.getLogger(ItemDAO.class);

    protected Context context;
    protected BundleDAO bundleDAO;
    protected BitstreamDAO bitstreamDAO;

    public abstract Item create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final Item create(Item item) throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "create_item",
                    "item_id=" + item.getID()));

        item.setLastModified(new Date());
        update(item);

        return item;
    }

    public Item retrieve(int id)
    {
        // Check the cache
        return (Item) context.fromCache(Item.class, id);
    }

    public Item retrieve(UUID uuid)
    {
        return null;
    }

    public void update(Item item) throws AuthorizeException
    {
        // Check authorisation. We only do write authorization if user is
        // not an editor
        if (!item.canEdit())
        {
            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);
        }

        log.info(LogManager.getHeader(context, "update_item", "item_id="
                + item.getID()));

        // Update the associated Bundles & Bitstreams
        Bundle[] bundles = item.getBundles();

        // Delete any Bundles that were removed from the in-memory list
        for (Bundle dbBundle : bundleDAO.getBundles(item))
        {
            boolean deleted = true;
            for (Bundle bundle : bundles)
            {
                if (bundle.equals(dbBundle))
                {
                    // If the bundle still exists in memory, don't delete
                    deleted = false;
                    break;
                }
            }

            if (deleted)
            {
                removeBundleFromItem(item, dbBundle);
            }
        }

        // Now that we've cleared up the db, we make the Item <-> Bundle
        // link concrete.
        for (Bundle bundle : bundles)
        {
            link(item, bundle);
        }

        // Set sequence IDs for bitstreams in item
        int sequence = 0;

        // find the highest current sequence number
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bitstreams[k].getSequenceID() > sequence)
                {
                    sequence = bitstreams[k].getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;

        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bitstreams[k].getSequenceID() < 0)
                {
                    bitstreams[k].setSequenceID(sequence);
                    sequence++;
                    bitstreamDAO.update(bitstreams[k]);
                }
            }

            bundleDAO.update(bundles[i]);
        }
    }

    public void delete(int id) throws AuthorizeException
    {
        Item item = retrieve(id);
        update(item); // Sync in-memory object before removal

        context.removeCached(item, id);

        log.info(LogManager.getHeader(context, "delete_item", "item_id=" + id));

        // Remove from indices, if appropriate
        if (item.isArchived())
        {
            // Remove from Browse indices
            try
            {
                IndexBrowse ib = new IndexBrowse(context);
                ib.itemRemoved(item);
                DSIndexer.unIndexContent(context, item);
            }
            catch (IOException ioe)
            {
                throw new RuntimeException(ioe);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
            catch (BrowseException e)
            {
                throw new RuntimeException(e);
            }
        }

        // Remove bundles
        for (Bundle bundle : item.getBundles())
        {
            item.removeBundle(bundle);
            removeBundleFromItem(item, bundle);
        }

        // remove all of our authorization policies
        AuthorizeManager.removeAllPolicies(context, item);
    }

    public void decache(Item item)
    {
        // Remove item and it's submitter from cache
        context.removeCached(item, item.getID());
        EPerson submitter = item.getSubmitter();

        // FIXME: I don't think we necessarily want to do this.
        if (submitter != null)
        {
            context.removeCached(submitter, submitter.getID());
        }

        // Remove bundles & bitstreams from cache if they have been loaded
        for (Bundle bundle : item.getBundles())
        {
            context.removeCached(bundle, bundle.getID());
            for (Bitstream bitstream : bundle.getBitstreams())
            {
                context.removeCached(bitstream, bitstream.getID());
            }
        }
    }

    public abstract List<Item> getItems();
    public abstract List<Item> getItemsByCollection(Collection collection);
    public abstract List<Item> getItemsBySubmitter(EPerson eperson);

    public abstract List<Item> getParentItems(Bundle bundle);

    // FIXME: This is so similar to the usage of ArchiveManager.move() that it
    // would be pretty silly not to use it. The only issue with that is that we
    // don't generally want to expose via the API the ability to move Bundles
    // between Items (or do we? I doubt it).
    protected final void removeBundleFromItem(Item item, Bundle bundle)
        throws AuthorizeException
    {
        unlink(item, bundle);

        // If the bundle is now orphaned, delete it.
        if (getParentItems(bundle).size() == 0)
        {
            // make the right to remove the bundle explicit because the
            // implicit relation has been removed. This only has to concern the
            // currentUser because he started the removal process and he will
            // end it too. also add right to remove from the bundle to remove
            // it's bitstreams.
            AuthorizeManager.addPolicy(context, bundle, Constants.DELETE,
                    context.getCurrentUser());
            AuthorizeManager.addPolicy(context, bundle, Constants.REMOVE,
                    context.getCurrentUser());

            // The bundle is an orphan, delete it
            bundleDAO.delete(bundle.getID());
        }
    }

    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        // FIXME: Pre-DAOs this wasn't checked.
        AuthorizeManager.authorizeAction(context, item, Constants.ADD);
    }

    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, item, Constants.REMOVE);
    }

    // Everything below this line needs to be re-thought

    public abstract void loadMetadata(Item item);
    public abstract List<DCValue> getMetadata(Item item, String schema,
            String element, String qualifier, String lang);
}
