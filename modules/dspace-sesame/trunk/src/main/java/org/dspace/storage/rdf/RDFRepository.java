/*
 * RDFRepository.java
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

package org.dspace.storage.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 * This implements an efficient, flexible storage mechanism for
 * RDF statements (triples).  Each statement is actually stored as
 * a "quad", the extra member is the URI representing a DSpace Object
 * to which the statement is attached.  This lets the repository
 * collect statements about an object and then return all of them
 * rapidly and easily by using this extra "index".
 * <p>
 * This repository is based on OpenRDF Sesame 2.0, release alpha4 (see
 *  http://www.openrdf.org/ for details).  It tries to obscure
 * most of the implementation details, but to work efficiently it is
 * necessary to expose some OpenRDF and Sesame data types associated
 * with RDF elements: URIs, resources, properties, and values.
 * <p>
 * This class must be subclassed to implement a repository; see the
 * History System implementation for a good example.
 * The subclass is expected to provide:
 *  - A dedicated directory in the filesystem for triplestore files, as a
 *    parameter to constructor. This should probably be a configuration option.
 *  - An implementation of makeKey(), tailored to the way it wants to
 *    associate statements with objects.
 *  - An implementation of main() that calls our mainImpl(), so each
 *    repository's command-line program accesses the correct repository.
 *  - A static function like getInstance() to create the single
 *    operating instance of the repository.
 * <p>
 * Also note that every open repository adds a shutdown hook to the JVM.
 * This means the JVM *MUST* run System.exit() when it terminates.
 * Falling off the end of main() is NOT an adequate substitute.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 */
public abstract class RDFRepository
    implements Runnable
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(RDFRepository.class);

    /**  choice of formats that we can read and write */
    public  enum FormatOfRDF { RDFXML, N3 };

    private RepositoryConnection theConn = null;

    private Repository theRep = null;

    // remember path, mainly for diagnostic printouts and dump label.
    private File fromPath = null;

    // shutdown hook.
    private Thread hook = null;

    /**
     * Constructor -- dirPath must be a filesystem directory
     * dedicated to this subclass of repository.  Nothing else may
     * write files there.
     */
    protected RDFRepository(File dirPath)
    {
        super();

        fromPath = dirPath;
        try
        {
            // use "Native" b-tree triple store
            Sail sail = new NativeStore(dirPath);
            theRep = new SailRepository(sail);
            theRep.initialize();

            // ensure repository gets shut down.
            hook = new Thread(this);
            Runtime.getRuntime().addShutdownHook(hook);
        } 
        catch (RepositoryException e) 
        {
            log.error("Failed to create Sesame repository:", e);
            throw new RuntimeException("Failed to create Sesame repository:", e);
		}
    }

    /**
     * Return URI to be used as a key relating to statements about
     * (or related to) this DSpace object.  The URI should correspond
     * one-to-one with the object, i.e. you can reconstruct the object
     * given the URI, and no other object may map to this URI.
     *
     * If you can count on all objects having a Handle available at
     * the time they are accessing this RDF repository, you can use
     * a Handle-based URI such as is created by makePersistentObjectURI()
     * but otherwise it may have to be based on the Database ID or other
     * unique characteristic.  Think about what happens at ingest time,
     * if a packager ingests your objects and crosswalks RDF, they may
     * not have a handle at the time they deposit RDF here.
     *
     * @param context - the DSpace Context
     * @param dso - any DSpace Object
     * @return an OpenRDF URI object representing the object.
     * @throws SQLException in the event of a database failure.
     */
    protected abstract URI makeKey(Context context, DSpaceObject dso)
        throws SQLException;

    /**
     * Sample implementation of makeKey() that bases a URI on the
     * distinguishing features of the DSpaceObject instance, its
     * Type and (database) ID.  This is NOT a persistent identifier,
     * but might be "good enough" for applications where the RDF
     * store is considered as ephemeral as the RDBMS.
     *
     * @param context - the dSpace context
     * @param dso - any DSpace object.
     * @return new URI or null if one cannot be created.
     * @throws SQLException if there is a problem accessing the database.
     */
    protected URI makeSimpleKey(Context context, DSpaceObject dso)
        throws SQLException
    {
        // if it's any kind of ingestion wrapper, get wrapped obj.
        int type = dso.getType();
        
        /* TODO: MRD uncomment with AIP CHANGES
        if (type == Constants.INGESTION_ITEM ||
            type == Constants.INGESTION_COLLECTION ||
            type == Constants.INGESTION_COMMUNITY)
            dso = ((IngestionWrapper)dso).getWrappedObject();
        */
        if (dso == null)
            return null;
        return makeSimpleKey(dso.getType(), dso.getID());
    }

    /**
     * Makes a non-persistent key based on the type and DB-ID of
     * a DSpace object -- for use in situations where the object
     * instance is not available, e.g. after the object has been
     * deleted.
     *
     * @param type - type of the DSpace object.
     * @param id - database ID of the DSpace object.
     * @return new URI or null if one cannot be created.
     * @throws SQLException if there is a problem accessing the database.
     */
    protected URI makeSimpleKey(int type, int id)
        throws SQLException
    {
        try
        {
            String frag = Constants.typeText[type] + "_" +
                            String.valueOf(id);
            return new URIImpl(
                new java.net.URI("info", "dspace/objectModel", frag).toString());
        }
        catch (java.net.URISyntaxException e)
        {
            log.error("Failed making URI of object: ",e);
            return null;
        }
    }

    /**
     * Add these namespace objects (prefix and URI) to the repository.
     * The subclass should call this whenever it opens the repository,
     * in case there were any changes in its set of namespaces (from
     * code change, patch, etc.).
     * Since this should only occur once in the lifetime of a JVM it
     * is cheap to do this every time to ensure we don't miss any changes.
     * @param namespaces array of OpenRDF Namespace objects binding prefix to URI.
     */
    public void addNamespaces(Namespace[] namespaces)
        throws RDFException
    {
        try
        {
            RepositoryConnection conn = theRep.getConnection();
            try
            {
                // Populate set with the NS's we already know about..
                Set<String> nsNames = new HashSet<String>();
                for (RepositoryResult<Namespace> ni = conn.getNamespaces();
                     ni.hasNext();)
                {
                    Namespace ns = ni.next();
                    nsNames.add(ns.getName());
                }
                 
                // and only add new ones.
                for (Namespace ns : namespaces)
                {
                    String name = ns.getName();
                    if (! nsNames.contains(name))
                        conn.setNamespace(ns.getPrefix(), name);
                }
            }
            finally
            {
                conn.close();
            }
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Returns the format most likely to apply to the filename.
     *
     * @param path file path, the extension is the part that will be judged.
     * @param deflt default format if choice cannot be made.
     * @return format corresponding to file or value of deflt.
     */
    public static FormatOfRDF guessFormat(File path, FormatOfRDF deflt)
    {
        String lpath = path.getName().toLowerCase();
        if (lpath.endsWith(".n3"))
            return FormatOfRDF.N3;
        else if (lpath.endsWith(".xml") || lpath.endsWith(".rdf"))
            return FormatOfRDF.RDFXML;
        else
            return deflt;
    }

    /**
     * Returns the format that most likely corresponds to the name in
     * the String argument.
     *
     * @param arg name of format e.g. from command-line argument
     * @param deflt default format if choice cannot be made.
     * @return format corresponding to arg or value of deflt.
     */
    public static FormatOfRDF guessFormat(String arg, FormatOfRDF deflt)
    {
        String targ = arg.trim();
        if (targ.equalsIgnoreCase("n3"))
            return FormatOfRDF.N3;
        else if (targ.equalsIgnoreCase("xml") || targ.equalsIgnoreCase("rdf") ||
                 targ.equalsIgnoreCase("rdfxml"))
            return FormatOfRDF.RDFXML;
        else
            return deflt;
    }

    /**
     * Returns the persistent, globally-unique URI of the given object,
     * if possible.  If there is no basis for a persistent URI (i.e. if
     * it has no Handle), returns null.
     *
     * @param context - the dSpace context
     * @param dso - any DSpace object.
     * @return new URI or null if one cannot be created.
     * @throws SQLException if there is a problem accessing the database.
     */
    public static URI makePersistentObjectURI(Context context, DSpaceObject dso)
        throws SQLException
    {
        String handle = null;
        String frag = null;
        if (dso != null)
        {
            // get bitstream's parent Item to form a Handle-based ID.
            if (dso.getType() == Constants.BITSTREAM)
            {
                Bundle bn[] = ((Bitstream)dso).getBundles();
                if (bn.length > 0)
                {
                    Item i[] = bn[0].getItems();
                    if (i.length > 0)
                    {
                        if (context == null)
                            handle = i[0].getHandle();
                        else
                            handle = HandleManager.findHandle(context, i[0]);
                    }
                    frag = String.valueOf(((Bitstream)dso).getSequenceID());
                }
            }

            // special case for EPerson, make up a mailto: URL
            else if (dso.getType() == Constants.EPERSON)
            {
                return(new URIImpl("mailto:"+((EPerson)dso).getEmail()));
            }

            // special case for Site, since findHandle() won't work.
            else if (dso.getType() == Constants.SITE)
            {
                handle = Site.getSiteHandle();
            }

            // any other kind of object, just get the handle.
            else
            {
                handle = dso.getHandle();
                if (handle == null && context != null)
                    handle = HandleManager.findHandle(context, dso);
            }
            if (handle != null)
                return new URIImpl(makeHandleURI(handle, frag));
        }
        return null;
    }

    /**
     * Returns the persistent, globally-unique URI of the given Handle.
     *
     * @param handle - string containing the bare handle, eg. "1721.1/13".
     * @return new URI or null if one cannot be created.
     */
    public static URI makePersistentObjectURI(String handle)
    {
        return new URIImpl(makeHandleURI(handle, null));
    }


    /**
     * Add an RDF statement related to a DSpace object indicated by the "key"
     * argument.  Key should be a URI returned by the subclass's version of
     * makeKey().
     *
     * The client can call addStatement with no preparation, since it auto-
     * matically opens a cached connection, but once it is done adding
     * statements it should call finishAdds() below.
     *
     * @param subject subject of the RDF statement
     * @param pred predicate of the RDF statement
     * @param obj object of the RDF statement
     * @param key extra "context" information stored with the triple that
     *            can be applied to retrieve it later; useful for relating
     *            statements to e.g. DSpace object model objects. Can be null.
     */
    public void addStatement(Resource subject, URI pred, Value obj, URI key)
        throws RDFException
    {
        try
        {
            getConnection().add(subject, pred, obj, key);
            // XXX only enable when needed for debugging, this spews a LOT of output for History.
            //if (log.isDebugEnabled())
            //    log.debug("Adding to Sesame, context="+((key == null)?"null":key.toString()));
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Signal that the client is done adding statements, after calling
     * addStatement().  Always be sure to call this after any calls
     * to addStatement() and other methods that implicitly create a connection
     * to the RDF repository.
     */
    public void finishAdds()
        throws RDFException
    {
        try
        {
            if (theConn != null)
            {
                theConn.close();
                log.debug("Closing Sesame connection.");
            }
            theConn = null;
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Ingest statements about the object described by key.  If key
     * is null, the statements are not attached to any object and may
     * not be retrieved by key later.
     *
     * @param context the URI under which to store any ingested triples, for
     *                later retrieval.
     * @param is stream containing RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void ingestStatementsOfKey(Resource context, InputStream is, FormatOfRDF fmt)
        throws IOException,  RDFException
    {
        try
        {
            RDFFormat readFormat;
            switch (fmt)
            {
                case N3: readFormat = RDFFormat.TURTLE; break;
                case RDFXML: readFormat = RDFFormat.RDFXML;  break;
                default:
                    throw new UnsupportedRDFormatException("No reader for format="+fmt);
            }
             
            RepositoryConnection conn = getConnection();
            conn.add(is, "", readFormat, context);
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
        catch (RDFParseException e)
        {
            throw new RDFException(e);
        }
        catch (UnsupportedRDFormatException e)
        {
            throw new RDFException(e);
        }
        finally
        {
            finishAdds();
        }
    }

    /**
     * Ingest statements about the given object.  Calls makeKey() to
     * create the key stored with each statement.
     *
     * @param context the DSpace context
     * @param dso DSpace object,  ingested statements will be keyed to it.
     * @param is stream containing RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void ingestStatementsOfObject(Context context, DSpaceObject dso,
                                      InputStream is, FormatOfRDF fmt)
        throws SQLException, IOException, RDFException
    {
        ingestStatementsOfKey(makeKey(context, dso), is, fmt);
    }

    /**
     * Export all statements in the repository to the stream.
     *
     * @param os stream to which to write RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void export(OutputStream os, FormatOfRDF fmt)
        throws RDFException
    {
        try
        {
            RDFHandler writer = getWriterForFormat(os, fmt);
            RepositoryConnection conn = getConnection();
           
            
            conn.export(writer);
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
        catch (RDFHandlerException e)
        {
            throw new RDFException(e);
        } 
        finally
        {
            finishAdds();
        }
    }

    /**
     * Returns true if there are any statements in the repository
     * related to the given object.
     *
     * @param context the DSpace context
     * @param dso DSpace object to look for statements keyed to it.
     * @return true if the repository contains any statements with that key.
     */
    public boolean hasStatementsOfObject(Context context, DSpaceObject dso)
        throws RDFException
    {
        try
        {
            URI uri = makeKey(context, dso);
            boolean result = getConnection().hasStatement(null,uri, null,false);
            if (log.isDebugEnabled())
                log.debug("hasStatementsOfObject("+dso.toString()+"), uri="+uri.toString()+", result="+String.valueOf(result));
            return result;
        }
        catch (SQLException e)
        {
            throw new RDFException(e);
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Evaluate a query that returns statements, writing the result to
     * the given stream.  The query may be in SERQL or SPARQL, as chosen
     * by the query-lanaguge argument.
     *
     * @param lang one of the OpenRDF Sesame QueryLanguage enums.
     * @param os stream to which to write RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void evaluateQuery(QueryLanguage lang, String query, OutputStream os, FormatOfRDF fmt)
        throws RDFException
    {
        try
        {
            RDFWriter writer = getWriterForFormat(os, fmt);
            RepositoryConnection conn = getConnection();
            log.debug("Evaluating graph query=\""+query+"\"");
            
            GraphQuery gq = conn.prepareGraphQuery(lang, query);
       
            gq.evaluate(
            	writer
            );
            
        } catch (QueryEvaluationException e) {
        	throw new RDFException(e);
		} catch (RDFHandlerException e) {
			throw new RDFException(e);
		} catch (RepositoryException e) {
			throw new RDFException(e);
		} catch (MalformedQueryException e) {
			throw new RDFException(e);
		}
        finally
        {
            finishAdds();
        }
    }

    /**
     * Export the statements related to a given object to a stream.
     *
     * @param context the DSpace context
     * @param dso DSpace object to look for statements keyed to it.
     * @param os stream to which to write RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void exportStatementsOfObject(Context context, DSpaceObject dso,
                                         OutputStream os, FormatOfRDF fmt)
        throws SQLException, RDFException
    {
        exportStatementsOfKey(makeKey(context, dso), os, fmt);
    }

    /**
     * Export the statements related to a given object to a stream.
     *
     * @param key the URI under which triples were stored.
     * @param os stream to which to write RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void exportStatementsOfKey(URI key, OutputStream os, FormatOfRDF fmt)
        throws SQLException, RDFException
    {
        try
        {
            if (log.isDebugEnabled())
                log.debug("Exporting statements for context=\""+key.toString()+"\"");
            getConnection().export(getWriterForFormat(os, fmt), key);
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
        catch (RDFHandlerException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Export the statements related to ALL of the given objects
     * in one document.
     *
     * @param context the DSpace context
     * @param objs List of DSpace objects to look for statements keyed to.
     * @param os stream to which to write RDF in the specified format
     * @param fmt format of the RDF serialized on the stream
     */
    public void exportStatementsOfObjects(Context context, List objs,
                                          OutputStream os, FormatOfRDF fmt)
        throws SQLException, RDFException
    {
        try
        {
            RepositoryConnection c = getConnection();
            RDFHandler hnd = getWriterForFormat(os, fmt);
            hnd.startRDF();
            for (RepositoryResult<Namespace> ni = c.getNamespaces();
                 ni.hasNext();)
            {
                Namespace ns = ni.next();
                hnd.handleNamespace(ns.getPrefix(), ns.getName());
            }
            for (Iterator oi = objs.iterator(); oi.hasNext();)
            {
                URI key = makeKey(context, (DSpaceObject)oi.next());
                if (log.isDebugEnabled())
                    log.debug("Exporting statements for context=\""+key.toString()+"\"");
                
//              Get all statements in the context
                RepositoryResult<Statement> result =
                      c.getStatements(null, null, null, true, key);

                try {
                   while (result.hasNext()) {
                      Statement st = result.next();
                      hnd.handleStatement(st);
                   }
                }
                finally {
                   result.close();
                }
            }
            hnd.endRDF();
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
        catch (RDFHandlerException e)
        {
            throw new RDFException(e);
        }
    }

    /**
     * Remove statements about the given key.
     *
     * @param key the URI under which triples were stored.
     */
    public void removeStatementsOfKey(URI key)
        throws SQLException, RDFException
    {
        try
        {
            if (log.isDebugEnabled())
                log.debug("Removinging statements for context=\""+key.toString()+"\"");
  
            getConnection().clear(key);
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }


    /**
     * Clear all statements from the repository.  Use caution!
     */
    public void clear()
        throws RDFException
    {
        try
        {
            getConnection().clear();
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
    }

    // for thread on shutdown hook, auto-shutdown repository.
    public void run()
    {
        shutdownInternal(false);
    }

    /**
     * Shutdown the repository; also unregisters the exit hook.
     * After calling this, there must be no more activity on the respository.
     */
    public void shutdown()
    {
        shutdownInternal(true);
    }

    //-------------------------------------------------------------------
    // internal methods

    // shutdown before terminating the JVM, maybe unregister hook
    private void shutdownInternal(boolean unregister)
    {
        try
        {
            if (unregister && hook != null)
            {
                Runtime.getRuntime().removeShutdownHook(hook);
                hook = null;
            }
            if (theRep != null)
            {
                finishAdds();
                log.info("shutting down the RDF repository "+theRep.toString());
                theRep.shutDown();
                theRep = null;
            }
        }
        catch (Exception e)
        {
            log.error("Got Exception while shutting down: ",e);
        }
    }

    // get this instance's cached private Connection
    private RepositoryConnection getConnection()
        throws RepositoryException
    {
        if (theConn == null)
        {
            theConn = theRep.getConnection();
        }
        return theConn;
    }

    /**
     * Make a Handle-format URI for an object.  This is the
     * one place to change, to alter the format of all Handle URIs.
     * <p>
     * XXX FIXME: This URI format is NOT final, it is just a prototype.
     * Layout is e.g. "info:dspace/handle#1721.2/13"
     *   (or with fragment): "info:dspace/handle#1721.2/13:FOO"
     */
    private static String makeHandleURI(String hdl, String fragment)
    {
        try
        {
            if (fragment != null)
                hdl += ":"+fragment;
            return new java.net.URI("info", "dspace/handle", hdl).toString();
        }
        catch (java.net.URISyntaxException e)
        {
            return null;
        }
    }

    private static RDFWriter getWriterForFormat(OutputStream os, FormatOfRDF format)
    {
        switch (format)
        {
            case RDFXML: return new RDFXMLWriter(os);
            case N3: return new N3Writer(os);
        }
        throw new IllegalArgumentException("Unknown writer format: "+format);
    }

    // list the context keys; mostly for diagnostics
    private void exportContextIDs(OutputStream os)
        throws RDFException
    {
        try
        {
            RepositoryConnection conn = getConnection();
            PrintStream ps = new PrintStream(os);
     
            RepositoryResult<Resource> ic = conn.getContextIDs();
            while (ic.hasNext())
            {
                Resource r = (Resource)ic.next();
                ps.println(r.toString());
            }
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        } 
        finally
        {
            finishAdds();
        }
    }

    /**
     * Dump all quads (and namespaces) in repository for an external backup.
     * Format is based on NTriples and N3 so it can be parsed easily,
     * e.g. for migration to a different repository.
     *
     * If compression is requested, use GZIP library because it is compatible
     * with external open-source tools.  The compressed archive is thus
     * accessible without the Java infrastructure so it will be useful
     * to preservationists who might be attempting to recover a
     * DSpace archive without the DSpace software.
     */
    private void dump(OutputStream os, boolean compress)
        throws IOException, SQLException, RDFException
    {
        try
        {
            if (compress)
                os = new GZIPOutputStream(os);
            PrintStream ps = new PrintStream(os);
            ps.println("# Dump of all quads in DSpace RDFRepository from "+fromPath.toString());
            ps.println("#\n# IMPORTANT NOTE: This is a NONSTANDARD FORMAT, it is not N3 or NTriples ");
            ps.println("# although it looks similar.  It is an application-level backup of the ");
            ps.println("# contents of a DSpace RDF repository; restore it with the same subclass ");
            ps.println("# of org.dspace.storage.rdf.RDFRepository that dumped it. \n#");
            ps.println("# Namespaces:");
            for (RepositoryResult<Namespace> ic = getConnection().getNamespaces();
                 ic.hasNext();)
            {
                Namespace ns = ic.next();
                StringBuffer line = new StringBuffer("@prefix ");
                line.append(ns.getPrefix()).append(": ");
                line.append(NTriplesUtil.toNTriplesString(new URIImpl(ns.getName())))
                    .append(" .");
                ps.println(line);
            }
            ps.println("# quad format: Context   Subject   Predicate   Object");
            for (RepositoryResult<Statement> ic = getConnection().getStatements(null,null,null,false);
                 ic.hasNext();)
            {
                Statement st = ic.next();
                StringBuffer line = new StringBuffer(80);
                Resource key = st.getContext();
                if (key == null)
                    line.append("null ");
                else
                    line.append(NTriplesUtil.toNTriplesString(key)).append(" ");
                line.append(NTriplesUtil.toNTriplesString(st.getSubject()))
                    .append(" ");
                line.append(NTriplesUtil.toNTriplesString(st.getPredicate()))
                    .append(" ");
                line.append(NTriplesUtil.toNTriplesString(st.getObject()))
                    .append(" .");
                ps.println(line.toString());
            }
            ps.close();
        }
        catch (RepositoryException e)
        {
            throw new RDFException(e);
        }
        finally
        {
            finishAdds();
        }
    }

    /**
     * read back a dump produced by dump() above.
     * adds the dumped namespaces and statements to the archive, so it
     * behaves more like an ingestion than strictly restoring the previous
     * state.  For a true "restore", empty the repo with the -E option first.
     */
    private void restore(InputStream in, boolean compress)
        throws IOException, RepositoryException, RDFException
    {
        if (compress)
            in = new GZIPInputStream(in);

        LineNumberReader br = new LineNumberReader(new InputStreamReader(in));
        RepositoryConnection conn = getConnection();
        ValueFactory vf = theRep.getValueFactory();

        try
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                // ignore leading whitespace.
                line = line.trim();
             
                // ignore comment
                if (line.startsWith("#"))
                    continue;
             
                // namespace:  "@prefix  PREFIX:  <URI> ."
                if (line.startsWith("@prefix "))
                {
                    String word[] = line.split("\\s", 4);
                    if (word.length != 4 || !word[3].equals("."))
                        throw new IllegalArgumentException("Badly formed namespace line.");
                    URI name = NTriplesUtil.parseURI(word[2], vf);
                    if (word[1].endsWith(":"))
                        conn.setNamespace(word[1].substring(0,word[1].length()-1),
                                          name.toString());
                    else
                        throw new IllegalArgumentException("Namespace prefix must end with colon (:).");
                }
             
                // else assume  <context> <subj> <pred> <value> .
                // fortunately, context/subj/pred may NOT contain whitespace
                // so we can assume any whitespace is a separator.  value can
                // be literal that does contain whitespace.
                //
                // NOTE: NTriple parser is not tolerant of leading or trailing
                // whitespace, so be sure to strip off all whitespace from words
                else
                {
                    URI context = null;
                    String word[] = line.split("\\s", 4);

                    // sanity checks
                    if (word.length < 4)
                        throw new IllegalArgumentException("not enough terms for an RDF quad.");
                    if (! word[3].endsWith("."))
                        throw new IllegalArgumentException("line does not end with period (.).");
                    word[3] = word[3].substring(0, word[3].length()-1).trim();
             
                    // special handling for null contexts
                    if (!word[0].equals("null"))
                        context = NTriplesUtil.parseURI(word[0], vf);
             
                    conn.add(NTriplesUtil.parseResource(word[1], vf),
                             NTriplesUtil.parseURI(word[2], vf),
                             NTriplesUtil.parseValue(word[3], vf),
                             context);
                }
            }
        }
        // add line number to all messages, INCLUDING the onese thrown
        // by NTriplesUtil parser which also use this exception:
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Illegal dump format at line# "+String.valueOf(br.getLineNumber())+", "+e.getMessage());
        }
        finally
        {
            finishAdds();
        }
    }

    // prints usage info to System.out & dies.
    private void Usage(Options options, int status, String msg)
    {
        HelpFormatter hf = new HelpFormatter();
        if (msg != null)
            System.out.println(msg+"\n");
        hf.printHelp(this.getClass().getName()+" [options] [file..]\n",
                       options, false);
        System.exit(status);
    }


    /**
     * "main" implementation that should be called from the subclass's
     * main() to give command-line access to the repository.
     * The significant commands are:
     *
     *  -h  -- get help on options
     *  -d <HANDLE> [-f fmt] -- disseminate RDF associated with given object.
     *  -s <HANDLE> [-f fmt] [file] -- submit RDF to be associated with given object.
     *  -i  [file...]  -- ingest RDF from file, but NOT associated with object.
     *  -x  -- export entire contents of repository
     *  -q <QUERY> [-l <LANG>] -- execute a query in the given language
     *  -z -- zero repository, deletes all RDF statements. DANGEROUS.
     *  -C -- list context key URIs (for debugging/maintenance)
     *  -Q -- dump entire repository as quads (for debugging/maintenance)
     *
     */
    protected void mainImpl(String[] argv)
        throws Exception
    {
        Options options = new Options();

        options.addOption("h", "help", false, "show help message");
        options.addOption("d", "disseminate", true, "disseminate statements about the following Handle, to standard output");
        options.addOption("f", "format", true, "format keyword, i.e. \"n3\", \"xml\", etc. default=n3");
        options.addOption("i", "ingest", false, "ingest the following RDF files without specific object");
        options.addOption("s", "submit", true, "submit external file of statements about a specific Object, requires file argument.");
        options.addOption("x", "export", false, "export the repository to standard output.");
        options.addOption("E", "empty-repository", false, "Empty (clear)  the repository.");
        options.addOption("C", "list-context-keys", false, "list context keys in the repository.");
        options.addOption("D", "dump-quads", false, "dump out all quads (triple + context) in repository.");
        options.addOption("R", "restore-quads", false, "restore the results of a dump-quads to this repository, as an ingest.  present contents are left in place.");
        options.addOption("z", "compress", false, "enable GZIP compression on output of dump-quads or input of restore-quads.");
        options.addOption("q", "query", true, "execute SERQL/SPARQL query on repository, print the results.");
        options.addOption("l", "language", true, "set the query language used for -q; SERQL is the default.");
        options.addOption("k", "key", false, "specify key instead of Handle for disseminate or submit instead of looking up handle.");

        try
        {
            CommandLine line = (new PosixParser()).parse(options, argv);
            if (line.hasOption("h"))
                Usage(options, 0, null);
         
            try
            {
                // ingest external RDF file(s) un-related to an Object
                if (line.hasOption("i"))
                {
                    String files[] = line.getArgs();
                    for (String path : files)
                    {
                        ingestStatementsOfKey(null, new FileInputStream(path),
                               guessFormat(new File(path),
                                 guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML)));
                    }
                }

                // submit external RDF ..FOR a specific Object.
                else if (line.hasOption("s"))
                {
                    String hdl = line.getOptionValue("s");
                    Context c = new Context();
                    try
                    {
                        String files[] = line.getArgs();
                        if (files.length < 1)
                            Usage(options, 1, "Missing input file.");
                        if (line.hasOption("k"))
                            ingestStatementsOfKey(new URIImpl(hdl),
                              new FileInputStream(files[0]),
                              guessFormat(new File(files[0]),
                                         guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML)));
                        else
                        {
                            DSpaceObject dso = HandleManager.resolveToObject(c, hdl);
                            if (dso == null)
                            {
                                System.err.println("No object found for handle: "+hdl);
                                System.exit(2);
                            }
                            ingestStatementsOfObject(c, dso,
                              new FileInputStream(files[0]),
                              guessFormat(new File(files[0]),
                                         guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML)));
                        }
                    }
                    finally
                    {
                        c.abort();
                    }
                }

                // disseminate RDF for one Handle
                else if (line.hasOption("d"))
                {
                    String hdl = line.getOptionValue("d");
                    Context c = new Context();
                    try
                    {
                        if (line.hasOption("k"))
                            exportStatementsOfKey(new URIImpl(hdl), System.out,
                                                  guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML));
                        else
                        {
                            DSpaceObject dso = HandleManager.resolveToObject(c, hdl);
                            if (dso == null)
                            {
                                System.err.println("No object found for handle: "+hdl);
                                System.exit(2);
                            }
                            exportStatementsOfObject(c, dso, System.out,
                                                  guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML));
                        }
                    }
                    finally
                    {
                        c.abort();
                    }
                }

                // display the results of an arbitrary query
                else if (line.hasOption("q"))
                {
                    String query = line.getOptionValue("q");

                    // check for language option
                    QueryLanguage lang = QueryLanguage.SERQL;
                    if (line.hasOption("l"))
                    {
                        if (line.getOptionValue("l").equalsIgnoreCase("SERQL"))
                            lang = QueryLanguage.SERQL;
                        else if (line.getOptionValue("l").equalsIgnoreCase("SPARQL"))
                            lang = QueryLanguage.SPARQL;
                        else
                            Usage(options, 1, "Query language must be either SERQL or SPARQL");
                    }
                    evaluateQuery(lang, query, System.out,
                       guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML));
                }

                // list context keys
                else if (line.hasOption("C"))
                {
                    exportContextIDs(System.out);
                }

                // dump out all quads e.g. for source-level backup of repository
                // NOTE: This is a non-standard format peculiar to RDFRepo
                else if (line.hasOption("D"))
                {
                    // if there's a file arg, send output there, else stdout
                    OutputStream out = System.out;
                    String files[] = line.getArgs();
                    if (files.length > 0)
                        out = new FileOutputStream(new File(files[0]));
                    dump(out, line.hasOption("z"));
                }

                // restore repository (ingest) from dump of quads created by "D" option
                // NOTE: This is a non-standard format peculiar to RDFRepo
                else if (line.hasOption("R"))
                {
                    // if there's a file arg, get input from there, else stdin
                    InputStream in = System.in;
                    String files[] = line.getArgs();
                    if (files.length > 0)
                        in = new FileInputStream(new File(files[0]));
                    restore(in, line.hasOption("z"));
                }

                // export entire repository as RDF triples;
                // NOT suitable for backup/restore since the context would
                // be lost.
                else if (line.hasOption("x"))
                {
                    export(System.out,
                               guessFormat(line.getOptionValue("f", ""), FormatOfRDF.RDFXML));
                }

                // clear repository
                else if (line.hasOption("E"))
                {
                    System.err.print("WARNING: This DELETES ALL RDF DATA IN THE REPOSITORY!\nAre you sure? [y/N]");
                    byte buf[] = new byte[128];
                    int buflen = System.in.read(buf);
                    String answer = new String(buf, 0, buflen);
                    if (answer.startsWith("y") || answer.startsWith("Y"))
                    {
                        System.err.println("Clearing RDF Repository ...");
                        clear();
                    }
                    else
                        System.err.println("Aborted, RDF Repository is unchanged.");
                }

                // for debugging, if "k" option given alone, print out key for Handle
                else if (line.hasOption("k"))
                {
                    String args[] = line.getArgs();
                    if (args.length > 0)
                    {
                        Context c = new Context();
                        DSpaceObject dso = HandleManager.resolveToObject(c, args[0]);
                        if (dso == null)
                        {
                            System.err.println("No object found for handle: "+args[0]);
                            System.exit(2);
                        }
                        System.err.println(makeKey(c, dso).toString());
                    }
                    else
                        Usage(options, 1, "Missing handle for -k option.");
                }

                else
                    Usage(options, 1, "Missing action option.");

            }
            finally
            {
                finishAdds();
            }
        }
        catch (org.apache.commons.cli.ParseException pe)
        {
            Usage(options, 1, "Error in arguments: "+pe.toString());
        }
        catch (Throwable e)
        {
            System.err.println("Got exception: "+e.toString());
            e.printStackTrace();
        }
        finally
        {
            System.exit(0);
        }
    }
}
