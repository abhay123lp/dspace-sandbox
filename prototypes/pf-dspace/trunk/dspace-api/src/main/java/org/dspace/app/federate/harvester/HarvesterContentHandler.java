/*
 * HarvesterContentHandler.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2005/11/15 21:44:39 $
 *
 * Copyright (c) 2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.federate.harvester;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dspace.core.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <P>
 * A SAX content handler that quickly splices an OAI-PMH response to ListRecords
 * or GetRecord into separate files. The result is a {@link HarvestResults}
 * object, which contains the OAI identifiers and the temporary files on the
 * filesystem containing the metadata. The temporary files are scheduled to be
 * deleted when the JVM exits.
 * </P>
 * 
 * <P>
 * This is reusable but not thread-safe.
 * </P>
 * 
 * <P>
 * The handler has three states. When an OAI identifier is reached,
 * <code>STATE_IDENTIFIER</code> is set, which causes the handler to start
 * reading the OAI identifier in. The next opening OAI &lt;metadata&gt; element
 * then causes a temporary file to be opened and the metadata within it to be
 * written to that file (<code>STATE_METADATA</code>).
 * </P>
 * 
 * <P>
 * TODO: Possibly not robust to malformed responses (the harvester parser is
 * deliberately non-validating for speed)
 * </P>
 * 
 * @author Robert Tansley
 * @author WeiHua Huang
 * @author James Rutherford
 */
public class HarvesterContentHandler extends DefaultHandler
{
    /** State when we're not in an identifier element or metadata element */
    private final int STATE_NOMINAL = 0;

    /**
     * Inside identifier element -- expecting character data that's the OAI
     * identifier of the current record
     */
    private final int STATE_IDENTIFIER = 1;

    /**
     * Inside metadata element -- all elements, attributes etc. to be written to
     * external file
     */
    private final int STATE_METADATA = 2;

    /** Expecting a resumption token */
    private final int STATE_RESUMPTION_TOKEN = 3;

    /** URI of OAI-PMH native XML elements */
    private final String OAI_URI = "http://www.openarchives.org/OAI/2.0/";

    /** Current state of handler */
    private int state;

    /** Temp directory to write metadata files to */
    private File tempDir;

    /** Current metadata XML file that metadata record is being written to */
    private File currentTempFile;

    /** Output stream to current XML metadata file */
    private OutputStream currentOutput;

    /** OAI identifier of current record */
    private StringBuffer currentIdentifier;
    
    /** Resumption token */
    private StringBuffer resumptionToken;

    /** Namespace prefices - key = prefix, value = namespace URI */
    private Map namespaces;

    /**
     * Prefix mappings
     * 
     * /** Harvest results object
     */
    private HarvestResults harvestResults;

    /**
     * Create a new handler
     * 
     * @param results
     *            the results object to fill with harvested metadata and
     *            identifiers
     * @param temp
     *            temporary directory to write metadata files to
     */
    public HarvesterContentHandler(HarvestResults results, String temp)
    {
        harvestResults = results;
        tempDir = new File(temp);
    }

    public void startDocument() throws SAXException
    {
        state = STATE_NOMINAL;
        currentTempFile = null;
        currentOutput = null;
        currentIdentifier = new StringBuffer();
        resumptionToken = new StringBuffer();
        namespaces = new HashMap();
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
    {
        switch (state)
        {
        case STATE_NOMINAL:

            if (uri.equals(OAI_URI))
            {
                if (localName.equals("identifier"))
                {
                    state = STATE_IDENTIFIER;
                    break;
                }
                else if (localName.equals("metadata"))
                {
                    try
                    {
                        // New metadata temp file
                        currentTempFile = File.createTempFile("metadata.",
                                null, tempDir);
                        // Schedule temp file for deletion when JVM exits
                        currentTempFile.deleteOnExit();
                        currentOutput = new BufferedOutputStream(
                                new FileOutputStream(currentTempFile));
                        // Write XML header, so temp file is a valid XML
                        // document in its own right
                        currentOutput
                                .write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n"
                                        .getBytes("UTF-8"));
                        state = STATE_METADATA;
                    }
                    catch (IOException e)
                    {
                        throw new SAXException(e);
                    }
                }
                else if (localName.equals("resumptionToken"))
                {
                    state = STATE_RESUMPTION_TOKEN;
                    break;
                }
            }

            // Otherwise ignore
            break;

        case STATE_IDENTIFIER:
            throw new SAXException(
                    "Illegal state: element found inside <identifier>");

        case STATE_METADATA:
            // Just pipe to output
            try
            {
                StringBuffer element = new StringBuffer("<");
                element.append(qName);

                /*
                 * If new namespaces have been encountered, need to add these to
                 * this element
                 */
                if (namespaces.size() > 0)
                {
                    addNamespaces(element);
                }

                for (int i = 0; i < attributes.getLength(); i++)
                {
                    element.append(" ");
                    element.append(attributes.getQName(i));
                    element.append("=\"");
                    String value = Utils.addEntities(attributes.getValue(i));
                    value.replaceAll("\"", "&quot;");
                    element.append(value);
                    element.append("\"");
                }

                element.append(">");
                currentOutput.write(element.toString().getBytes("UTF-8"));

            }
            catch (IOException e)
            {
                throw new SAXException(e);
            }
            break;

        case STATE_RESUMPTION_TOKEN:
            throw new SAXException(
                    "Illegal state: element found inside <resumptionToken>");
        }

    }

    public void characters(char[] ch, int start, int length)
            throws SAXException
    {
        switch (state)
        {
        case STATE_IDENTIFIER:
            currentIdentifier.append(new String(ch, start, length));
            break;

        case STATE_METADATA:
            String currentData = new String(ch, start, length);
            try
            {
                currentOutput.write(escapeXML(currentData).getBytes("UTF-8"));
            }
            catch (IOException e)
            {
                throw new SAXException(e);
            }
            break;

        case STATE_RESUMPTION_TOKEN:
            resumptionToken.append(new String(ch, start, length));
            break;
        }
        // Ignore if not in IDENTIFIER or METADATA
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        switch (state)
        {
        case STATE_IDENTIFIER:
            if (uri.equals(OAI_URI) && localName.equals("identifier"))
            {
                state = STATE_NOMINAL;
                break;
            }
            else
            {
                throw new SAXException(
                        "Illegal state: closing element that isn't </identifier> found in STATE_IDENTIFIER");
            }

        case STATE_METADATA:
            try
            {
                if (uri.equals(OAI_URI) && localName.equals("metadata"))
                {
                    // Finished reading metadata for this record. Add it to
                    // results.
                    currentOutput.flush();
                    currentOutput.close();

                    harvestResults
                            .addResult(currentIdentifier.toString(), currentTempFile);

                    // Just to ensure that any malformed OAI-PMH responses don't
                    // overwrite data
                    currentTempFile = null;
                    currentOutput = null;
                    currentIdentifier = new StringBuffer();

                    state = STATE_NOMINAL;
                }
                else
                {
                    StringBuffer closingTag = new StringBuffer("</").append(
                            qName).append(">");
                    currentOutput
                            .write(closingTag.toString().getBytes("UTF-8"));
                }
            }
            catch (IOException e)
            {
                throw new SAXException(e);
            }
            break;

        case STATE_RESUMPTION_TOKEN:
            if (uri.equals(OAI_URI) && localName.equals("resumptionToken"))
            {
                harvestResults.setResumptionToken(resumptionToken.toString());
                state = STATE_NOMINAL;
                break;
            }
            else
            {
                throw new SAXException(
                        "Illegal state: closing element that isn't </resumptionToken> found in STATE_RESUMPTION TOKEN");
            }
        }

        // ignore everything else
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        // Don't need to add OAI namespace
        if (!uri.equals(OAI_URI))
        {
            namespaces.put(prefix, uri);
        }
    }

    /**
     * Utility method to add any namespaces found to the outgoing XML element
     * declaration. After these namespaces are added, the namespace map is
     * cleared so they won't be re-added later.
     * 
     * @param element
     *            element declaration under construction, just after element
     *            itself has been added (i.e. contents should be:
     *            <code>&lt;element</code>)
     */
    private void addNamespaces(StringBuffer element)
    {
        String defaultNamespace = (String) namespaces.get("");
        if (defaultNamespace != null)
        {
            element.append(" xmlns=\"").append(defaultNamespace).append("\"");
        }

        Iterator i = namespaces.keySet().iterator();
        while (i.hasNext())
        {
            String prefix = (String) i.next();
            if (!prefix.equals(""))
            {
                element.append(" xmlns:").append(prefix).append("=\"").append(
                        namespaces.get(prefix)).append("\"");
            }
        }

        namespaces.clear();
    }

	private String escapeXML(String xml)
	{
		xml = xml.replaceAll("&", "&amp;");
		xml = xml.replaceAll(">", "&gt;");
		xml = xml.replaceAll("<", "&lt;");

		return xml;
	}
}
