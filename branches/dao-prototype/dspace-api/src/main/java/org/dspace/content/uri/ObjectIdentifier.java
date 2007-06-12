/*
 * ObjectIdentifier.java
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
package org.dspace.content.uri;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * @author James Rutherford
 */
public class ObjectIdentifier
{
    private static Logger log = Logger.getLogger(ObjectIdentifier.class);

    private int resourceID;
    private int resourceTypeID;
    private UUID uuid;

    public ObjectIdentifier(int resourceID, int resourceTypeID)
    {
        this.resourceID = resourceID;
        this.resourceTypeID = resourceTypeID;
    }

    public ObjectIdentifier(UUID uuid)
    {
        this.uuid = uuid;
    }

    public DSpaceObject getObject(Context context)
    {
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);
        CollectionDAO collectionDAO =
            CollectionDAOFactory.getInstance(context);
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        BundleDAO bundleDAO = BundleDAOFactory.getInstance(context);

        if (uuid == null)
        {
            switch(resourceTypeID)
            {
                case (Constants.BITSTREAM):
                    try
                    {
                        return Bitstream.find(context, resourceID);
                    }
                    catch (SQLException sqle)
                    {
                        throw new RuntimeException(sqle);
                    }
                case (Constants.BUNDLE):
                    return bundleDAO.retrieve(resourceID);
                case (Constants.ITEM):
                    return itemDAO.retrieve(resourceID);
                case (Constants.COLLECTION):
                    return collectionDAO.retrieve(resourceID);
                case (Constants.COMMUNITY):
                    return communityDAO.retrieve(resourceID);
                default:
                    throw new RuntimeException("Not a valid DSpaceObject type");
            }
        }
        else
        {
            DSpaceObject dso = null;

            try
            {
                dso = (Bitstream) Bitstream.find(context, uuid);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }

            if (dso == null)
            {
                dso = (Bundle) bundleDAO.retrieve(uuid);
            }
            if (dso == null)
            {
                dso = (Item) itemDAO.retrieve(uuid);
            }
            if (dso == null)
            {
                dso = (Collection) collectionDAO.retrieve(uuid);
            }
            if (dso == null)
            {
                dso = (Community) communityDAO.retrieve(uuid);
            }

            if (dso == null)
            {
                throw new RuntimeException("Couldn't find " + uuid);
            }
            else
            {
                return dso;
            }
        }
    }

    public URL getURL()
    {
        boolean openURL = false;

        String base = ConfigurationManager.getProperty("dspace.url") + "/";
        String value = (openURL ? "id=info:dspace/" : "info:dspace/");
        
        if (uuid == null)
        {
            value += "dsi/" + resourceTypeID + "/" + resourceID;
        }
        else
        {
            value += "uuid/" + uuid.toString();
        }

        try
        {
            return new URL(base + (openURL ? "openurl?" : "resource/") +
                    URLEncoder.encode(value, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new RuntimeException(uee);
        }
        catch (MalformedURLException murle)
        {
            throw new RuntimeException(murle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
