/*
 * HarvestResults.java
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
package org.dspace.app.federate.harvester;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Class containing the identifiers and metadata of objects harvested via
 * OAI-PMH, and any resumption token found.
 * 
 * @author Weihua Huang
 * @author Robert Tansley
 */
public class HarvestResults
{
    /** The metadata files */
    private List metadataFiles;

    /** The corresponding OAI identifiers */
    private List oaiIdentifiers;

    /** The resumption token, if any */
    private String resumptionToken;

    /**
     * Construct a new HarvestResults object with no results and no resumption
     * token.
     */
    HarvestResults()
    {
        metadataFiles = new LinkedList();
        oaiIdentifiers = new LinkedList();
        resumptionToken = null;
    }

    /**
     * Retrieve the file containing the metadata for object at the given index
     * 
     * @param index
     *            index of the object to get metadata for
     * @return the File containing the XML metadata in the local file system
     */
    public File getMetadataFile(int index)
    {
        return (File) metadataFiles.get(index);
    }

    /**
     * Retrieve the OAI identifier of the object at the given index
     * 
     * @param index
     *            index of the object to get OAI identifier for
     * @return the OAI identifier
     */
    public String getIdentifier(int index)
    {
        return (String) oaiIdentifiers.get(index);
    }

    /**
     * Get the number of results
     * 
     * @return the number of retrieved objects
     */
    public int getResultCount()
    {
        return metadataFiles.size();
    }

    /**
     * Returns any resumption token given by the remote repository.
     * 
     * @return The resumption token, or <code>null</code>
     */
    public String getResumptionToken()
    {
        return resumptionToken;
    }

    /**
     * Indicates whether a resumption token was given by the remote repository,
     * indicating that there are further results.
     * 
     * @return <code>true</code> if the remote repository returned a
     *         resumption token, <code>false</code> otherwise
     */
    public boolean hasResumptionToken()
    {
        if (resumptionToken != null)
        {
            return true;
        }
        return false;
    }

    /**
     * Set resumptionToken
     * 
     * @param resumptionToken
     */
    void setResumptionToken(String resumptionToken)
    {
        this.resumptionToken = resumptionToken;
    }

    /**
     * Add the given OAI identifier and corresponding metadata file to the
     * results
     * 
     * @param id
     *            the OAI identifier
     * @param metadata
     *            corresponding metadata XML file
     */
    void addResult(String id, File metadata)
    {
        metadataFiles.add(metadata);
        oaiIdentifiers.add(id);
    }
}
