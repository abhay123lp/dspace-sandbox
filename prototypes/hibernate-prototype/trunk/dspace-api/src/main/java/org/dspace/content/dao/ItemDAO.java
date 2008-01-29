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

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.content.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;

/**
 * @author James Rutherford
 */
public abstract class ItemDAO extends ContentDAO
    implements CRUD<Item>, Link<Item, Bundle>
{
    protected Logger log = Logger.getLogger(ItemDAO.class);

    protected Context context;

    public ItemDAO(Context context)
    {
        this.context = context;

    }

//    public abstract Item create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
//    protected final Item create(Item item) throws AuthorizeException
//    {
//        log.info(LogManager.getHeader(context, "create_item",
//                    "item_id=" + item.getId()));
//
//        item.setLastModified(new Date());
//        update(item);
//
//        return item;
//    }
//
//    public Item retrieve(int id)
//    {
//        return (Item) context.fromCache(Item.class, id);
//    }

    public Item retrieve(UUID uuid)
    {
        return null;
    }

//    public void update(Item item) throws AuthorizeException
//    {
//        log.info("checkpoint 0");
//        // Check authorisation. We only do write authorization if user is
//        // not an editor
//        if (!item.canEdit())
//        {
//            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);
//        }
//
//        MetadataValueDAO mvDAO = MetadataValueDAOFactory.getInstance(context);
//        MetadataFieldDAO mfDAO = MetadataFieldDAOFactory.getInstance(context);
//        MetadataSchemaDAO msDAO = MetadataSchemaDAOFactory.getInstance(context);
//
//        log.info(LogManager.getHeader(context, "update_item", "item_id="
//                + item.getId()));
//
//        // Update the associated Bundles & Bitstreams
//        Bundle[] bundles = item.getBundles();
//
//        // Delete any Bundles that were removed from the in-memory list
//        for (Bundle dbBundle : bundleDAO.getBundles(item))
//        {
//            boolean deleted = true;
//            for (Bundle bundle : bundles)
//            {
//                if (bundle.equals(dbBundle))
//                {
//                    // If the bundle still exists in memory, don't delete
//                    deleted = false;
//                    break;
//                }
//            }
//
//            if (deleted)
//            {
//                unlink(item, dbBundle);
//            }
//        }
//
//        // Now that we've cleared up the db, we make the Item <-> Bundle
//        // link concrete.
//        for (Bundle bundle : bundles)
//        {
//            link(item, bundle);
//        }
//
//        // Set sequence IDs for bitstreams in item
//        int sequence = 0;
//
//        // find the highest current sequence number
//        for (Bundle bundle : bundles)
//        {
//            for (Bitstream bitstream : bundle.getBitstreams())
//            {
//                if (bitstream.getSequenceID() > sequence)
//                {
//                    sequence = bitstream.getSequenceID();
//                }
//            }
//        }
//
//        // start sequencing bitstreams without sequence IDs
//        sequence++;
//
//        for (Bundle bundle : bundles)
//        {
//            for (Bitstream bitstream : bundle.getBitstreams())
//            {
//                if (bitstream.getSequenceID() < 0)
//                {
//                    bitstream.setSequenceID(sequence);
//                    sequence++;
//                    bitstreamDAO.update(bitstream);
//                }
//            }
//
//            bundleDAO.update(bundle);
//        }
//
//        // Next we take care of the metadata
//
//        log.info("checkpoint 1");
//
//        // First, we figure out what's in memory, and what's in the database
//        List<MetadataValue> dbMetadata = mvDAO.getMetadataValues(item);
//        List<DCValue> memMetadata = item.getMetadata();
//
//        log.info("got " + dbMetadata.size() + " metadata values from the db");
//        log.info("got " + memMetadata.size() + " metadata values from memory");
//
//        // Now we have Lists of metadata values stored in-memory and in the
//        // database, we can go about saving changes.
//
//        // Step 1: remove any metadata that is no longer in memory (this
//        // includes values that may have changed, but since we allow
//        // multiple values for a given field for an object, we can't tell
//        // what's changed and what's just gone.
//
//        for (MetadataValue dbValue : dbMetadata)
//        {
//            boolean deleted = true;
//
//            for (DCValue memValue : memMetadata)
//            {
//                if (dbValue.equals(memValue))
//                {
//                    deleted = false;
//                }
//            }
//
//            if (deleted)
//            {
//                mvDAO.delete(dbValue.getID());
//            }
//        }
//
//        // Step 2: go through the list of in-memory metadata and save it to
//        // the database if it's not already there.
//
//        // Map counting number of values for each MetadataField
//        // Values are Integers indicating number of values written for a
//        // given MetadataField. Keys are MetadataField IDs.
//        Map<Integer, Integer> elementCount =
//                new HashMap<Integer, Integer>();
//
//        MetadataSchema schema;
//        MetadataField field;
//
//        for (DCValue memValue : memMetadata)
//        {
//            boolean exists = false;
//            MetadataValue storedValue = null;
//
//            schema = msDAO.retrieveByName(memValue.schema);
//
//            if (schema == null)
//            {
//                schema = msDAO.retrieve(MetadataSchema.DC_SCHEMA_ID);
//            }
//
//            field = mfDAO.retrieve(
//                    schema.getId(), memValue.element, memValue.qualifier);
//
//            // Work out the place number for ordering
//            int current = 0;
//
//            Integer key = field.getId();
//            Integer currentInteger = elementCount.get(key);
//
//            if (currentInteger != null)
//            {
//                current = currentInteger;
//            }
//
//            current++;
//            elementCount.put(key, current);
//
//            for (MetadataValue dbValue : dbMetadata)
//            {
//                if (dbValue.equals(memValue))
//                {
//                    // If it already exists, we make a note of the fact and
//                    // hold on to a copy of the object so we can update it
//                    // later.
//                    exists = true;
//                    storedValue = dbValue;
//                    break;
//                }
//            }
//
//            if (!exists)
//            {
//                MetadataValue value = mvDAO.create();
//                value.setFieldID(field.getId());
//                value.setItemID(item.getId());
//                value.setValue(memValue.value);
//                value.setLanguage(memValue.language);
//                value.setPlace(current);
//                mvDAO.update(value);
//            }
//            else
//            {
//                // Even if it already exists, the place may have changed.
//                storedValue.setPlace(current);
//                mvDAO.update(storedValue);
//            }
//        }
//
//        // Update browse indices
//        IndexBrowse ib = null;
//        try
//        {
//            ib = new IndexBrowse(context);
//            ib.indexItem(item);
//        }
//        catch (BrowseException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void delete(int id) throws AuthorizeException
//    {
//        Item item = retrieve(id);
//        update(item); // Sync in-memory object before removal
//
//        context.removeCached(item, id);
//
//        log.info(LogManager.getHeader(context, "delete_item", "item_id=" + id));
//
//        // Remove from indices, if appropriate
//        /** XXX FIXME
//         ** Although all other Browse index updates are managed through
//         ** Event consumers, removing an Item *must* be done *here* (inline)
//         ** because otherwise, tables are left in an inconsistent state
//         ** and the DB transaction will fail.
//         ** Any fix would involve too much work on Browse code that
//         ** is likely to be replaced soon anyway.   --lcs, Aug 2006
//         **
//         ** NB Do not check to see if the item is archived - withdrawn /
//         ** non-archived items may still be tracked in some browse tables
//         ** for administrative purposes, and these need to be removed.
//         **/
//        // Remove from Browse indices
//        try
//        {
//            IndexBrowse ib = new IndexBrowse(context);
//            ib.itemRemoved(item);
//            // FIXME: I think this is unnecessary because it is taken care
//            // of by the search consumer, but I'm not sure.
//            // DSIndexer.unIndexContent(context, item);
//        }
//        catch (BrowseException e)
//        {
//            throw new RuntimeException(e);
//        }
//
//        // Remove bundles
//        for (Bundle bundle : item.getBundles())
//        {
//            item.removeBundle(bundle);
//            unlink(item, bundle);
//        }
//
//        // remove all of our authorization policies
//        AuthorizeManager.removeAllPolicies(context, item);
//    }
//
//    public void decache(Item item)
//    {
//        // Remove item and it's submitter from cache
//        context.removeCached(item, item.getId());
//        EPerson submitter = item.getSubmitter();
//
//        // FIXME: I don't think we necessarily want to do this.
//        if (submitter != null)
//        {
//            context.removeCached(submitter, submitter.getId());
//        }
//
//        // Remove bundles & bitstreams from cache if they have been loaded
//        for (Bundle bundle : item.getBundles())
//        {
//            context.removeCached(bundle, bundle.getId());
//            for (Bitstream bitstream : bundle.getBitstreams())
//            {
//                context.removeCached(bitstream, bitstream.getId());
//            }
//        }
//    }
//
//    /**
//     * Returns a list of items that are both in the archive and not withdrawn.
//     */
      public abstract List<Item> getItems(EntityManager em);
      public abstract void removeFromCollections(EntityManager em, Item item);
//
//    /**
//     * This function primarily exists to service the Harvest class. See that
//     * class for documentation on usage.
//     */
//    public abstract List<Item> getItems(DSpaceObject scope,
//            String startDate, String endDate, int offset, int limit,
//            boolean items, boolean collections, boolean withdrawn)
//        throws ParseException;
//
//    /**
//     * This is a simple 'search' function that returns Items that are in the
//     * archive, are not withdrawn, and match the given schema, field, and
//     * value.
//     */
//    public List<Item> getItems(MetadataValue value)
//    {
//        return getItems(value, null, null);
//    }
//
//    /**
//     * The dates passed in here are used to limit the results by ingest date
//     * (dc.date.accessioned).
//     */
//    public abstract List<Item> getItems(MetadataValue value,
//            Date startDate, Date endDate);
//    
//    public abstract List<Item> getItemsByCollection(Collection collection);
//    public abstract List<Item> getItemsBySubmitter(EPerson eperson);
//
//    public abstract List<Item> getParentItems(Bundle bundle);
//
//    public void link(Item item, Bundle bundle) throws AuthorizeException
//    {
//        if (!linked(item, bundle))
//        {
//            AuthorizeManager.authorizeAction(context, item, Constants.ADD);
//
//            log.info(LogManager.getHeader(context, "add_bundle", "item_id="
//                    + item.getId() + ",bundle_id=" + bundle.getId()));
//
//            item.addBundle(bundle);
//        }
//    }
//
//    public void unlink(Item item, Bundle bundle) throws AuthorizeException
//    {
//        AuthorizeManager.authorizeAction(context, item, Constants.REMOVE);
//
//        item.removeBundle(bundle);
//
//        // If the bundle is now orphaned, delete it.
//        if (getParentItems(bundle).size() == 0)
//        {
//            // make the right to remove the bundle explicit because the
//            // implicit relation has been removed. This only has to concern the
//            // currentUser because he started the removal process and he will
//            // end it too. also add right to remove from the bundle to remove
//            // it's bitstreams.
//            AuthorizeManager.addPolicy(context, bundle, Constants.DELETE,
//                    context.getCurrentUser());
//            AuthorizeManager.addPolicy(context, bundle, Constants.REMOVE,
//                    context.getCurrentUser());
//
//            // The bundle is an orphan, delete it
//            bundleDAO.delete(bundle.getId());
//        }
//    }
//
//    public abstract boolean linked(Item item, Bundle bundle);
//
//    // Everything below this line needs to be re-thought
//
//    public abstract void loadMetadata(Item item);
//    public abstract List<DCValue> getMetadata(Item item, String schema,
//            String element, String qualifier, String lang);
}
