/*
 * RebuildBitstreamTable.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/01/20 16:13:19 $
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

import org.dspace.core.Context;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Logger;

/**
 * Administrative command-line driver for the BitstreamStorageManager,
 * gives access to function that rebuilds its RDBMS tables from an online
 * asset store.  This is part of the process of rebuilding a DSpace
 * Archive after catastrophic failure or loss of the RDBMS.  It has
 * no other use.
 *
 * @author Larry Stone
 * @version $Revision: 1.6 $
 */
public class RebuildBitstreamTable
{
    /** log4j log */
    private static Logger log = Logger.getLogger(RebuildBitstreamTable.class);

    public static void main(String[] argv)
    {
        Context context = null;
        try
        {
            log.info("Checking/Restoring the asset store");

            // set up command line parser
            CommandLineParser parser = new PosixParser();
            CommandLine line = null;

            // create an options object and populate it
            Options options = new Options();

            options.addOption("v", "verbose", false, "Verbose");
            options.addOption("r", "restore", false, "Restore (create) new Bitstream table entries for orphaned assetstore files");
            options.addOption("h", "help", false, "Help");

            try
            {
                line = parser.parse(options, argv);
            }
            catch (ParseException e)
            {
                log.fatal(e);
                System.exit(1);
            }

            // user asks for help
            if (line.hasOption('h'))
            {
                printHelp(options);
                System.exit(0);
            }

            context = new Context();
            BitstreamStorageManager.checkAssetStores(context,
                                                     line.hasOption('r'),
                                                     line.hasOption('v'),
                                                     System.err);
            context.complete();
            context = null;
            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.err.println("Caught exception:"+e.toString());
            e.printStackTrace();
        }
        finally
        {
            if (context != null)
                context.abort();
        }
        System.exit(1);
    }

    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("RebuildBitstreamTable\n", options);
    }

}
