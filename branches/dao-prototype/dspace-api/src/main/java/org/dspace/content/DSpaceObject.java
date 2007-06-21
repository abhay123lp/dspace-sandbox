/*
 * DSpaceObject.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

import org.dspace.core.Context;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

/**
 * Abstract base class for DSpace objects
 */
public abstract class DSpaceObject
{
    private static Logger log = Logger.getLogger(DSpaceObject.class);

    protected int id;
    protected ObjectIdentifier oid;
    protected List<PersistentIdentifier> identifiers;

    /**
     * Get the type of this object, found in Constants
     * 
     * @return type of the object
     */
    public abstract int getType();

    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    public int getID()
    {
        return id;
    }

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
    public PersistentIdentifier getPersistentIdentifier()
    {
        if ((identifiers != null) && (identifiers.size() > 0))
        {
            return identifiers.get(0);
        }
        else
        {
            log.warn("no persistent identifiers found. type=" + getType() +
                    ", id=" + getID());
            return null;
        }
    }

    public List<PersistentIdentifier> getPersistentIdentifiers()
    {
        if (identifiers == null)
        {
            identifiers = new ArrayList<PersistentIdentifier>();
        }

        return identifiers;
    }

    public void addPersistentIdentifier(PersistentIdentifier identifier)
    {
        this.identifiers.add(identifier);
    }

    public void setPersistentIdentifiers(List<PersistentIdentifier> identifiers)
    {
        this.identifiers = identifiers;
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

    public boolean equals(DSpaceObject other)
    {
        if (this.getType() == other.getType())
        {
            if (this.getID() == other.getID())
            {
                return true;
            }
        }

        return false;
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
