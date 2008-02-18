/*
 * DSpaceObject.java
 *
 * Version: $Revision: 1721 $
 *
 * Date: $Date: 2008-01-24 16:57:38 +0100 (gio, 24 gen 2008) $
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.dspace.core.Context;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ObjectIdentifier;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

/**
 * Abstract base class for DSpace objects
 */
@MappedSuperclass
public abstract class DSpaceObject
{
    private static Logger log = Logger.getLogger(DSpaceObject.class);
    
    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    private StringBuffer eventDetails = null;

    protected Context context;
    protected int id;
    protected UUID uuid;
    protected ObjectIdentifier oid;
    protected List<ExternalIdentifier> externalIdentifiers;
    
    /**
     * Reset the cache of event details.
     */
    protected void clearDetails()
    {
        eventDetails = null;
    }

    /**
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     * @param detail detail string to add.
     */
    protected void addDetails(String d)
    {
        if (eventDetails == null)
        {
            eventDetails = new StringBuffer(d);
        }
        else
        {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * @returns summary of event details, or null if there are none.
     */
    @Transient
    protected String getDetails()
    {
        return (eventDetails == null ? null : eventDetails.toString());
    }

    /**
     * Get the type of this object, found in Constants
     * 
     * @return type of the object
     */
    @Transient
    public abstract int getType();

    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public int getId()
    {
        return id;
    }
    
    @Transient
    public int getID() {
        return getId();
    }
    
    @Transient
    public ObjectIdentifier getIdentifier()
    {
        return oid;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    /**
     * For those cases where you only want one, and you don't care what sort.
     */
    @Transient
    public ExternalIdentifier getExternalIdentifier()
    {
        if ((externalIdentifiers != null) && (externalIdentifiers.size() > 0))
        {
            return externalIdentifiers.get(0);
        }
        else
        {
            log.warn("no external identifiers found. type=" + getType() +
                    ", id=" + getId());
            return null;
        }
    }
    @Transient
    public List<ExternalIdentifier> getExternalIdentifiers()
    {
        if (externalIdentifiers == null)
        {
            externalIdentifiers = new ArrayList<ExternalIdentifier>();
        }

        return externalIdentifiers;
    }

    public void addExternalIdentifier(ExternalIdentifier identifier)
    {
        this.externalIdentifiers.add(identifier);
    }

    public void setExternalIdentifiers(List<ExternalIdentifier> identifiers)
    {
        this.externalIdentifiers = identifiers;
    }

    /**
     * Get a proper name for the object. This may return <code>null</code>.
     * Name should be suitable for display in a user interface.
     *
     * @return Name for the object, or <code>null</code> if it doesn't have
     *         one
     */
    @Transient
    public abstract String getName();

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }
/*
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }
*/
    public boolean equals(DSpaceObject other)
    {
        if (this.getType() == other.getType())
        {
            if (this.getId() == other.getId())
            {
                return true;
            }
        }

        return false;
    }
/*
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
*/
	public void setId(int id) {
		this.id = id;
	}
}
