/*
 * HandleType.java
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
package org.dspace.uri.handle;

import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.ObjectIdentifier;

/**
 * Entity class for representing the identifier of type Handle.  This class
 * encapsulates all the general information about this identifier type, and
 * extends the ExternalIdentifierType class to make that available to the
 * rest of the identifier mechanisms.
 *
 * @author James Rutherford
 * @author Richard Jones
 */
public class HandleType extends ExternalIdentifierType
{
    /**
     * Construct a new instance of the HandleType class.  This contains
     * all the essential information which defines the Handle
     */
    public HandleType()
    {
        super("hdl", "http", "hdl.handle.net", "://", "/");
    }

    /**
     * Get an instance of the Handle associated with this type using the given value
     * and ObjectIdentifier.
     *
     * @param value
     * @param oid
     * @return
     */
    public Handle getInstance(String value, ObjectIdentifier oid)
    {
        return new Handle(value, oid);
    }

    /**
     * Is the given identifier type the same as the current instance.  This should not compare their
     * in-memory equalness, but their value equalness.  For example, in:
     *
     * <code>
     * HandleType ht1 = new HandleType();
     * HandleType ht2 = new HandleType();
     * boolean equal = ht1.equals(ht2);
     * </code>
     *
     * the value of "equal" should be "true".  This is equivalent to asking
     *
     * <code>type instanceof HandleType</code>
     *
     * @param type
     * @return
     */
    public boolean equals(ExternalIdentifierType type)
    {
        if (type instanceof HandleType)
        {
            return true;
        }
        return false;
    }
}
