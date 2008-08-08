/*
 * EssenceModuleGenerator.java
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
import com.sun.syndication.io.ModuleGenerator;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * <code>EssenceModuleGenerator</code> generates the <code>XML</code> to
 * represent the <code>essence</code> element.
 *
 * @author Desmond Elliott
 */
public class EssenceModuleGenerator implements ModuleGenerator
{
    /** The <code>XML Namespace</code> of the <code>EssenceModule</code> */
    private static final Namespace SAMPLE_NS =
            Namespace.getNamespace("essence", EssenceModule.URI);
    
    /** Set of <code>Namespace</code> objects */
    private static final Set NAMESPACES;

    /**
     * A static set of <code>Namespace</code> objects is created to support the
     * <code>getNameSpaces()</code> method more efficiently.
     */
    static
    {
        Set nss = new HashSet();
        nss.add(SAMPLE_NS);
        NAMESPACES = Collections.unmodifiableSet(nss);
    }

    /**
     * Gets the <code>XML namespace URI</code> of the <code>EssenceModule</code>
     *
     * @return the <code>XML namespace URI</code> of the
     *         <code>EssenceModule</code>
     */
    public String getNamespaceUri()
    {
        return EssenceModule.URI;
    }

    /**
     * Gets the Set of <code>Namespaces</code>
     *
     * @return the Set of <code>Namespaces</code>
     */
    public Set getNamespaces()
    {
        return NAMESPACES;
    }

    /**
     * Adds a new <code>EssenceModule element</code> to the
     * <code>JDOM element</code> based on the <code>module</code> argument.
     *
     * @param module the <code>EssenceModule element</code> to add to the
     *        <code>JDOM element</code>
     * @param element the <code>JDOM element</code> which the
     *        <code>EssenceModule element</code> will be added to
     */
    public void generate(Module module, Element element)
    {
        EssenceModule em = (EssenceModule)module;

        if (em.getKey() != null)
        {
            element.addContent(generateSimpleElement("key", em.getKey()));
        }

        if (em.getValue() != null)
        {
            element.addContent(generateSimpleElement("value",
                                                     em.getValue().toString()));
        }

        if (em.getURI() != null)
        {
            element.addContent(generateSimpleElement("uri", em.getURI()));
        }

        if (em.getType() != null)
        {
            element.addContent(generateSimpleElement("type", em.getType()));
        }
    }

    /**
     * Creates and returns a new <code>Element</code> with a given
     * <code>name</code> and a given <code>value</code> in the
     * <code>XML namespace</code> of the <code>EssenceModule</code>.
     *
     * @param name the <code>name</code> of the content you are adding. Should
     *             be either <code>key</code> or <code>value</code> or
     *             <code>uri</code> or <code>type</code>.
     * @param value the <code>content</code> to add to the <code>Element</code>
     * @return a new <code>Element</code> with a given <code>name</code> and a
     *         given <code>value</code> in the <code>XML namespace</code> of the
     *         <code>EssenceModule</code>.
     */
    protected Element generateSimpleElement(String name, String value)
    {
        Element element = new Element(name, SAMPLE_NS);
        element.addContent(value);

        return element;
    }
}