/*
 * AIPManager.java
 *
 * Version: $Revision: 1.19 $
 *
 * Date: $Date: 2006/03/30 02:46:42 $
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

package org.dspace.administer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.PosixParser;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InternalAIP;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Site;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageException;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;
import org.dspace.authorize.AuthorizeException;

/**
 * Administrative command-line driver for the Internal AIP mechanism.
 *
 * The following options are mutually exclusive and determine the
 * primary function of the application:
 *
 *  -u update: (and create) AIPs, ensure internal AIP for each object is up-to-date.
 *
 *  -c check: test if a given bitstream is really an Internal AIP and if so,
 *     add it to the AIP table.  Used to rebuild InternalAIP table after RDBMS failure.
 *
 *  -q query: display the current state of the internal AIP for an object.
 *
 *  -r restore: restore the state and contents of a DSpace Object from its
 *     internal AIP.  Used to restore the archive after RDBMS failure.
 *
 *  -P probe: test whether the indicated bitstream (or Standard Input) is
 *     an internal AIP manifest.
 *
 *  -D delete an internal AIP, requires -i option with AIP number.
 *
 *  Configurations:
 *     aipManager.dispatcher - name of dispatcher to use with this app;
 *       recommend one with History turned off to prevent double entries.
 *
 * @author Larry Stone
 * @version $Revision: 1.6 $
 */
public class AIPManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AIPManager.class);

    /**
     * find or create an AIP record, and update the internal AIP
     */
    private static boolean doUpdateAIP(Context context, DSpaceObject dso,
                            boolean force, int verbosity)
        throws SQLException, IOException, AuthorizeException, InterruptedException
    {
        InternalAIP ia = InternalAIP.find(context, dso);
        if (ia == null)
            ia = InternalAIP.create(context, dso);

        // log the start of AIP generation so we'll know what object any following errors belong to.
        if (verbosity > 1)
            System.out.println("Starting AIP for handle="+dso.getHandle()+"...");

        try
        {
            boolean result = ia.updateAIP(context, force);
            if (result && verbosity > 0)
                report(dso);
            return result;
        }
        catch (PackageException e)
        {
            System.out.println("ERROR: "+e.toString());
            log.error("Failed while updating AIP: ",e);
        }
        catch (CrosswalkException e)
        {
            System.out.println("ERROR: "+e.toString());
            log.error("Failed while updating AIP: ",e);
        }
        return false;
    }

    // Check an internal AIP bitstream, and its membership in InternalAIP table.
    // -force means reestablish the table entry if necessary.
    // catch common package exceptions so -a can process all bitstreams.
    private static void doCheckAIP(Context context, Bitstream bitstream, boolean force, int verbosity)
        throws AuthorizeException, SQLException, IOException
    {
        System.out.print("AIP in Bitstream #"+String.valueOf(bitstream.getID()));
        try
        {
            InternalAIP ia = InternalAIP.checkAIPBitstream(context, bitstream, force);
            if (ia == null)
            {
                // attempt to find the ID and date of the AIP
                if (verbosity > 1)
                {
                    String hdl = InternalAIP.getHandleOfAIPBitstream(context, bitstream);
                    Date creat = InternalAIP.getCreateDateOfAIPBitstream(context, bitstream);
                    System.out.println("...NOT attached to InternalAIP; hdl="+
                      (hdl == null ? "[null]":hdl)+", created="+(creat==null?"[null]":creat.toString()));
                }
                else
                    System.out.println("...NOT attached to any InternalAIP.");
            }
            else
            {
                System.out.print("...InternalAIP ID="+String.valueOf(ia.getID()));
                if (verbosity > 0)
                {
                    if (ia.getDSpaceObject() != null)
                        System.out.println(", for "+ia.getDSpaceObject().toString());
                    else if (ia.getHandle() != null)
                        System.out.println(", for handle="+ia.getHandle());
                    else
                        System.out.println(", for (UNKNOWN OBJECT)");
                }
                else
                        System.out.println("");
            }
        }
        catch (PackageException e)
        {
            System.out.println("...ERROR: "+e.toString());
            log.error("Failed while checking AIP: ",e);
        }
        catch (CrosswalkException e)
        {
            System.out.println("...ERROR: "+e.toString());
            log.error("Failed while checking AIP: ",e);
        }
    }


    // restore an object from its internal AIP
    private static void doRestoreAIP(Context context, InternalAIP aip, DSpaceObject defaultParent, boolean force, int verbosity)
        throws CrosswalkException, PackageException, AuthorizeException,
            SQLException, IOException
    {
        if (verbosity >= 1)
        {
            System.out.println("Restoring AIP # "+String.valueOf(aip.getID())+
                 ", handle="+aip.getHandle()+", into Default parent="+defaultParent.toString());
        }
        aip.restoreFromAIP(context, defaultParent);
    }

    // call doRestoreAIP but swallow any exceptions;
    // returns true upon success
    // Also isolate this operation in its own "transaction" by comitting
    // the context immediately upon success, and rolling back an error.
    private static boolean atomicRestoreAIP(Context context, InternalAIP aip, DSpaceObject defaultParent, boolean force, int verbosity)
    {
        boolean result = false;
        try
        {
            doRestoreAIP(context, aip, defaultParent, force, verbosity);
            context.commit();
            result = true;
        }
        catch (Exception e)
        {
            log.error("Failed restoring AIP: ",e);
            System.err.println(e.toString()+"\nStack trace follows:");
            e.printStackTrace();
            System.err.println(e+"\nContinuing...\n");
            try
            {
                context.rollback();
            }
            catch (Exception ee)
            {
                log.error("Failed in rollback after failure restoring AIP: ",ee);
            }
        }
        return result;
    }

    private static void usageError(String msg)
    {
        System.err.println(msg+"\nAdd option --help for a list of options.");
        System.exit(1);
    }

    private static DSpaceObject getIasDSO(Context context, String val)
        throws SQLException
    {
        DSpaceObject result = HandleManager.resolveToObject(context, val);
        if (result == null)
        {
            System.err.println("ERROR: Could not find DSpace Object for identifier=\""+val+"\".");
            System.exit(3);
        }
        return result;
    }


    // interpret the value of the -i option
    private static InternalAIP getIasInternalAIP(Context context, String val)
        throws SQLException
    {
        InternalAIP result = null;

        // is it a handle? i.e. contains '/'
        if (val.indexOf("/") >= 0)
        {
            int id = HandleManager.getID(context, val);
            if (id < 0)
            {
                System.err.println("ERROR: Handle \""+val+"\" does not exist.");
                System.exit(2);
            }
            else
                result = InternalAIP.findByHandleID(context, id);
        }

        // it's a DB ID number of an Internal AIP
        else
        {
            result = InternalAIP.find(context, Integer.parseInt(val));
        }

        if (result == null)
        {
            System.err.println("ERROR: Could not find Internal AIP for identifier=\""+val+"\".");
            System.exit(3);
        }
        return result;
    }

    // interpret the value of the -b option
    private static Bitstream getValueOfB(Context context, String val)
        throws SQLException, URISyntaxException
    {
        Bitstream result = null;

        if (val.indexOf(":") >= 0)
            result = Bitstream.dereferenceAbsoluteURI(context, new URI(val));
        else
            result = Bitstream.find(context, Integer.parseInt(val));

        if (result == null)
        {
            System.err.println("ERROR: Could not find Bitstream for identifier=\""+val+"\".");
            System.exit(3);
        }
        return result;
    }

    /**
     * Usage: AIPManager [options]
     * Creates and/or updates internal AIPs in asset store.
     * Or, restores archive from AIPs in asset store.
     * See class comments for the major functions.
     *  --all | --instance <handle>  choose
     *  --force - update all selected AIPs whether or not it is needed.
     *  --verbose (repeat for more verbosity, twice makes sense)
     *  --help
     */
    public static void main(String[] argv) throws Exception
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        int status = 0;

        // mutually exclusive function options:
        OptionGroup func = new OptionGroup();
        func.addOption(new Option("u", "update", false, "Function: Update AIP(s). Requires -i or -a."));
        func.addOption(new Option("c", "check", false, "Function: Check AIP bitstream(s) and optionally restore AIP records; requires -a or -b."));
        func.addOption(new Option("r", "restore", false, "Function: Restore object from AIP(s), requires either -a or (-i and -p)."));
        func.addOption(new Option("q", "query", false, "Function: Query state of internal AIP for object; requires -i."));
        func.addOption(new Option("D", "delete", false, "Function: delete an internal AIP, requires -i."));
        func.addOption(new Option("P", "Probe", false, "Probe bitstream or standard input to see if it is an AIP manifest."));

        Options options = new Options();
        options.addOptionGroup(func);
        options.addOption("v", "verbose", false,
                "verbose diagnostic messages");
        options.addOption("f", "force", false,
                "force all selected objects to update AIPs");
        options.addOption("i", "identifier", true,
                        "Indicates AIP to operate on by Handle of its object, or InternalAIP DB ID.");
        options.addOption("a", "all", false,
                                "update AIPs for ALL archived objects");
        options.addOption("b", "bitstream", true, "Indicate bitstream to -c function, or by itself retrieve bitstream.  Value is URI or DB ID.");
        options.addOption("h", "help", false, "help");
        options.addOption("p", "parent", true, "Handle of default parent for AIP to be --restore'd");
        options.addOption("e", "eperson", true, "Email address of EPerson to impersonate");
        options.addOption("n", "count", true, "Limit the total number of restore or update objects with -a option");
        CommandLine line = parser.parse(options, argv);

        // all this to let -v option be doubled up for increased verbosity..
        int verbosity = 0;
        int verboseId = options.getOption("v").getId();
        Option opts[] = line.getOptions();
        for (int i = 0; i < opts.length; ++i)
        {
            if (opts[i].getId() == verboseId)
                ++verbosity;
        }
        boolean isForce = line.hasOption('f');

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("AIPManager\n", options);
            System.exit(0);
        }

        Context context = null;
        try
        {
            context = new Context();
            String disp = ConfigurationManager.getProperty("aipManager.dispatcher");
            if (disp != null)
                context.setDispatcher(disp);

            if (line.hasOption('e'))
            {
                String eperson = line.getOptionValue('e');
                EPerson myEPerson = EPerson.findByEmail(context, eperson);
                if (myEPerson == null)
                    usageError("Error, eperson cannot be found: " + eperson);
                context.setCurrentUser(myEPerson);
            }

            // the mutually-exclusive commands - let CLI handle checking.

            // update
            if (line.hasOption('u'))
            {
                //  update AIPs for _all_ objects in archive.
                if (line.hasOption('a'))
                {
                    int limit = -1;
                    if (line.hasOption('n'))
                        limit = Integer.parseInt(line.getOptionValue('n'));
                    int commCount = 0,
                        collCount = 0,
                        itemCount = 0;

                    // Communities
                    Community comms[] = Community.findAll(context);
                    for (int i = 0; limit != 0 && i < comms.length; ++i)
                    {
                        if (doUpdateAIP(context, comms[i], isForce, verbosity))
                        {
                            ++commCount;
                            if (limit > 0)
                                --limit;
                        }
                    }

                    // Collections
                    Collection colls[] = Collection.findAll(context);
                    for (int i = 0; limit != 0 && i < colls.length; ++i)
                    {
                        if (doUpdateAIP(context, colls[i], isForce, verbosity))
                        {
                            ++collCount;
                            if (limit > 0)
                                --limit;
                        }
                    }

                    // Items
                    ItemIterator ii = Item.findAll(context);
                    while (limit != 0 && ii.hasNext())
                    {
                        if (doUpdateAIP(context, ii.next(), isForce, verbosity))
                        {
                            ++itemCount;
                            if (limit > 0)
                                --limit;
                        }
                    }
                    if (verbosity > 0)
                        System.out.println("\nUpdated objects in archive:"+
                            "\n  Communities: "+String.valueOf(commCount)+
                            "\n  Collections: "+String.valueOf(collCount)+
                            "\n  Items: "+String.valueOf(itemCount));
                }
                else if (line.hasOption('i'))
                {
                    DSpaceObject dso = getIasDSO(context, line.getOptionValue('i'));
                    if (doUpdateAIP(context, dso, isForce, verbosity) && verbosity > 0)
                        System.out.println("Updated "+
                            Constants.typeText[dso.getType()]+" handle="+dso.getHandle()+".");
                     
                }
                else
                    usageError("MUST specify one of -a or -i with --update.");
            }

            // check AIP
            else if (line.hasOption('c'))
            {
                if (line.hasOption('b'))
                {
                    Bitstream bs = getValueOfB(context, line.getOptionValue('b'));
                    if (InternalAIP.probe(context, bs))
                        doCheckAIP(context, bs, isForce, verbosity);
                    else
                    {
                        System.out.println("Bitstream "+bs.toString()+" does not appear to be an Internal AIP.");
                        status = 1;
                    }
                }

                //  check AIPs for _all_ objects in archive.
                else if (line.hasOption('a'))
                {
                    Iterator bi = Bitstream.findAll(context);
                    while (bi.hasNext())
                    {
                        try
                        {
                            Bitstream bs = (Bitstream)bi.next();
                            if (InternalAIP.probe(context, bs))
                                doCheckAIP(context, bs, isForce, verbosity);
                            else if (verbosity > 2)
                                System.out.println("Skipping Non-AIP Bitstream ID="+String.valueOf(bs.getID()));
                        }
                        catch (Exception e)
                        {
                            log.error("Failed while checking AIP: ",e);
                            System.err.println(e+"\nStack trace follows:");
                            e.printStackTrace();
                            System.err.println(e+"\nContinuing...\n");
                        }
                    }
                }
                else
                    usageError("Usage error, must choose either --all or --bitstream option with --check.");
            }

            // probe checking for AIP on either stdin or specific bitstream:
            else if (line.hasOption('P'))
            {
                boolean result = false;
                if (line.hasOption('b'))
                {
                    Bitstream bs = getValueOfB(context, line.getOptionValue('b'));
                    result = InternalAIP.probe(context, bs);
                    System.out.println("Bitstream "+bs.toString()+(result ? " appears to be an internal AIP." : " is NOT a valid AIP manifest."));
                }
                else
                {
                    result = InternalAIP.probe(context, System.in);
                    System.out.println("(Standard input)"+(result ? " appears to be an internal AIP." : " is NOT a valid AIP manifest."));
                }
                status = result ? 0 : 1;
            }
             
            // restore AIP
            else if (line.hasOption('r'))
            {

                // sanity check
                if (!line.hasOption('e'))
                    usageError("The -e <eperson> option is required for restore.");

                if (line.hasOption('i'))
                {
                    // -p only required for a single-object restore
                    DSpaceObject parent = null;
                    if (line.hasOption('p'))
                    {
                        parent = HandleManager.resolveToObject(context, line.getOptionValue('p'));
                        if (parent == null)
                            usageError("No object found for handle="+line.getOptionValue('p'));
                    }
                    else
                        usageError("Missing required -p <defaultParentHandle> value.");
                     
                    InternalAIP ia = getIasInternalAIP(context, line.getOptionValue('i'));
                    doRestoreAIP(context, ia, parent, isForce, verbosity);
                }

                /* Restore AIPs for _all_ objects in archive.
                 * Keep iterating through a list of all AIPs, restoring the
                 * ones whose parents are present, until only the irreconcilable
                 * orphans are left (if any; ideally there should be none.)
                 */
                else if (line.hasOption('a'))
                {
                    List<InternalAIP> left = new LinkedList<InternalAIP>();
                    Set<String> exists = new HashSet<String>();
                    Map<InternalAIP,String> parentOf = new HashMap<InternalAIP,String>();
                    int limit = -1;
                    if (line.hasOption('n'))
                        limit = Integer.parseInt(line.getOptionValue('n'));

                    // first pass, restore children of Site (top-level communities)
                    log.debug("First pass, limit="+String.valueOf(limit));
                    String rootHdl = Site.getSiteHandle();
                    for (Iterator iai = InternalAIP.findAll(context);
                         limit != 0 && iai.hasNext();)
                    {
                        InternalAIP ia = (InternalAIP)iai.next();

                        // check if it exists already
                        String hdl = ia.getHandle();
                        DSpaceObject dso = HandleManager.resolveToObject(context, hdl);
                        if (dso != null)
                        {
                            exists.add(hdl);
                            if (log.isDebugEnabled())
                                log.debug("Skipping because Object already EXISTS: AIP="+String.valueOf(ia.getID())+", object="+hdl);
                            if (verbosity > 0)
                                System.err.println("Skipping because Object already EXISTS: AIP="+String.valueOf(ia.getID())+", object="+hdl);
                        }

                        // doesn't exist, try restoring if top-level.
                        else
                        {
                            String parent = ia.getParentHandle();
                            if (parent == null)
                                System.err.println("ERROR, could not get Parent handle from AIP="+String.valueOf(ia.getID())+", object="+hdl);
                            else if (parent.equals(rootHdl))
                            {
                                if (atomicRestoreAIP(context, ia, Site.find(context,0), isForce, verbosity))
                                {
                                    exists.add(hdl);
                                    if (log.isDebugEnabled())
                                        log.info("Restored: AIP="+String.valueOf(ia.getID())+", object="+hdl);
                                    if (limit > 0)
                                        --limit;
                                }
                                else
                                    log.warn("FAILED to Restore: AIP="+String.valueOf(ia.getID())+", object="+hdl);
                            }
                            else
                            {
                                left.add(ia);
                                parentOf.put(ia, parent);
                            }
                        }
                    }

                    // make more passes until nothing left to do.
                    log.debug("Second pass, limit="+String.valueOf(limit)+", left.size="+String.valueOf(left.size()));
                    boolean restoredOne = true;
                    while (limit != 0 && !left.isEmpty() && restoredOne)
                    {
                        restoredOne = false;
                        for (ListIterator<InternalAIP> iai = left.listIterator();
                             limit != 0 && iai.hasNext();)
                        {
                            InternalAIP ia = iai.next();
                            String parent = parentOf.get(ia);
                            if (exists.contains(parent))
                            {
                                DSpaceObject parentObj = HandleManager.resolveToObject(context, parent);
                                if (atomicRestoreAIP(context, ia, parentObj, isForce, verbosity))
                                {
                                    String hdl = ia.getHandle();
                                    log.info("Restored: AIP="+String.valueOf(ia.getID())+", object="+hdl);
                                    exists.add(hdl);
                                    restoredOne = true;
                                    if (limit > 0)
                                        --limit;
                                }
                                else
                                    log.warn("FAILED to Restore: AIP="+String.valueOf(ia.getID())+", object="+ia.getHandle());

                                iai.remove();
                            }
                        }
                    }

                    // report on any leftovers, unless there was a limit.
                    log.debug("Finished, limit="+String.valueOf(limit)+", left.size="+String.valueOf(left.size()));
                    if (!left.isEmpty())
                    {
                        if (limit >= 0)
                            System.err.println("Stopping because of limit, there are "+String.valueOf(left.size())+" AIPs left to restore.");
                        else
                        {
                            System.err.println("Could NOT restore the following AIPs:");
                            for (InternalAIP ia : left)
                            {
                                System.err.println("  AIP="+String.valueOf(ia.getID())+", object="+
                                        ia.getHandle());
                            }
                        }
                    }
                }
                else
                    usageError("Usage error, must choose either --all or --identifier option with --check.");
            }

            // query contents of InternalAIP for handle
            //  -i <hdl> is handle of DSO to look up AIP of..
            else if (line.hasOption('q'))
            {
                if (line.hasOption('i'))
                {
                    String hdl = line.getOptionValue('i');
                    InternalAIP ia = getIasInternalAIP(context, hdl);
                    System.out.println("Internal AIP for object="+hdl);
                    System.out.println("  ID="+String.valueOf(ia.getID()));
                    System.out.println("  Last Modified="+ia.getLastModified().toString());
                    System.out.println("  AIP Bitstream ID="+String.valueOf(ia.getAIP().getID()));
                    for (Bitstream mbs : ia.getMetadataBitstreams(context))
                    {
                        System.out.println("  ..Aux Metadata Bitstream ID="+String.valueOf(mbs.getID()));
                    }
                }
                else
                    usageError("Missing -i option with Handle of object.");
            }

            // delete an internal AIP record and its bitstreams.
            //  -i <internalAipID> is ID of internalAip record.
            else if (line.hasOption('D'))
            {
                if (line.hasOption('i'))
                {
                    String bn = line.getOptionValue('i');
                    InternalAIP ia = getIasInternalAIP(context, bn);
                    ia.delete();
                    System.out.println("Deleted InternalAIP ID="+bn);
                }
                else
                    usageError("Missing -i option with ID number of InternalAIP.");
            }

            // fetch contents of Bitstream URI
            else if (line.hasOption('b'))
            {
                Bitstream bs = getValueOfB(context, line.getOptionValue('b'));
                Utils.copy(bs.retrieve(), System.out);
            }

            else
                usageError("MUST specify one of --update, --check, --query, --delete, or --restore.");

            context.complete();
            context = null;
        }
        catch (Exception e)
        {
            log.error("Failed in AIPManager: ",e);
            e.printStackTrace();
            System.out.println(e);
            status = 1;
        }
        finally
        {
            if (context != null)
                context.abort();
        }
        System.exit(status);
    }

    // display verbose message
    private static void report(DSpaceObject dso)
    {
        System.out.println("Updated AIP for "+
          Constants.typeText[dso.getType()]+" handle="+dso.getHandle());
    }
}
