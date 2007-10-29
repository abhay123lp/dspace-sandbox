/*
 * Packager.java
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

package org.dspace.app.packager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.IngestionWrapper;
import org.dspace.content.InstallItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageIngester;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.WorkflowItem;

/**
 * Command-line interface to the Packager plugin.
 * <p>
 * This class ONLY exists to provide a CLI for the packager plugins. It does not
 * "manage" the plugins and it is not called from within DSpace, but the name
 * follows a DSpace convention.
 * <p>
 * It can invoke one of the Submission (SIP) packagers to create a new DSpace
 * Item out of a package, or a Dissemination (DIP) packager to write an Item out
 * as a package.
 * <p>
 * Usage is as follows:<br>
 * (Add the -h option to get the command to show its own help)
 * 
 * <pre>
 *  1. To submit a SIP:
 *   java org.dspace.app.packager.Packager
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -p {parent-handle} [ -p {parent2} ...]
 *       -o {name}={value} [ -o {name}={value} ..]
 *       [-r]   --- use ONLY to restore an AIP
 *       [-w]   --- skip Workflow
 *       {package-filename}
 * 
 *   {PackagerType} must match one of the aliases of the chosen Packager
 *   plugin.
 * 
 *   The &quot;-w&quot; option circumvents Workflow, and is optional.  The &quot;-o&quot;
 *   option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 * 
 *  2. To write out a DIP:
 *   java org.dspace.content.packager.Packager
 *       -d
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -i {identifier-handle-of-object}
 *       -o {name}={value} [ -o {name}={value} ..]
 *       {package-filename}
 * 
 *   The &quot;-d&quot; switch chooses a Dissemination packager, and is required.
 *   The &quot;-o&quot; option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 * </pre>
 * 
 * Note that {package-filename} may be "-" for standard input or standard
 * output, respectively.
 * 
 * @author Larry Stone
 * @version $Revision$
 */
public class Packager
{
    // die from illegal command line
    private static void usageError(String msg)
    {
        System.out.println(msg);
        System.out.println(" (run with -h flag for details)");
        System.exit(1);
    }

    public static void main(String[] argv) throws Exception
    {
        Options options = new Options();
        options.addOption("p", "parent", true,
                "Handle(s) of parent Community or Collection into which to ingest object (repeatable)");
        options.addOption("e", "eperson", true,
                "email address of eperson doing importing");
        options
                .addOption(
                        "w",
                        "install",
                        false,
                        "disable workflow; install immediately without going through collection's workflow");
        options.addOption("r", "replace", false, "ingest in \"replacment\" mode, e.g. for AIP");
        options.addOption("t", "type", true, "package type or MIMEtype");
        options
                .addOption("o", "option", true,
                        "Packager option to pass to plugin, \"name=value\" (repeatable)");
        options.addOption("d", "disseminate", false,
                "Disseminate package (output); default is to submit.");
        options.addOption("s", "submit", false,
                "Submission package (Input); this is the default. ");
        options.addOption("i", "item", true, "Handle of item to disseminate.");
        options.addOption("h", "help", false, "help");

        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, argv);

        String sourceFile = null;
        String eperson = null;
        String[] parents = null;
        boolean useWorkflow = true;
        boolean replaceMode = false;
        String packageType = null;
        boolean submit = true;
        String itemHandle = null;
        PackageParameters pkgParams = new PackageParameters();

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("Packager  [options]  package-file|-\n",
                    options);
            System.out.println("\nAvailable Submission Package (SIP) types:");
            String pn[] = PluginManager
                    .getAllPluginNames(PackageIngester.class);
            for (int i = 0; i < pn.length; ++i)
                System.out.println("  " + pn[i]);
            System.out
                    .println("\nAvailable Dissemination Package (DIP) types:");
            pn = PluginManager.getAllPluginNames(PackageDisseminator.class);
            for (int i = 0; i < pn.length; ++i)
                System.out.println("  " + pn[i]);
            System.exit(0);
        }
        if (line.hasOption('w'))
            useWorkflow = false;
        if (line.hasOption('r'))
            replaceMode = true;
        if (line.hasOption('e'))
            eperson = line.getOptionValue('e');
        if (line.hasOption('p'))
            parents = line.getOptionValues('p');
        if (line.hasOption('t'))
            packageType = line.getOptionValue('t');
        if (line.hasOption('i'))
            itemHandle = line.getOptionValue('i');
        String files[] = line.getArgs();
        if (files.length > 0)
            sourceFile = files[0];
        if (line.hasOption('d'))
            submit = false;
        if (line.hasOption('o'))
        {
            String popt[] = line.getOptionValues('o');
            for (int i = 0; i < popt.length; ++i)
            {
                String pair[] = popt[i].split("\\=", 2);
                if (pair.length == 2)
                    pkgParams.addProperty(pair[0].trim(), pair[1].trim());
                else if (pair.length == 1)
                    pkgParams.addProperty(pair[0].trim(), "");
                else
                    System.err
                            .println("Warning: Illegal package option format: \""
                                    + popt[i] + "\"");
            }
        }

        // Sanity checks on arg list: required args
        if (sourceFile == null || eperson == null || packageType == null
                || (submit && parents == null))
        {
            System.err
                    .println("Error - missing a REQUIRED argument or option.\n");
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("PackageManager  [options]  package-file|-\n",
                    options);
            System.exit(0);
        }

        // find the EPerson, assign to context
        Context context = new Context();
        EPerson myEPerson = null;
        myEPerson = EPerson.findByEmail(context, eperson);
        if (myEPerson == null)
            usageError("Error, eperson cannot be found: " + eperson);
        context.setCurrentUser(myEPerson);

        if (submit)
        {
            // make sure we have an input file
            InputStream source = (sourceFile.equals("-")) ? System.in
                    : new FileInputStream(sourceFile);

            PackageIngester sip = (PackageIngester) PluginManager
                    .getNamedPlugin(PackageIngester.class, packageType);
            if (sip == null)
                usageError("Error, Unknown package type: " + packageType);

            System.out.println("Destination parents:");

            // validate each parent arg
            DSpaceObject parentObjs[] = new DSpaceObject[parents.length];
            for (int i = 0; i < parents.length; i++)
            {
                // sanity check: did handle resolve?
                parentObjs[i] = HandleManager.resolveToObject(context,
                        parents[i]);
                if (parentObjs[i] == null)
                    throw new IllegalArgumentException(
                            "Bad parent list -- "
                                    + "Cannot resolve parent handle \""
                                    + parents[i] + "\"");
                System.out.println((i == 0 ? "Owner: " : "Parent: ")
                        + parentObjs[i].getHandle());
            }

            try
            {
                IngestionWrapper iw = sip.ingest(context, parentObjs[0],
                        source, pkgParams, null);

                if (iw.getType() == Constants.INGESTION_ITEM)
                {
                    WorkspaceItem wi = (WorkspaceItem)iw;

                    // replace existing package, if necessary (e.g. ingest AIP)
                    if (replaceMode)
                    {
                        System.err.println("Installing item with Package handle="+wi.getHandle());
                        InstallItem.replaceItem(context, wi, wi.getHandle());
                        // get new copy of item to reread from RDBMS:
                        Item item = wi.getItem();
                        if (wi.getWithdrawn())
                        {
                            System.err.println("Marking item Withdrawn.");
                            item.withdraw();
                        }
                        System.out.println("Created and installed item, handle="+
                                HandleManager.findHandle(context, item));
                    }

                    // submit normally, passing along to workflow
                    else if (useWorkflow)
                {
                    String handle = null;

                    // Check if workflow completes immediately, and
                    // return Handle if so.
                    WorkflowItem wfi = WorkflowManager.startWithoutNotify(context, wi);

                    if (wfi.getState() == WorkflowManager.WFSTATE_ARCHIVE)
                    {
                        Item ni = wfi.getItem();
                        handle = HandleManager.findHandle(context, ni);
                    }
                    if (handle == null)
                    System.out.println("Created Workflow item, ID="
                                + String.valueOf(wfi.getID()));
                    else
                        System.out.println("Created and installed item, handle="+handle);
                }

                    // skip workflow, but otherwise normal submission
                else
                {
                        System.err.println("Installing item with handle="+wi.getHandle());
                        InstallItem.installItem(context, wi, wi.getHandle());
                        // get new copy of item to reread from RDBMS:
                        Item item = wi.getItem();
                        if (wi.getWithdrawn())
                        {
                            System.err.println("Marking item Withdrawn.");
                            item.withdraw();
                        }
                    System.out.println("Created and installed item, handle="
                                + HandleManager.findHandle(context, item));
                    }
                }
                context.complete();
                System.exit(0);
            }
            catch (Exception e)
            {
                // abort all operations
            	e.printStackTrace();
            	context.abort();
                System.out.println(e);
                System.exit(1);
            }
        }
        else
        {
            OutputStream dest = (sourceFile.equals("-")) ? (OutputStream) System.out
                    : (OutputStream) (new FileOutputStream(sourceFile));

            PackageDisseminator dip = (PackageDisseminator) PluginManager
                    .getNamedPlugin(PackageDisseminator.class, packageType);
            if (dip == null)
                usageError("Error, Unknown package type: " + packageType);

            DSpaceObject dso = HandleManager.resolveToObject(context, itemHandle);
            if (dso == null)
                throw new IllegalArgumentException("Bad Item handle -- "
                		+ "Cannot resolve handle \"" + itemHandle +"\"");
            dip.disseminate(context, dso, pkgParams, dest);
        }
        System.exit(0);
    }
}
