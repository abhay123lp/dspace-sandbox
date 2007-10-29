/*
 * PolicyRepository.java
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

import java.io.File;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
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
 *
 * Implements an RDF triple-store for Policy RDF statements associated
 * with objects in the DSpace data model.
 * It is built on RDFRepository, see that class for details.
 *
 * There is typically one instance of this class in a JVM, retrieve it with
 * the getInstance() method.
 *
 * Configuration:
 *  policy.dir -- value is pathname of a directory to house the repository.
 *              the contents are opaque.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 * @see RDFRepository
 */
public class PolicyRepository
    extends RDFRepository
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(PolicyRepository.class);

    /** format of choice for disseminating and ingesting Policy data */
    public static final RDFRepository.FormatOfRDF policyFormat = RDFRepository.FormatOfRDF.RDFXML;

    /** well-known namespace URIs */
    public static final String RDF_NS_URI = RDF.NAMESPACE;

    public static final String RDFS_NS_URI = RDFS.NAMESPACE;

    public static final String XMLS_NS_URI = XMLSchema.NAMESPACE;

    public static final String OWL_NS_URI = "http://www.w3.org/2002/07/owl#";

    // Unqualified Dublin Core
    // public static final String DC_NS_URI = "http://purl.org/dc/elements/1.1/";

    // XXX BOGUS namespace for Policy entries XXX PLACEHOLDER
    public static final String DSPACE_HISTORY_NS_URI = "http://www.dspace.org/history#";

    // XXX BOGUS namespace for Policy entries XXX PLACEHOLDER
    public static final String DSPACE_OBJECT_NS_URI = "http://www.dspace.org/objectModel#";

    // Policy language namespaces
    public static final String REI_POLICY_NS_URI =
        "http://www.cs.umbc.edu/~lkagal1/rei/ontologies/ReiPolicy.owl#";

    public static final String REI_ACTION_NS_URI =
        "http://www.cs.umbc.edu/~lkagal1/rei/ontologies/ReiAction.owl#";

    public static final String REI_CONSTRAINT_NS_URI =
        "http://www.cs.umbc.edu/~lkagal1/rei/ontologies/ReiConstraint.owl#";
    public static final String REI_DEONTIC_NS_URI =
        "http://www.cs.umbc.edu/~lkagal1/rei/ontologies/ReiDeontic.owl#";
    public static final String REI_ENTITY_NS_URI =
        "http://www.cs.umbc.edu/~lkagal1/rei/ontologies/ReiEntity.owl#";
    public static final String DSPACE_INST_NS_URI =
        "http://www.dspace.org/instances.rdf#";
    public static final String DSPACE_RULES_NS_URI =
        "http://www.dspace.org/rules.rdfs#";

    // XXX MUST keep this up to date when adding namespaces above:
    public static final Namespace namespaces[] =
    {
        new NamespaceImpl("xmls",       XMLS_NS_URI),
        new NamespaceImpl("rdf",        RDF_NS_URI),
        new NamespaceImpl("rdfs",       RDFS_NS_URI),
        new NamespaceImpl("owl",        OWL_NS_URI),

        new NamespaceImpl("policy",     REI_POLICY_NS_URI),
        new NamespaceImpl("action",     REI_ACTION_NS_URI),
        new NamespaceImpl("constraint", REI_CONSTRAINT_NS_URI),
        new NamespaceImpl("deontic",    REI_DEONTIC_NS_URI),
        new NamespaceImpl("entity",     REI_ENTITY_NS_URI),
        new NamespaceImpl("dso",        DSPACE_OBJECT_NS_URI),
        new NamespaceImpl("inst",       DSPACE_INST_NS_URI),
        new NamespaceImpl("dsr",        DSPACE_RULES_NS_URI),
        new NamespaceImpl("history",    DSPACE_HISTORY_NS_URI),
    };

    // one shared RDF repository object - instances use Connections.
    private static PolicyRepository theInstance = null;

    private PolicyRepository(File dir)
    {
        super(dir);
    }

    /**
     * NEVER returns null, throws upon failure (unlikely)
     */
    static public PolicyRepository getInstance()
    {
        if  (theInstance == null)
        {
            try
            {
                theInstance = new PolicyRepository(new File(ConfigurationManager.getProperty("policy.dir")));
                theInstance.addNamespaces(namespaces);
            }
            catch (RDFException e)
            {
                log.error("Failed to create Policy Repository: ", e);
            }
        }
        return theInstance;
    }

    // delegate to RDFRepository
    public static void main(String[] argv)
        throws Exception
    {
        getInstance().mainImpl(argv);
    }

    /**
     * Override the URI method to make URI *WITHOUT* Handle, since
     * we need it to ingest Policies before Handle exists..
     * @param context - the dSpace context
     * @param dso - any DSpace object.
     * @return new URI or null if one cannot be created.
     * @throws SQLException if there is a problem accessing the database.
     */
    protected URI makeKey(Context context, DSpaceObject dso)
        throws SQLException
    {
        return makeSimpleKey(context, dso);
               /**** XXX
        return
        try
        {
            String frag = Constants.typeText[dso.getType()] + "_" +
                            String.valueOf(dso.getID());
            return new URIImpl(
                new java.net.URI("info", "dspace/objectModel", frag).toString());
        }
        catch (java.net.URISyntaxException e)
        {
            log.error("Failed making URI of object: ",e);
            return null;
        }
                        ***/
    }
}
