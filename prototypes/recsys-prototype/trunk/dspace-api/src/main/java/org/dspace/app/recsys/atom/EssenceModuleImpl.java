/*
 * EssenceModuleImpl.java
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

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * <code>EssenceModuleImpl</code> implements the
 * <code>EssenceModule</code> interface. An <code>EssenceModule</code> has a
 * <code>key</code>, a <code>value</code>, a <code>URI</code>, and a
 * <code>type</code>. They <code>key</code> is the term describing this
 * <code>e</code>, the <code>value</code> is the frequency of
 * occurence of the <code>key</code>, the <code>URI</code> is the
 * <code>URI</code> of the <code>XML Namespace</code>, and the
 * <code>type</code> is the type of medatada, e.g. dc.subject.
 *
 * @author Desmond Elliott
 */
public class EssenceModuleImpl extends ModuleImpl implements EssenceModule
{
    private String key;
    private Integer value;
    private String uri;
    private String type;

    /**
     * Creates and returns a new <code>EssenceModule</code> with the
     * <code>EssenceModule</code> <code>XML Namespace</code>
     */
    public EssenceModuleImpl()
    {
        super(EssenceModule.class, EssenceModule.URI);
    }

    /** @inheritDoc */
    public String getKey()
    {
        return key;
    }

    /** @inheritDoc */
    public void setKey(String key)
    {
        this.key = key;
    }

    /** @inheritDoc */
    public Integer getValue()
    {
        return value;
    }

    /** @inheritDoc */
    public void setValue(Integer value)
    {
        this.value = value;
    }

    /** @inheritDoc */
    public String getURI()
    {
        return uri;
    }

    /** @inheritDoc */
    public void setURI(String uri)
    {
        this.uri = uri;
    }

    /** @inheritDoc */
    public String getType()
    {
        return type;
    }

    /** @inheritDoc */ 
    public void setType(String type)
    {
        this.type = type;
    }

    /** @inheritDoc */
    public Class getInterface()
    {
        return EssenceModule.class;
    }

    /** @inheritDoc */
    public void copyFrom(Object obj)
    {
        EssenceModule sm = (EssenceModule) obj;
        setKey(sm.getKey());
        setValue(sm.getValue());
    }

}