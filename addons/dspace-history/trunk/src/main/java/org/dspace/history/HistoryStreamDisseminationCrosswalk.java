/*
 * HistoryStreamDisseminationCrosswalk
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

package org.dspace.history;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdf.RDFException;

/**
 * Write out the history records relevant to a DSpace Object in the format (e.g.
 * RDF/XML) chosen by the HistoryRepository.
 * 
 * @author Larry Stone
 * @version $Revision: 1.0 $
 */
public class HistoryStreamDisseminationCrosswalk implements
        StreamDisseminationCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(HistoryStreamDisseminationCrosswalk.class);

    /**
     * Predicate: Can this disseminator crosswalk the given object. For History,
     * we can only get history data for objects that are first-class in the
     * "archival", i.e. Item, Coll, Comm. Bitstreams belong to Items. Then check
     * if it has any history.
     * 
     * @param dso
     *            dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        try
        {
            int type = dso.getType();
            if (type == Constants.ITEM || type == Constants.COLLECTION
                    || type == Constants.COMMUNITY)
                return HistoryRepository.getInstance().hasStatementsOfObject(
                        context, dso);
            else
                return false;
        }
        catch (RDFException e)
        {
            log.error("Failed checking for statements: ", e);
            return false;
        }
    }

    /**
     * Writes all statements that were stored as "belonging to" the given DSpace
     * Object.
     * 
     * @param dso
     *            the DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     * 
     * @throws CrosswalkInternalException (
     *             <code>CrosswalkException</code>) failure of the crosswalk
     *             itself.
     * @throws CrosswalkObjectNotSupported (
     *             <code>CrosswalkException</code>) Cannot crosswalk this
     *             kind of DSpace object.
     * @throws IOException
     *             I/O failure in services this calls
     * @throws SQLException
     *             Database failure in services this calls
     * @throws AuthorizeException
     *             current user not authorized for this operation.
     */
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {
        // bitstream is not "archival object" in history.. let the Item
        // carry its history records so it also gets the intervening
        // Bundles and can thus establish the relationship.
        if (dso.getType() == Constants.BITSTREAM)
            return;

        try
        {
            // if target is an Item, get history of its descendants too.
            HistoryRepository rep = HistoryRepository.getInstance();
            rep.exportStatementsOfObject(context, dso, out,
                    HistoryRepository.HistoryFormat);
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
