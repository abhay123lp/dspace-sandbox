/*
 * AbstractMETSIngester
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

package org.dspace.content.packager;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.MediaFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.IngestionWrapper;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.jdom.Element;

/**
 * Base class for package ingester of METS (Metadata Encoding & Transmission
 * Standard) Package.<br>
 * See <a
 * href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * ingesters for more specific METS "profiles". METS is an abstract and flexible
 * framework that can encompass many different kinds of metadata and inner
 * package structures.
 * 
 * <p>
 * <b>Parameters:</b> 1. "validate" -- true/false attempt to schema-validate
 * the METS manifest. 2. "manifestOnly" -- package consists only of a manifest
 * document. "internal" -- like ManifestOnly except refs are to bitstreams in
 * assetstore. 3. "ignoreHandle" -- true/false, ignore AIP's idea of handle when
 * ingesting. 4. "ignoreParent" -- true/false, ignore AIP's idea of parent when
 * ingesting.
 * 
 * <b>Configuration Properties:</b>
 * 
 * 1. <code>mets.CONFIGNAME.ingest.preserveManifest</code> - if <em>true</em>,
 * the METS manifest itself is preserved in a bitstream named
 * <code>mets.xml</code> in the <code>METADATA</code> bundle. If it is
 * <em>false</em> (the default), the manifest is discarded after ingestion.
 * 
 * 2. <code>mets.CONFIGNAME.ingest.manifestBitstreamFormat</code> - short name
 * of the bitstream format to apply to the manifest; MUST be specified when
 * preserveManifest is true.
 * 
 * 3. <code>mets.default.ingest.crosswalk.MD_SEC_NAME</code> = PLUGIN_NAME
 * Establishes a default crosswalk plugin for the given type of metadata in a
 * METS mdSec (e.g. "DC", "MODS"). The plugin may be either a stream or
 * XML-oriented ingestion crosswalk. Subclasses can override the default mapping
 * with their own, substituting their configurationName for "default" in the
 * configuration property key above.
 * 
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.METSManifest
 */
public abstract class AbstractMETSIngester
       implements PackageIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractMETSIngester.class);

    /**
     * An instance of MdrefManager holds the state needed to
     * retrieve the contents (or bitstream corresponding to) an
     * external metadata stream referenced by an <code>mdRef</code>
     * element in the METS manifest.
     * <p>
     * Initialize it with the Content (ORIGINAL) Bundle containing all of the
     * metadata bitstreams.  Match an mdRef by finding the bitstream
     * with the same name.
     */
    protected class MdrefManager
        implements METSManifest.Mdref
    {
        private Map packageFiles = null;
        private Context context;
        private PackageParameters params;

        // constructor initializes metadata map (from package)
        private MdrefManager(Context context,
                             Map packageFiles,
                             PackageParameters params)
        {
            super();
            this.context = context;
            this.packageFiles = packageFiles;
            this.params = params;
        }

        /**
         * Find the local Bitstream referenced in
         * an <code>mdRef</code> element.
         * @param mdref the METS mdRef element to locate the bitstream for.
         * @return bitstream or null if none found.
         */
        public Bitstream getBitstreamForMdRef(Element mdref)
            throws MetadataValidationException, PackageValidationException,
                   IOException, SQLException, AuthorizeException
        {
            String path = METSManifest.getFileName(mdref);
            if (packageFiles == null)
                throw new MetadataValidationException("Failed referencing mdRef element, because there is no map of package files.");
            return resolveBitstream(context, path, mdref, packageFiles, params);
        }
         
        /**
         * Make the contents of an external resource mentioned in
         * an <code>mdRef</code> element available as an <code>InputStream</code>.
         * See the <code>METSManifest.MdRef</code> interface for details.
         * @param mdref the METS mdRef element to locate the input for.
         * @return the input stream of its content.
         */
        public InputStream getInputStream(Element mdref)
            throws MetadataValidationException, PackageValidationException,
                   IOException, SQLException, AuthorizeException
        {
            Bitstream mdbs = getBitstreamForMdRef(mdref);
            if (mdbs == null)
                throw new MetadataValidationException("Failed dereferencing bitstream for mdRef element="+mdref.toString());
            return mdbs.retrieve();
        }
    }

    /**
     * Create a new DSpace object out of a METS content package.
     * All contents are dictated by the METS manifest.
     * Package is a ZIP archive (or optionally bare manifest XML document).
     * In a Zip, all files relative to top level
     * and the manifest (as per spec) in mets.xml.
     *
     * @param context - DSpace context.
     * @param collection - collection under which to create new item.
     * @param pkg - input stream containing package to ingest.
     * @param license - may be null, which takes default license.
     * @return workspace item created by ingest.
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public IngestionWrapper ingest(Context context, DSpaceObject parent,
                                InputStream pkg, PackageParameters params,
                                String license)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        DSpaceObject dso = null;
        boolean success = false;
        HashSet packageFiles = new HashSet();
        Bitstream manifestBitstream = null;
        IngestionWrapper result = null;
        boolean validate = params.getBooleanProperty("validate", true);
        METSManifest manifest;

        // map of bitstream name -> bitstream object; delete any unused ones.
        HashMap deleteMe = new HashMap();

        try
        {
            int type;
            MdrefManager callback;
            try
            {
                // need to bypass privilege checks to
                // create and manipulate "unattached" bitstreams
                context.setIgnoreAuthorization(true);

                // 1. read "package" stream:  it will be either bare Manifest
                // or Package contents into bitstreams, depending on params:
                if (params.getBooleanProperty("manifestOnly", false) ||
                    params.getBooleanProperty("internal", false))
                {
                    manifestBitstream = Bitstream.create(context, pkg);
                    manifestBitstream.setName(METSManifest.MANIFEST_FILE);
                    manifestBitstream.setSource(METSManifest.MANIFEST_FILE);
                    deleteMe.put(METSManifest.MANIFEST_FILE, manifestBitstream);
                    manifestBitstream.update();
                 
                    if (log.isDebugEnabled())
                        log.debug("Got standalone manifest, len="+String.valueOf(manifestBitstream.getSize()));
                }
                else
                {
                    ZipInputStream zip = new ZipInputStream(pkg);
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null)
            {
                if (ze.isDirectory())
                    continue;
                        Bitstream bs = Bitstream.create(context, new PackageUtils.UnclosableInputStream(zip));
                String fname = ze.getName();
                        if (fname.equals(METSManifest.MANIFEST_FILE))
                            manifestBitstream = bs;
                        deleteMe.put(fname, bs);
                        bs.setSource(fname);
                        bs.setName(fname);
                        AuthorizeManager.addPolicy(context, bs, Constants.READ, context.getCurrentUser());
                        bs.update();
                        packageFiles.add(fname);
                    }
                    zip.close();
                    if (log.isDebugEnabled())
                        log.debug("Got Zip, manifestBitstream="+manifestBitstream);
                    }
                }
            finally
                {
                context.setIgnoreAuthorization(false);
                }

            // 2. parse the manifest and sanity-check it.
            if (manifestBitstream == null)
                throw new PackageValidationException("No METS Manifest found (filename="+METSManifest.MANIFEST_FILE+").  Package is unacceptable.");
            manifest = METSManifest.create(manifestBitstream.retrieve(),
                         validate, getConfigurationName());

            // 3. Extract object type and instantiate the new object:
            type = getObjectType(manifest);
            callback = new MdrefManager(context, deleteMe, params);
            result = IngestionWrapper.create(type);

            // 4. crosswalk sourceMD first, for the object to fill in
            //     parent, submitter, handle, etc if applicable: (as for AIP)
            // ..but set a default parent if there is no sourceMD.
            if (!manifest.crosswalkObjectSourceMD(context, result, callback))
                result.setParent(parent);

            checkManifest(manifest);

            if (log.isDebugEnabled())
            {
                String h = result.getHandle();
                String p = (result.getParent() == null) ? null :
                           result.getParent().getHandle();
                log.debug("After object TechMD: parent="+(p == null?"null":p)+
                        ", hdl="+(h == null?"null":h));
            }

            // 5. create shell of object -- use parent and handle from
            //    TechMD above, if any, and if options don't say to ignore them:
            if (params.getBooleanProperty("ignoreHandle", false))
                result.setHandle(null);
            // check that package included a resolvable parent
            if (params.getBooleanProperty("ignoreParent", false))
                result.setParent(parent);
            else if (result.getParent() == null)
                throw new PackageValidationException("The parent object required to ingest this package could not be resolved.  Check manifest for details.");
            result = result.createWrappedObject(context);
            dso = result.getWrappedObject();

            // 4. crosswalk techMD, digiprovMD, rightsMD now that there
            //     is a target object.
            manifest.crosswalkObjectOtherAdminMD(context, dso, callback);

            // 6. Object-type-specific logic: Ingesting an ITEM
            if (type == Constants.ITEM)
            {
                // XXX FIXME: maybe add an option to apply template Item on ingest

                Item item = (Item)dso;
                Bundle contentBundle = item.createBundle(Constants.CONTENT_BUNDLE_NAME);
                Bundle mdBundle = null;

                // save manifest bitstream in Item if desired, give it
                // "magic" bitstream format to find it later for dissemination.
                if (preserveManifest())
                {
                    mdBundle = item.createBundle(Constants.METADATA_BUNDLE_NAME);
                    mdBundle.addBitstream(manifestBitstream);

                    // Get magic bitstream format to identify manifest.
                    String fmtName = getManifestBitstreamFormat();
                    if (fmtName == null)
                        throw new PackageValidationException("Configuration Error: No Manifest BitstreamFormat configured for METS ingester type="+getConfigurationName());
                    BitstreamFormat manifestFormat = PackageUtils.findOrCreateBitstreamFormat(context,
                         fmtName, "application/xml",
                         fmtName+" package manifest");
                    manifestBitstream.setFormat(manifestFormat);
                    manifestBitstream.update();
                    deleteMe.remove(METSManifest.MANIFEST_FILE);
                }

                // -------- Acquire all the bitstreams
                /*  Grovel a content-file list out of METS Manifest --
                 *  - check or acquire a bitstream for each one
                 *  - crosswalk any bitstream AMD and cleanup
                 *  - put each bitstream in the appropriate bundle
                 *  - mark Item's primary bitstream
             */
                String pbsID = null;
                Element pbsFile = manifest.getPrimaryOrLogoBitstream();
                if (pbsFile != null)
                {
                    pbsID = pbsFile.getAttributeValue("ID");
                    if (log.isDebugEnabled())
                        log.debug("Got primary bitstream file ID=\""+pbsID+"\"");
                }
                 
            List manifestContentFiles = manifest.getContentFiles();
                HashSet missingFiles = new HashSet();

            // Compare manifest files with the ones found in package:
            //  a. Start with content files (mentioned in <fileGrp>s)
                boolean setPBS = false;
                BitstreamFormat unknownFormat = BitstreamFormat.findUnknown(context);
            for (Iterator mi = manifestContentFiles.iterator(); mi.hasNext(); )
            {
                Element mfile = (Element)mi.next();
                    String mfileID = mfile.getAttributeValue("ID");
                    if (mfileID == null)
                    throw new PackageValidationException("Invalid METS Manifest: file element without ID attribute.");
                String path = METSManifest.getFileName(mfile);
                    Bitstream bs = resolveBitstream(context, path, mfile,
                            deleteMe, params);
                if (bs == null)
                {
                    log.warn("Cannot find bitstream for filename=\""+path+
                             "\", skipping it..may cause problems later.");
                    missingFiles.add(path);
                }
                else
                {
                        // build compare lists by deleting matches.
                        if (packageFiles.contains(path))
                            packageFiles.remove(path);

                        //  attach bitstream to correct bundle.
                    String bundleName = manifest.getBundleName(mfile);
                        Bundle bn;
                        Bundle bns[] = item.getBundles(bundleName);
                        if (bns != null && bns.length > 0)
                            bn = bns[0];
                        else
                            bn = item.createBundle(bundleName);
                        bn.addBitstream(bs);

                        // crosswalk bitstream's AMD
                        manifest.crosswalkBitstream(context, bs, mfileID, callback);

                        // is this the primary bitstream?
                        if (pbsID != null && mfileID.equals(pbsID))
                        {
                            bn.setPrimaryBitstreamID(bs.getID());
                            bn.update();
                            setPBS = true;
                    }

                        // Some optional subclass business to second-guess
                        // values for details of bitstream:
                        finishBitstream(context, bs, mfile, manifest, params);

                        /**
                         * Last-ditch attempt to divine the format, if crosswalk failed to set it:
                         *    1. attempt to guess from MIME type
                         *    2. if that fails, guess from "name" extension.
                         */
                        if (bs.getFormat().equals(unknownFormat))
                        {
                            if (log.isDebugEnabled())
                                log.debug("Guessing format of Bitstream left un-set: "+bs.toString());
                            String mimeType = mfile.getAttributeValue("MIMETYPE");
                            BitstreamFormat bf = (mimeType == null) ? null :
                                    BitstreamFormat.findByMIMEType(context, mimeType);
                            if (bf == null)
                                bf = FormatIdentifier.guessFormat(context, bs);
                            bs.setFormat(bf);
                        }
                        bs.update();
                }
            }

                // sanity check for primary bitstream
                if (pbsID != null && !setPBS)
                    log.warn("Could not find primary bitstream file ID=\""+pbsID+"\"");

                //  b. Account for package members mentioned in <mdRef>s --
                //     just correct the file lists, don't crosswalk yet.
            for (Iterator mi = manifest.getMdFiles().iterator(); mi.hasNext(); )
            {
                Element mdref = (Element)mi.next();
                String path = METSManifest.getFileName(mdref);

                if (packageFiles.contains(path))
                    packageFiles.remove(path);
                else
                    missingFiles.add(path);
            }

                // -------- Sanity checks of bitstreams vs. manifest
                 
            // KLUDGE: make sure Manifest file doesn't get flagged as missing
            // or extra, since it won't be mentioned in the manifest.
                if (packageFiles.contains(METSManifest.MANIFEST_FILE))
                    packageFiles.remove(METSManifest.MANIFEST_FILE);

            // Give subclass a chance to refine the lists of in-package
            // and missing files, delete extraneous files, etc.
                checkPackageFiles(packageFiles, missingFiles, manifest, params);

            // Any discrepency in file lists is a fatal error:
            if (!(packageFiles.isEmpty() && missingFiles.isEmpty()))
            {
                StringBuffer msg = new StringBuffer("Package is unacceptable: contents do not match manifest.");
                if (!missingFiles.isEmpty())
                {
                    msg.append("\nPackage is missing these files listed in Manifest:");
                    for (Iterator mi = missingFiles.iterator(); mi.hasNext(); )
                        msg.append("\n\t"+(String)mi.next());
                }
                if (!packageFiles.isEmpty())
                {
                    msg.append("\nPackage contains extra files NOT in manifest:");
                    for (Iterator mi = packageFiles.iterator(); mi.hasNext(); )
                        msg.append("\n\t"+(String)mi.next());
                }
                throw new PackageValidationException(msg.toString());
            }

                // have subclass manage license since it may be extra package file.
                addLicense(context, item, license, (Collection)result.getParent(), params);

                // XXX FIXME
                // should set lastModifiedTime e.g. when ingesting AIP.
                // maybe only do it in the finishObject() callback for AIP.

            } // if ITEM
            else if (type == Constants.COLLECTION ||
                     type == Constants.COMMUNITY)
            {
                Element logoFile = manifest.getPrimaryOrLogoBitstream();
                if (logoFile != null)
                {
                    String logoID = logoFile.getAttributeValue("ID");
                    if (log.isDebugEnabled())
                        log.debug("Got logo bitstream file ID=\""+logoID+"\"");

                    for (Iterator mi = manifest.getContentFiles().iterator(); mi.hasNext(); )
            {
                Element mfile = (Element)mi.next();
                        if (logoID.equals(mfile.getAttributeValue("ID")))
                {
                            String path = METSManifest.getFileName(mfile);
                            Bitstream bs = resolveBitstream(context, path, mfile,
                                deleteMe, params);
                            if (bs == null)
                    {
                                log.error("Cannot find bitstream for filename=\""+path+
                                         "\", skipping it..may cause problems later.");
                                throw new PackageValidationException("Cannot resolve bitstream for logo, from file ID="+logoID);
            }
                else
                {
                                // build compare lists by deleting matches.
                                if (packageFiles.contains(path))
                                    packageFiles.remove(path);
                                if (dso.getType() == Constants.COLLECTION)
                                    ((Collection)dso).setLogo(bs.retrieve());
                    else
                                    ((Community)dso).setLogo(bs.retrieve());
                            }
                            break;
                        }
                    }
                }
            }
            else
                throw new PackageValidationException("Unknown DSpace Object type in package, type="+String.valueOf(type));

            /* 7. Crosswalk the DMD for the object;
             *    if an Item, also sanity-check the metadata for minimum reqs.
             */
            crosswalkObjectDmd(context, dso, manifest, callback, manifest.getItemDmds());

            if (type == Constants.ITEM)
                PackageUtils.checkMetadata((Item)dso);

            // 8. subclass hook for final checks and rearrangements
            finishObject(context, dso);

            // this also updates the wrapped object.
            if (result != null)
                result.update();
            success = true;
            log.info(LogManager.getHeader(context, "ingest",
                "Created new Object, type="+String.valueOf(dso.getType())+
                ", dbID="+String.valueOf(dso.getID())));
            return result;
        }
        catch (SQLException se)
        {
            // disable attempt to delete the workspace object, since
            // database may have suffered a fatal error and the
            // transaction rollback will get rid of it anyway.
            result = null;

            // Pass this exception on to the next handler.
            throw se;
        }
        finally
        {
            // get rid of any leftover package files
            for (Iterator bi = deleteMe.keySet().iterator();
                 bi.hasNext();)
            {
                String fname = (String)bi.next();
                Bitstream bs = (Bitstream)deleteMe.get(fname);
                if (log.isDebugEnabled())
                    log.debug("Queueing leftover bitstream named \""+fname+"\" for deletion.");
                if (result != null)
                    result.addObjectToDelete(bs);
            }
            deleteMe.clear();

            if (result != null)
            {
            // kill item (which also deletes bundles, bitstreams) if ingest fails
                if (!success)
                    result.deleteAll();

                // cleanup unneeded registered objects.
                else
                    result.cleanup();
            }
        }
    }

    /**
     * XXX FIXME Replace is not implemented yet.
     */
    public DSpaceObject replace(Context context, DSpaceObject dso,
                     InputStream in, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        throw new UnsupportedOperationException("The replace operation is not implemented.");
    }

    // whether or not to save manifest as a bitstream in METADATA bndl.
    private boolean preserveManifest()
    {
        return ConfigurationManager.getBooleanProperty("mets."+
            getConfigurationName()+".ingest.preserveManifest", false);
    }

    // return short name of manifest bitstream format
    private String getManifestBitstreamFormat()
        {
        return ConfigurationManager.getProperty("mets."+
            getConfigurationName()+".ingest.manifestBitstreamFormat");
    }

    /**
     * Profile-specific tests to validate manifest.  The implementation
     * can access the METS document through the <code>manifest</code>
     * variable, an instance of <code>METSManifest</code>.
     * @throws MetadataValidationException if there is a fatal problem with the METS document's conformance to the expected profile.
     */
    abstract void checkManifest(METSManifest manifest)
        throws MetadataValidationException;

    /**
     * Hook for subclass to modify the test of the package's
     * integrity, and add other tests. E.g. evaluate a PGP signature of
     * the manifest in a separate file.
     * <p>
     * The <code>packageFiles</code> contains "extra" files that were in
     * the package but were not referenced by the METS manifest (either as
     * content or metadata (mdRefs)).
     * The implementation of this method should look for any "extra" files
     * uses (e.g. a checksum or cryptographic signature for the manifest
     * itself) and remove them from the Set.
     * <p>
     * The <code>missingFiles</code> set is for
     * any files
     * referenced by the manifest but not found in the package.
     * The implementation can check it for "false positives", or add
     * other missing files it knows of.
     * <p>
     * If either  of the Sets <code>missingFiles</code>
     * or <code>packageFiles</code>
     * is not empty, the ingest will fail.
     *
     * @param packageFiles files in package but not referenced by METS
     * @param missingFiles files referenced by manifest but not in package
     *
     */
    abstract public void checkPackageFiles(Set packageFiles,
                                           Set missingFiles,
                                           METSManifest manifest,
                                           PackageParameters params)
        throws PackageValidationException, CrosswalkException;

    /**
     * Select the <code>dmdSec</code> element(s) to apply to the
     * Item.  The implementation is responsible for choosing which
     * (if any) of the metadata sections to crosswalk to get the
     * descriptive metadata for the item being ingested.  It is
     * responsible for calling the crosswalk, using the manifest's helper
     * i.e. <code>manifest.crosswalkItemDmd(context,item,dmdElement,callback);</code>
     * (The final argument is a reference to itself since the
     * class also implements the <code>METSManifest.MdRef</code> interface
     * to fetch package files referenced by mdRef elements.)
     * <p>
     * Note that <code>item</code> and <code>manifest</code> are available
     * as protected fields from the superclass.
     *
     * @param context the DSpace context
     * @param dmds array of Elements, each a METS dmdSec that applies to the Item as a whole.
     *
     */
    abstract public void crosswalkObjectDmd(Context context, DSpaceObject dso,
                                       METSManifest manifest, MdrefManager cb,
                                       Element dmds[])
        throws CrosswalkException, PackageValidationException,
               AuthorizeException, SQLException, IOException;

    /**
     * Add license(s) to Item based on contents of METS and other policies.
     * The implementation of this method controls exactly what licenses
     * are added to the new item, including the DSpace deposit license.
     * It is given the collection (which is the source of a default deposit
     * license), an optional user-supplied deposit license (in the form of
     * a String), and the METS manifest.  It should invoke
     * <code>manifest.getItemRightsMD()</code> to get an array of
     * <code>rightsMd</code> elements which might contain other license
     * information of interest, e.g. a Creative Commons license.
     * <p>
     * This framework does not add any licenses by default.
     * <p>
     * Note that crosswalking rightsMD sections can also add a deposit or CC
     * license to the object.
     *
     * @param context the DSpace context
     * @param collection DSpace Collection to which the item is being submitted.
     * @param license optional user-supplied Deposit License text (may be null)
     */
    abstract public void addLicense(Context context, Item item, String license,
                                    Collection collection, PackageParameters params)
        throws PackageValidationException,
               AuthorizeException, SQLException, IOException;

    /**
     * Hook for final "finishing" operations on the new Item.
     * This method is called when the new Item is otherwise complete and
     * ready to be returned.  The implementation should use this
     * opportunity to make whatever final checks and modifications are
     * necessary.
     *
     * @param context the DSpace context
     */
    abstract public void finishObject(Context context, DSpaceObject dso)
        throws PackageValidationException, CrosswalkException,
         AuthorizeException, SQLException, IOException;

    /**
     * Determines what type of DSpace object is represented in this METS doc.
     * @returns one of the object types in Constants.
     */
    abstract public int getObjectType(METSManifest manifest)
        throws PackageValidationException;

    /**
     * Find the Bitstream corresponding to the given pathname relative
     * to the package.  If it identifies a member of the packageFiles
     * map, the implementation should delete it to mark it as processed.
     * @return Bitstream object, never null.
     */
    abstract public Bitstream resolveBitstream(Context context,
                                                String path,
                                                Element mfile,
                                                Map packageFiles,
                                                PackageParameters params)
        throws SQLException, PackageValidationException;

    /**
     * Subclass-dependent final processing on a Bitstream; could include
     * fixing up the name, bundle, other attributes.
     */
    abstract public void finishBitstream(Context context,
                                                Bitstream bs,
                                                Element mfile,
                                                METSManifest manifest,
                                                PackageParameters params)
        throws MetadataValidationException, SQLException, AuthorizeException, IOException;


    /**
     * Returns keyword that makes the configuration keys of this subclass
     * unique, e.g. if it returns NAME, they key would be:
     *    "mets.ingest.NAME.preserveManifest = true"
     */
    abstract public String getConfigurationName();
}
