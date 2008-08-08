/*
 * EssenceModuleParser.java
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
import com.sun.syndication.io.ModuleParser;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * <code>EssenceModuleParser</code> parses the <code>essence</code> element
 * from an Atom entry and creates a returns a <code>EssenceModule</code> which
 * represents it.
 *
 * @author Desmond Elliott
 */
public class EssenceModuleParser implements ModuleParser
{
    /** The <code>XML Namespace</code> of the <code>EssenceModule</code> */
    private static final Namespace SAMPLE_NS =
            Namespace.getNamespace("essence", EssenceModule.URI);

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
     * Returns a <code>Module</code> element after parsing the
     * <code>JDOM element</code> passed as the <code>dcRoot</code> argument.
     *
     * @param dcRoot the <code>JDOM element</code> to parse
     * @return an <code>EssenceModule</code>
     */
    public Module parse(Element dcRoot)
    {
        boolean foundSomething = false;
        EssenceModule fm = new EssenceModuleImpl();

        Element e = dcRoot.getChild("key", SAMPLE_NS);
        if (e != null)
        {
            foundSomething = true;
            fm.setKey(e.getText());
        }

        e = dcRoot.getChild("value", SAMPLE_NS);
        if (e != null)
        {
            foundSomething = true;
            fm.setValue(new Integer(e.getText()));
        }

        e = dcRoot.getChild("uri", SAMPLE_NS);
        if (e != null)
        {
            foundSomething = true;
            fm.setURI(e.getText());
        }

        e = dcRoot.getChild("type", SAMPLE_NS);
        if (e != null)
        {
            foundSomething = true;
            fm.setType(e.getText());
        }
        
        return (foundSomething) ? fm : null;
    }
}
