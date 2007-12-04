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
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

public abstract class ItemDAO extends ContentDAO<ItemDAO>
        implements CRUD<Item>, Link<Item, Bundle>
{
    protected Logger log = Logger.getLogger(ItemDAO.class);

    protected Context context;
    protected BundleDAO bundleDAO;
    protected BitstreamDAO bitstreamDAO;
    protected ExternalIdentifierDAO identifierDAO;

    public ItemDAO()
    {
    }

    public ItemDAO(Context context)
    {
        this.context = context;

        bundleDAO = BundleDAOFactory.getInstance(context);
        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
    }

    public abstract ItemDAO getChild();

    public abstract void setChild(ItemDAO childDAO);

    public abstract Item create() throws AuthorizeException;

    public abstract Item retrieve(int id);

    public abstract Item retrieve(UUID uuid);

    public abstract void update(Item item) throws AuthorizeException;

    public abstract void delete(int id) throws AuthorizeException;

    public abstract void decache(Item item);

    /**
     * Returns a list of items that are both in the archive and not withdrawn.
     */
    public abstract List<Item> getItems();

    /**
     * This function primarily exists to service the Harvest class. See that
     * class for documentation on usage.
     */
    public abstract List<Item> getItems(DSpaceObject scope,
                                        String startDate, String endDate,
                                        int offset, int limit,
                                        boolean items, boolean collections,
                                        boolean withdrawn)
            throws ParseException;

    /**
     * This is a simple 'search' function that returns Items that are in the
     * archive, are not withdrawn, and match the given schema, field, and
     * value.
     */
    public abstract List<Item> getItems(MetadataValue value);

    /**
     * The dates passed in here are used to limit the results by ingest date
     * (dc.date.accessioned).
     */
    public abstract List<Item> getItems(MetadataValue value,
                                        Date startDate, Date endDate);

    public abstract List<Item> getItemsByCollection(Collection collection);

    public abstract List<Item> getItemsBySubmitter(EPerson eperson);

    public abstract List<Item> getParentItems(Bundle bundle);

    public abstract void link(Item item, Bundle bundle)
            throws AuthorizeException;

    public abstract void unlink(Item item, Bundle bundle)
            throws AuthorizeException;

    public abstract boolean linked(Item item, Bundle bundle);

    public abstract void loadMetadata(Item item);

    public abstract List<DCValue> getMetadata(Item item,
                                              String schema, String element,
                                              String qualifier, String lang);
}
