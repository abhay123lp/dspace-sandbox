/*
 * ExternalIdentifier.java
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

import java.net.URISyntaxException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * @author James Rutherford
 */
public class ExternalIdentifier
{
    private static Logger log = Logger.getLogger(ExternalIdentifier.class);

    protected Context context;
    protected String value;
    protected ObjectIdentifier oid;

    private final String PROTOCOL;
    private final String NS;
    private final String BASE_URI;
    private final Type TYPE;

    public ExternalIdentifier()
    {
        this(ExternalIdentifier.Type.NULL);
    }

    public ExternalIdentifier(ExternalIdentifier.Type type)
    {
        this.TYPE = type;
        this.PROTOCOL = type.getProtocol();
        this.NS = type.getNamespace();
        this.BASE_URI = type.getBaseURI();
    }

    public ExternalIdentifier(Context context, DSpaceObject dso,
            ExternalIdentifier.Type type, String value)
    {
        this(type);
        this.context = context;
        this.value = value;
        this.oid = dso.getIdentifier();
    }

    public ObjectIdentifier getObjectIdentifier()
    {
        return oid;
    }

    public int getTypeID()
    {
        return TYPE.getID();
    }

    public Type getType()
    {
        return TYPE;
    }

    public URI getURI()
    {
        try
        {
            // eg: http + :// + hdl.handle.net + / + 1234/56
            return new URI(PROTOCOL + "://" + BASE_URI + "/" + value);
        }
        catch (URISyntaxException urise)
        {
            throw new RuntimeException(urise);
        }
    }

    public String getCanonicalForm()
    {
        // eg: hdl:1234/56
        return NS + ":" + value;
    }

    @Deprecated
    public String getValue()
    {
        return value;
    }

    /**
     * We can't say anything about the default behaviour here. This won't
     * always be true for getObject(), but for now, it's easiest to leave the
     * implementation in the Handle subclass.
     */
    public Map<String, List<String>> getMetadata()
    {
        return null;
    }

    public List<String> getMetadata(String field)
    {
        return null;
    }

    /**
     * This enum holds the required information about all supported types of
     * persistent identifier. Once the persistent identifier mechanism has
     * settled down, this information should be stored in a database registry
     * so that it can be administered via the UI.
     */
    public enum Type
    {
        // FIXME: Including a "type id" is a bit naughty, but it enables the
        // getID() method which is far better than ordinal(). Also, this
        // information will eventually live in the database (probably), and
        // when that happens, ordinal() will become useless.
        NULL (0, "", "dsi", ""),    // Internal type
        HANDLE (1, "http", "hdl", "hdl.handle.net"),
        UUID (2, "", "uuid", "");   // Internal type

        private final int id;
        private final String protocol;
        private final String namespace;
        private final String baseURI;

        private Type(int id, String protocol, String namespace, String baseURI)
        {
            this.id = id;
            this.protocol = protocol;
            this.namespace = namespace;
            this.baseURI = baseURI;
        }

        public int getID() { return id; }
        public String getProtocol() { return protocol; }
        public String getNamespace() { return namespace; }
        public String getBaseURI() { return baseURI; }
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
