/*
 * DSpaceMETSIngester
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
import java.util.Set;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.PluginManager;
import org.dspace.license.CreativeCommons;
import org.dspace.app.mediafilter.MediaFilter;

import org.jdom.Element;

/**
 * Packager plugin to ingest a
 * METS (Metadata Encoding & Transmission Standard) package
 * that conforms to the DSpace METS SIP (Submission Information Package) Profile.
 * See <a href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * for more information on METS, and
 * <a href="http://www.dspace.org/standards/METS/SIP/profilev0p9p1/metssipv0p9p1.pdf">
 * http://www.dspace.org/standards/METS/SIP/profilev0p9p1/metssipv0p9p1.pdf</a>
 * (or a similar file in the /standards/METS/SIP resource hierarchy)
 * for more information about the DSpace METS SIP profile.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.METSManifest
 */
public class DSpaceMETSIngester
       extends AbstractMETSIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceMETSIngester.class);

    // first part of required mets@PROFILE value
    private final static String PROFILE_START = "DSpace METS SIP Profile";

    // just check the profile name.
    void checkManifest(METSManifest manifest)
        throws MetadataValidationException
    {
        String profile = manifest.getProfile();
        if (profile == null)
            throw new MetadataValidationException("Cannot accept METS with no PROFILE attribute!");
        else if (!profile.startsWith(PROFILE_START))
            throw new MetadataValidationException("METS has unacceptable PROFILE value, profile="+profile);
    }

    public void checkPackageFiles(Set packageFiles, Set missingFiles,
                                  METSManifest manifest, PackageParameters params)
        throws PackageValidationException, CrosswalkException
    {
        // This is where a subclass would arrange to use or ignore
        // any "extra" files added to its type of package.
    }


    /**
     * Choose DMD section(s) to crosswalk.
     * <p>
     * The algorithm is:<br>
     * 1. Find MODS (preferably) or DC as primary DMD.<br>
     * 2. If (1) succeeds, crosswalk it and ignore all other DMDs with
     *    same GROUPID<br>
     * 3. Crosswalk remaining DMDs not eliminated already.
     */
    public void crosswalkObjectDmd(Context context, DSpaceObject dso,
                              METSManifest manifest,
                              AbstractMETSIngester.MdrefManager callback,
                              Element dmds[])
        throws CrosswalkException, PackageValidationException,
               AuthorizeException, SQLException, IOException
    {
        int found = -1;

        Item item = (Item)dso;

        // MODS is preferred
        for (int i = 0; i < dmds.length; ++i)
            if ("MODS".equals(manifest.getMdType(dmds[i])))
                found = i;

        // DC acceptable if no MODS
        if (found == -1)
        {
            for (int i = 0; i < dmds.length; ++i)
                if ("DC".equals(manifest.getMdType(dmds[i])))
                    found = i;
        }

        String groupID = null;
        if (found >= 0)
        {
            manifest.crosswalkItemDmd(context, item, dmds[found], callback);
            groupID = dmds[found].getAttributeValue("GROUPID");

            if (groupID != null)
            {
                for (int i = 0; i < dmds.length; ++i)
                {
                    String g = dmds[i].getAttributeValue("GROUPID");
                    if (g != null && !g.equals(groupID))
                        manifest.crosswalkItemDmd(context, item, dmds[i], callback);
                }
            }
        }

        // otherwise take the first.  Don't xwalk more than one because
        // each xwalk _adds_ metadata, and could add duplicate fields.
        else
        {
            if (dmds.length > 0)
                manifest.crosswalkItemDmd(context, item, dmds[0], callback);
        }
    }


    /**
     * Policy:  For DSpace deposit license, take deposit license
     * supplied by explicit argument first, else use collection's
     * default deposit license.
     * For Creative Commons, look for a rightsMd containing a CC license.
     */
    public void addLicense(Context context, Item item, String license,
                                    Collection collection, PackageParameters params)
        throws PackageValidationException,
               AuthorizeException, SQLException, IOException
    {
        if (PackageUtils.findDepositLicense(context, item) == null)
        PackageUtils.addDepositLicense(context, license, item, collection);
    }

    public void finishObject(Context context, DSpaceObject dso)
        throws PackageValidationException, CrosswalkException,
         AuthorizeException, SQLException, IOException
        {
        // nothing to do.
    }

    public int getObjectType(METSManifest manifest)
        throws PackageValidationException
            {
        return Constants.ITEM;
    }


    public Bitstream resolveBitstream(Context context,
                                                String path,
                                                Element mfile,
                                                Map packageFiles,
                                                PackageParameters params)
        throws SQLException,PackageValidationException
                {
        if (packageFiles.containsKey(path))
                    {
            Bitstream result = (Bitstream)packageFiles.get(path);
            packageFiles.remove(path);

            // Now that we're done using Name to match to <file>,
            // set default bitstream Name to last path element;
            // e.g. Zip entries all have '/' pathname separators
            // NOTE: set default here, hopefully crosswalk of
            // a bitstream techMD section will override it.
            String fname = result.getName();
            int lastSlash = fname.lastIndexOf('/');
            if (lastSlash >= 0  && lastSlash+1 < fname.length())
                result.setName(fname.substring(lastSlash+1));

            return result;
        }
        return null;
    }

    // return name of derived file as if MediaFilter created it, or null
    // only needed when importing a SIP without canonical DSpace derived file naming.
    private String makeDerivedFilename(String bundleName, String origName)
    {
        // get the MediaFilter that would create this bundle:
        String mfNames[] = PluginManager.getAllPluginNames(MediaFilter.class);

        for (int i = 0; i < mfNames.length; ++i)
                        {
            MediaFilter mf = (MediaFilter)PluginManager.getNamedPlugin(MediaFilter.class, mfNames[i]);
            if (bundleName.equals(mf.getBundleName()))
                return mf.getFilteredName(origName);
                        }
        return null;
                    }

    /**
     * Take a second pass over files to correct names of derived files
     * (e.g. thumbnails, extracted text) to what DSpace expects:
     */
    public void finishBitstream(Context context,
                                                Bitstream bs,
                                                Element mfile,
                                                METSManifest manifest,
                                                PackageParameters params)
        throws MetadataValidationException, SQLException, AuthorizeException, IOException
    {
        String bundleName = manifest.getBundleName(mfile);
        if (!bundleName.equals(Constants.CONTENT_BUNDLE_NAME))
        {
            String opath = manifest.getOriginalFilePath(mfile);
            if (opath != null)
            {
                // String ofileId = origFile.getAttributeValue("ID");
                // Bitstream obs = (Bitstream)fileIdToBitstream.get(ofileId);

                String newName = makeDerivedFilename(bundleName, opath);

                if (newName != null)
                {
                    //String mfileId = mfile.getAttributeValue("ID");
                    //Bitstream bs = (Bitstream)fileIdToBitstream.get(mfileId);
                    bs.setName(newName);
                    bs.update();
                }
            }
        }
    }


    public String getConfigurationName()
    {
        return "dspaceSIP";
    }


    public boolean probe(Context context, InputStream in, PackageParameters params)
    {
        throw new UnsupportedOperationException("PDF package ingester does not implement probe()");
    }
}
