/*
 * AIPIngester.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/03/17 00:04:38 $
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

package org.dspace.content.packager;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.IngestionWrapper;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;


/**
 * Plugin Interface to interpret an Archival Information Package (AIP)
 * and create (or replace) a DSpace Object from its contents.
 * <p>
 * This subclass of the PackageIngester adds a few methods specific to
 * the AIP, required by the code that reads AIPs to restore the
 * DSpace Archive.  They are documented below.
 * <p>
 * The ingest methods are also given an attribute-value
 * list of "parameters"  which may modify their actions.
 * The parameters list is a generalized mechanism to pass parameters
 * from the requestor to the packager, since different packagers will
 * understand different sets of parameters.
 *
 * @author Larry Stone
 * @version $Revision: 1.1 $
 * @see PackageIngester
 * @see PackageParameters
 * @see IngestionWrapper
 */
public interface AIPIngester
    extends PackageIngester
{
    /**
     * Test an input stream to tell if it appears to be a package that
     * would be acceptable to this ingester.  If it returns true, the
     * next step will probably be to process the package as an AIP, so
     * this test should be thorough -- i.e. check the profile of a METS
     * document, not just that it is a METS document of some kind.
     *
     * @param context the usual
     * @param in input stream: NOTE, this will get read, so it MUST be a
     *        type of stream that can be re-opened or "rewound".
     * @param params packager parameters
     * @return true if the stream appears to contain a package acceptable
     *  to this ingester, false otherwise.  Should not throw any exceptions
     *  if it is actually implemented.
     */
    public boolean probe(Context context, InputStream in, PackageParameters params);

    /**
     * Extract the handle (persistent identifier) from an AIP stream,
     * if available.  The stream will be partially or entirely read.
     *
     * @param context  DSpace context.
     * @param parent collection under which to create new item.
     * @param in  input stream containing package to ingest.
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return ingestion wrapper (or workspaceitem, for Item) created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public String getHandle(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Extract the handle (persistent identifier) of the original parent
     * of the object from an AIP stream, if available.
     * The stream will be partially or entirely read.
     *
     * @param context  DSpace context.
     * @param parent collection under which to create new item.
     * @param in  input stream containing package to ingest.
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return ingestion wrapper (or workspaceitem, for Item) created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public String getParentHandle(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Get the date that the AIP was created from the package stream,
     * if it is available.
     * The stream will be partially or entirely read.
     *
     * @param context  DSpace context.
     * @param parent collection under which to create new item.
     * @param in  input stream containing package to ingest.
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return ingestion wrapper (or workspaceitem, for Item) created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public Date getCreateDate(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Get all bitstreams holding external metadata for this
     * package.  Only significant for "internal" AIP.
     * The stream will be partially or entirely read.
     */
    public Bitstream[] getMetadataBitstreams(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;
}
