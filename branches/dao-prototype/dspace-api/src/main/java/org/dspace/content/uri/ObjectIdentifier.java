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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * @author James Rutherford
 */
public class ObjectIdentifier
{
    private static Logger log = Logger.getLogger(ObjectIdentifier.class);

    protected Context context;
    protected String value;
    protected int resourceID;
    protected int resourceTypeID;

    public ObjectIdentifier(Context context, int resourceID, int resourceTypeID)
    {
        this.context = context;
        this.resourceID = resourceID;
        this.resourceTypeID = resourceTypeID;
    }

    public ObjectIdentifier(Context context, DSpaceObject dso)
    {
        this(context, dso.getID(), dso.getType());
    }

    public ObjectIdentifier(Context context, PersistentIdentifier pid)
    {
        this(context, pid.getResourceID(), pid.getResourceTypeID());
    }

    // FIXME: URI or URL? I suppose this will always give a locator, so it
    // should be URL.
    public URL getURL()
    {
        String base = ConfigurationManager.getProperty("dspace.url");
        String value = resourceTypeID + "/" + resourceID;

        try
        {
            return new URL(base + "/resource/dsi/" + value);
        }
        catch (MalformedURLException murle)
        {
            throw new RuntimeException(murle);
        }
    }

    public DSpaceObject getObject()
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
                return BundleDAOFactory.getInstance(context).retrieve(resourceID);
            case (Constants.ITEM):
                return ItemDAOFactory.getInstance(context).retrieve(resourceID);
            case (Constants.COLLECTION):
                return CollectionDAOFactory.getInstance(context).retrieve(resourceID);
            case (Constants.COMMUNITY):
                return CommunityDAOFactory.getInstance(context).retrieve(resourceID);
            default:
                throw new RuntimeException("Not a valid DSpaceObject type");
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
