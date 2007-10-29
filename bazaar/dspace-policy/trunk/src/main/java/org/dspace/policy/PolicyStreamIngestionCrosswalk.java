/*
 * PolicyIngestionCrosswalk
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/04/10 04:11:09 $
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

package org.dspace.policy;

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdf.RDFException;

/**
 * Ingest policy data from a stream of RDF/XML.
 *
 * NOTE: Be sure the stream being ingested was output by
 * PolicyStreamDisseminationCrosswalk, and NOT by
 * PolicyStackStreamDisseminationCrosswalk, since the latter may contain
 * policies related to an object's ancestors as well as its own.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 * @see PolicyStackStreamDisseminationCrosswalk
 */
public class PolicyStreamIngestionCrosswalk
    implements StreamIngestionCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(PolicyStreamIngestionCrosswalk.class);

    /**
     * Execute crosswalk, returning List of XML elements.
     * Returns a <code>List</code> of JDOM <code>Element</code> objects representing
     * the XML produced by the crosswalk.  This is typically called when
     * a list of fields is desired, e.g. for embedding in a METS document
     * <code>xmlData</code> field.
     * <p>
     * When there are no results, an
     * empty list is returned, but never <code>null</code>.
     *
     * @param dso the  DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void ingest(Context context, DSpaceObject dso, InputStream in, String MIMEType)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        try
        {
            PolicyRepository rep = PolicyRepository.getInstance();
            rep.ingestStatementsOfObject(context, dso, in, PolicyRepository.policyFormat);
            if (log.isDebugEnabled())
                log.debug("Ingested policy segment for: "+dso.toString());
        }
        catch (RDFException e)
        {
            throw new CrosswalkInternalException(e);
        }
    }

    public String getMIMEType()
    {
        return "text/xml";
    }
}
