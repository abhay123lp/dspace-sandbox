/*
 * PolicyStackDisseminationCrosswalk
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

import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdf.RDFException;

/**
 * Write out the policy records relevant to a DSpace Object _and_
 * the statemetns related to all its ancestors (i.e. the "stack")
 * in the format (e.g. RDF/XML) chosen by the PolicyRepository.
 *
 * Note that this form of the policy content is more useful for
 * implementing the policies than for preserving them (i.e.
 * re-ingestion) -- see PolicyStreamDisseminationCrosswalk to get just
 * the policies related to the object itself for re-ingestion.
 * This code also assumes that policies are "inherited" from an object's
 * direct parent, and not from any other objects claiming it as a descendant.
 * If those rules change or require finer-grained customization this
 * implementation will have to be modified.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 * @see PolicyRepository
 */
public class PolicyStackStreamDisseminationCrosswalk
    implements StreamDisseminationCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(PolicyStackStreamDisseminationCrosswalk.class);

    /**
     * Predicate: Can this disseminator crosswalk the given object.
     * For Policy, we can only get Policy data for objects that
     * are first-class in the "archival", i.e. Item, Coll, Comm.
     * Bitstreams belong to Items.
     *
     * @param dso  dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        try
        {
            // XXX This is WRONG, need to compute the stack and
            // reutrn the OR of it..

            return PolicyRepository.getInstance().hasStatementsOfObject(context, dso);
        }
        catch (RDFException e)
        {
            log.error("Failed in hasStatementsOfObject: ",e);
            return false;
        }
    }

    /**
     * Writes out the policies associated with the whole "stack" of objects
     * related to the requested DSpaceObject, i.e. its parent and the
     * parent of its parent, up to the archive.
     *
     * @param dso the  DSpace Object whose metadata to export.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        try
        {
            List<DSpaceObject> stack = new ArrayList<DSpaceObject>();
            for (DSpaceObject cur = dso; cur != null; cur = getParent(context, cur))
                stack.add(cur);

            if (log.isDebugEnabled())
            {
                StringBuffer msg = new StringBuffer("Getting stack for dso=");
                msg.append(dso.toString()).append("; stack=(");
                for (DSpaceObject so : stack)
                    msg.append(so.toString()).append(", ");
                msg.append(")");
                log.debug(msg.toString());
            }

            // if target is an Item, get Policy of its descendants too.
            PolicyRepository rep = PolicyRepository.getInstance();
            rep.exportStatementsOfObjects(context, stack, out, PolicyRepository.policyFormat);
        }
        catch (RDFException e)
        {
            throw new CrosswalkInternalException(e);
        }
    }

    private static DSpaceObject getParent(Context context, DSpaceObject dso)
        throws SQLException
    {
        DSpaceObject result = null;

        switch (dso.getType())
        {
            case Constants.BITSTREAM:
                Bundle bn[] = ((Bitstream)dso).getBundles();
                if (bn.length > 0)
                {
                    Item i[] = bn[0].getItems();
                    if (i.length > 0)
                        result = i[0];
                }
                else
                    log.warn("Cannot find parent Item of bitstream: "+dso.toString());
                break;

            case Constants.ITEM:
                result = ((Item)dso).getOwningCollection();
                break;

            case Constants.COLLECTION:
                Community ps[] = ((Collection)dso).getCommunities();
                if (ps.length > 0)
                    result = ps[0];
                break;

            case Constants.COMMUNITY:
                Community p = ((Community)dso).getParentCommunity();
                if (p == null)
                    result = Site.find(context, 0);
                else
                    result = p;
                break;

            // NOTE: intentionally let Site drop through to return null.
        }
        return result;
    }

    public String getMIMEType()
    {
        return "text/xml";
    }
}
