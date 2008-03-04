/*
 * ItemProxy.java
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
package org.dspace.content.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

import org.apache.log4j.Logger;

/**
 * FIXME: This class could be optimized a great deal by being clever about
 * exactly what metadata (and maybe bundles) are pulled into memory.
 *
 * @author James Rutherford
 */
public class ItemProxy extends Item
{
    private static Logger log = Logger.getLogger(ItemProxy.class);

    private boolean bundlesLoaded = false;
    private boolean metadataLoaded = false;

    public ItemProxy(Context context, int id)
    {
        super(context, id);
    }

    @Override
    public Collection getOwningCollection()
    {
        if (owningCollection != null)
        {
            return owningCollection;
        }
        else if (owningCollectionId != -1)
        {
            owningCollection = collectionDAO.retrieve(owningCollectionId);
            return owningCollection;
        }
        else
        {
            log.warn("No owning collection information available!");
            return null;
        }
    }

    @Override
    public Bundle[] getBundles()
    {
        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundlesByItem(this));
        }

        return (Bundle[]) bundles.toArray(new Bundle[0]);
    }

    @Override
    public void setBundles(List<Bundle> bundles)
    {
        this.bundles = bundles;
        this.bundlesLoaded = true;
    }

    @Override
    public Bundle[] getBundles(String name)
    {
        List<Bundle> matchingBundles = new ArrayList<Bundle>();

        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        for (Bundle b : bundles)
        {
            if (name.equals(b.getName()))
            {
                matchingBundles.add(b);
            }
        }

        return (Bundle[]) matchingBundles.toArray(new Bundle[0]);
    }

    @Override
    public void addBundle(Bundle b) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        if (bundles == null)
        {
            bundles = new ArrayList<Bundle>();
        }

        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        for (Bundle bundle : bundles)
        {
            if (b.getID() == bundle.getID())
            {
                return;
            }
        }

        bundles.add(b);
    }

    @Override
    public void removeBundle(Bundle b) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.REMOVE);

        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        Iterator<Bundle> i = bundles.iterator();
        while (i.hasNext())
        {
            Bundle bundle = i.next();
            if (bundle.getID() == b.getID())
            {
                i.remove();
            }
        }
    }

    @Override
    public DCValue[] getMetadata(String schema, String element,
            String qualifier, String language)
    {
        // Really, we should query the DAO for specific metadata only under
        // certain conditions, but if the value doesn't exist in memory, we
        // can't ever guarantee that it's not in the database unless we
        // actually check.
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        List<DCValue> md = new ArrayList<DCValue>();
        for (DCValue dcv : metadata)
        {
            if (match(schema, element, qualifier, language, dcv))
            {
                md.add(dcv);
            }
        }

        return (DCValue[]) md.toArray(new DCValue[0]);
    }

    @Override
    public void setMetadata(List<DCValue> metadata)
    {
        this.metadata = metadata;
    }

    @Override
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String... values)
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;
            if (values[i] != null)
            {
                // remove control unicode char
                String temp = values[i].trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++)
                {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                        !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                        !String.valueOf(dcvalue[charPos]).equals("\n") &&
                        !String.valueOf(dcvalue[charPos]).equals("\r"))
                    {
                        dcvalue[charPos] = ' ';
                    }
                }
                dcv.value = String.valueOf(dcvalue);

                if (!metadata.contains(dcv))
                {
                    metadata.add(dcv);
                    metadataChanged = true;
                }
            }
            else
            {
                dcv.value = null;
            }
        }
    }

    @Override
    public void clearMetadata(String schema, String element, String qualifier,
            String lang)
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        if (metadata.size() == 0)
        {
            return;
        }

        Iterator<DCValue> i = metadata.iterator();
        while (i.hasNext())
        {
            if (match(schema, element, qualifier, lang, i.next()))
            {
                i.remove();
                metadataChanged = true;
            }
        }
    }

    @Override
    public void setSubmitter(EPerson submitter)
    {
        this.submitterId = submitter.getID();
        this.submitter = null;
    }

    @Override
    public void setSubmitter(int submitterId)
    {
        this.submitterId = submitterId;
        submitter = null;
    }

    @Override
    public EPerson getSubmitter()
    {
        if (submitter == null && submitterId > -1)
        {
            submitter = epersonDAO.retrieve(submitterId);
        }
        return submitter;
    }
}
