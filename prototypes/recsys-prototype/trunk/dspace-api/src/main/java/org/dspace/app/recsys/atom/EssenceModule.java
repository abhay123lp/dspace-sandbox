/*
 * EssenceModule.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.recsys.atom;

import com.sun.syndication.feed.module.Module;

/**
 * <code>EssenceModule</code> defines the getters and setters for the
 * properties of the <code>essence</code> XML element.
 *
 * @author Desmond Elliott
 */
public interface EssenceModule extends Module
{
    /** Defines the XML namespace of this module */
    public static final String URI =
            "http://www.dspace.org/recsys/essenceModule/1.0";

    /**
     * Gets the <code>key</code> of this <code>essence</code> element
     *
     * @return the <code>key</code> of this <code>essence</code> element
     */
    public String getKey();

    /**
     * Sets the <code>key</code> for this <code>essence</code> element
     *
     * @param key the <code>key</code> for this <code>essence</code> element
     */
    public void setKey(String key);

    /**
     * Gets the <code>value</code> of this <code>essence</code> element
     *
     * @return the <code>value</code> of this <code>essence</code> element
     */
    public Integer getValue();

    /**
     * Sets the <code>value</code> for this <code>essence</code> element
     *
     * @param value the <code>value</code> for this <code>essence</code> element
     */
    public void setValue(Integer value);

    /**
     * Gets the XML namespace <code>URI</code> of this
     * <code>essence</code> element
     *
     * @return the <code>URI</code> of this <code>essence</code> element
     */
    public String getURI();

    /**
     * Sets the <code>XML Namespace</code> for this <code>essence</code> element
     *
     * @param uri the <code>URI</code> for this <code>essence</code> element
     */
    public void setURI(String uri);

    /**
     * Gets the <code>type</code> of this <code>essence</code> element
     *
     * @return the <code>type</code> of this <code>essence</code> element
     */
    public String getType();

    /**
     * Sets the <code>type</code> of this <code>essence</code> element
     *
     * @param type the <code>type</code> of this <code>essence</code> element
     */
    public void setType(String type);
}