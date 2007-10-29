/*
 * HistoryRepository.java
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

import java.io.File;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdf.RDFException;
import org.dspace.storage.rdf.RDFRepository;
import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Implements an RDF triple-store for History statements. Built on
 * RDFRepository, see that class for details.
 * 
 * Configuration: history.dir = pathname of a directory dedicated to history
 * triplestore.
 * 
 * @author Larry Stone
 * @author Mark Diggory
 * @version $Revision: 1.0 $
 * @see RDFRepository
 */
public class HistoryRepository extends RDFRepository
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(HistoryRepository.class);

    /** format of choice for disseminating and ingesting History data */
    public static final RDFRepository.FormatOfRDF HistoryFormat = RDFRepository.FormatOfRDF.RDFXML;

    /** well-known namespace URI for RDF */
    public static final String RDF_NS_URI = RDF.NAMESPACE;

    /** well-known namespace URI for RDFS */
    public static final String RDFS_NS_URI = RDFS.NAMESPACE;

    /** well-known namespace URI for XMLS */
    public static final String XMLS_NS_URI = XMLSchema.NAMESPACE;

    /** ABC Harmony namespace */
    public static final String HARMONY_NS_URI = "http://metadata.net/harmony#";

    /** Unqualified Dublin Core namespace */
    public static final String DC_NS_URI = "http://purl.org/dc/elements/1.1/";

    /** DSpace History namespace (TEMPORARY PLACEHOLDER) */
    public static final String DSPACE_HISTORY_NS_URI = "http://www.dspace.org/history#";

    /** DSpace Object Model namespace (TEMPORARY PLACEHOLDER) */
    public static final String DSPACE_OBJECT_NS_URI = "http://www.dspace.org/objectModel#";

    // XXX MUST keep this up to date when adding namespaces above:
    private static final Namespace namespaces[] = {
            new NamespaceImpl("abc", HARMONY_NS_URI),
            new NamespaceImpl("rdfs", RDFS_NS_URI),
            new NamespaceImpl("rdf", RDF_NS_URI),
            new NamespaceImpl("xmls", XMLS_NS_URI),
            new NamespaceImpl("history", DSPACE_HISTORY_NS_URI),
            new NamespaceImpl("dso", DSPACE_OBJECT_NS_URI),
            new NamespaceImpl("dc", DC_NS_URI) };

    // one shared RDF repository object - instances use Connections.
    private static HistoryRepository theInstance = null;

    private HistoryRepository(File dir)
    {
        super(dir);
    }

    /**
     * Get an instance of the repository object, which will always be the single
     * cached instance since it can be shared and re-used. NEVER returns null,
     * throws exception upon any (unlikely) failure.
     * 
     * Assumes a default of dspace.dir/history in the event that history.dir is
     * undefined in properties.
     * 
     * @return repository instance, never null.
     */
    static public HistoryRepository getInstance()
    {
        if (theInstance == null)
        {
            try
            {
                String history_dir = ConfigurationManager
                        .getProperty("history.dir");
                String dspace_dir = ConfigurationManager
                        .getProperty("dspace.dir");

                if (history_dir == null)
                    history_dir = new File(dspace_dir, "history")
                            .getAbsolutePath();

                theInstance = new HistoryRepository(new File(history_dir));
                theInstance.addNamespaces(namespaces);
            }
            catch (RDFException e)
            {
                log.error("Failed to create History Repository: ", e);
            }
        }
        return theInstance;
    }

    /**
     * Delegate to RDFRepository's implementation.
     */
    public static void main(String[] argv) throws Exception
    {
        getInstance().mainImpl(argv);
    }

    // returns Item that owns a Bitstream, or else throws.
    private DSpaceObject getOwningItem(DSpaceObject dso) throws SQLException
    {
        if (dso.getType() == Constants.BITSTREAM)
        {
            Bundle bn[] = ((Bitstream) dso).getBundles();
            if (bn.length > 0)
            {
                Item i[] = bn[0].getItems();
                if (i.length > 0)
                    return i[0];
            }
            throw new SQLException("Cannot find parent Item of Bitstream #"
                    + String.valueOf(dso.getID()));
        }
        else
            throw new SQLException("Object MUST be a Bitstream type="
                    + String.valueOf(dso.getType()) + ", id="
                    + String.valueOf(dso.getID()));
    }

    /**
     * Create URI under which statements are stored for the archival object. If
     * subject is a Bitstream, "promote" to its owning Item. Bundle has been
     * factored out by event processor. Use a DB-ID-based key so that History
     * can be ingested even when the object hasn't yet got a Handle; since it is
     * only an index to the triple-store it doesn't have to be persistent.
     */
    protected URI makeKey(Context context, DSpaceObject dso)
            throws SQLException
    {
        if (dso.getType() == Constants.BITSTREAM)
            dso = getOwningItem(dso);
        return makeSimpleKey(context, dso);
    }

    /**
     * Alternate version to create URI when DSpaceObject is not available; e.g.
     * for a history event on deleted object. This is not the default because we
     * need the object for tricks like getting the owning object of a Bitstream.
     */
    protected URI makeKey(int type, int id) throws SQLException
    {
        return makeSimpleKey(type, id);
    }
}
