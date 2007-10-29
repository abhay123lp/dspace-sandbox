/*
 * AbstractMETSDisseminator.java
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
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.harvard.hul.ois.mets.Agent;
import edu.harvard.hul.ois.mets.AmdSec;
import edu.harvard.hul.ois.mets.Checksumtype;
import edu.harvard.hul.ois.mets.Div;
import edu.harvard.hul.ois.mets.DmdSec;
import edu.harvard.hul.ois.mets.MdRef;
import edu.harvard.hul.ois.mets.FLocat;
import edu.harvard.hul.ois.mets.FileGrp;
import edu.harvard.hul.ois.mets.FileSec;
import edu.harvard.hul.ois.mets.Fptr;
import edu.harvard.hul.ois.mets.Mptr;
import edu.harvard.hul.ois.mets.Loctype;
import edu.harvard.hul.ois.mets.MdWrap;
import edu.harvard.hul.ois.mets.Mdtype;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.MetsHdr;
import edu.harvard.hul.ois.mets.Name;
import edu.harvard.hul.ois.mets.Role;
import edu.harvard.hul.ois.mets.StructMap;
import edu.harvard.hul.ois.mets.TechMD;
import edu.harvard.hul.ois.mets.SourceMD;
import edu.harvard.hul.ois.mets.DigiprovMD;
import edu.harvard.hul.ois.mets.RightsMD;
import edu.harvard.hul.ois.mets.helper.MdSec;
import edu.harvard.hul.ois.mets.Type;
import edu.harvard.hul.ois.mets.XmlData;
import edu.harvard.hul.ois.mets.helper.MetsElement;
import edu.harvard.hul.ois.mets.helper.MetsException;
import edu.harvard.hul.ois.mets.helper.MetsValidator;
import edu.harvard.hul.ois.mets.helper.MetsWriter;
import edu.harvard.hul.ois.mets.helper.PCData;
import edu.harvard.hul.ois.mets.helper.PreformedXML;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.handle.HandleManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Base class for disseminator of
 * METS (Metadata Encoding & Transmission Standard) Package.<br>
 *   See <a href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * packagers for more specific METS "profiles".   METS is an
 * abstract and flexible framework that can encompass many
 * different kinds of metadata and inner package structures.
 * <p>
 * <b>Package Parameters:</b><br>
 * <code>manifestOnly</code> -- if true, generate a standalone XML
 * document of the METS manifest instead of a complete package.  Any
 * other metadata (such as licenses) will be encoded inline.
 * Default is <code>false</code>.
 *
 * <code>internal</code> -- if true, generate a standalone XML document
 * for an Internal AIP, very similar to the effect of manifestOnly.
 * Default is <code>false</code>.
 *
 *   <code>unauthorized</code> -- this determines what is done when the
 *   packager encounters a Bundle or Bitstream it is not authorized to
 *   read.  By default, it just quits with an AuthorizeException.
 *   If this option is present, it must be one of the following values:
 *     <code>skip</code> -- simply exclude unreadable content from package.
 *     <code>zero</code> -- include unreadable bitstreams as 0-length files;
 *       unreadable Bundles will still cause authorize errors.
 *
 * @author Larry Stone
 * @author Robert Tansley
 * @version $Revision$
 */
public abstract class AbstractMETSDisseminator
    implements PackageDisseminator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractMETSDisseminator.class);

    // JDOM xml output writer - indented format for readability.
    private static XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

    // for gensym()
    private int idCounter = 1;

    /**
     * Wrapper for a table of streams to add to the package, such as
     * mdRef'd metadata.  Key is relative pathname of file, value is
     * <code>InputStream</code> with contents to put in it.  Some
     * superclasses will put streams in this table when adding an mdRef
     * element to e.g. a rightsMD segment.
     */
    protected class MdStreamCache
    {
        private Map<MdRef,InputStream> extraFiles = new HashMap<MdRef,InputStream>();

        public void addStream(MdRef key, InputStream md)
        {
            extraFiles.put(key, md);
        }

        public Map<MdRef,InputStream> getMap()
        {
            return extraFiles;
        }

        public void close()
            throws IOException
        {
            for (InputStream is : extraFiles.values())
                is.close();
        }
    }

    /**
     * Make a new unique ID symbol with specified prefix.
     * @param prefix the prefix of the identifier, constrained to XML ID schema
     * @return a new string identifier unique in this session (instance).
     */
    protected synchronized String gensym(String prefix)
    {
        return prefix + "_" + String.valueOf(idCounter++);
    }

    public String getMIMEType(PackageParameters params)
    {
        return (params != null &&
                (params.getBooleanProperty("manifestOnly", false) ||
                 params.getBooleanProperty("internal", false))) ?
                "text/xml" : "application/zip";
    }

    /**
     * Export the object (Item, Collection, or Community) to a
     * package file on the indicated OutputStream.
     * Gets an exception of the object cannot be packaged or there is
     * a failure creating the package.
     *
     * @param context - DSpace context.
     * @param dso - DSpace object (item, collection, etc)
     * @param pkg - output stream on which to write package
     * @throws PackageException if package cannot be created or there is
     *  a fatal error in creating it.
     */
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, OutputStream pkg)
        throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        long lmTime = 0;
        if (dso.getType() == Constants.ITEM)
            lmTime = ((Item)dso).getLastModified().getTime();

            // how to handle unauthorized bundle/bitstream:
            String unauth = (params == null) ? null : params.getProperty("unauthorized");
        MdStreamCache extraStreams = null;
        try
        {
            // Generate a true manifest-only "package", no external data.
            if (params != null && params.getBooleanProperty("manifestOnly", false))
            {
                Mets manifest = makeManifest(context, dso, params, null);
                manifest.validate(new MetsValidator());
                manifest.write(new MetsWriter(pkg));
            }

            // for Internal "package", i.e. AIP saved in Asset Store,
            // put each "extra" metadata streams in a Bitstream and then
            // add the URI of that Bitstream to the PackageParam
            // additionalBitstreamURIs so the client can find it.  Sneaky!
            else if (params != null && params.getBooleanProperty("internal", false))
            {
                extraStreams = new MdStreamCache();
                Mets manifest = makeManifest(context, dso, params, extraStreams);
                for (Map.Entry ment : extraStreams.getMap().entrySet())
                {
                    InputStream is = (InputStream)ment.getValue();
                    Bitstream bs = Bitstream.create(context, is);
                    is.close();
             
                    String fname = makeBitstreamURL(bs, params);
                    if (log.isDebugEnabled())
                        log.debug("Wrote EXTRA stream to a disconnected bitstream: "+fname);
                    MdRef ref = (MdRef)ment.getKey();
                    ref.setXlinkHref(fname);
                    params.addProperty("additionalBitstreamURIs", fname);
            }

                // can only validate now after fixing up extraStreams
                manifest.validate(new MetsValidator());
                manifest.write(new MetsWriter(pkg));
            }

            // make a Zip-based package
            else
            {
                // map of extra streams to put in Zip
                extraStreams = new MdStreamCache();
                ZipOutputStream zip = new ZipOutputStream(pkg);
                zip.setComment("METS archive created by DSpace METSDisseminationCrosswalk");
                Mets manifest = makeManifest(context, dso, params, extraStreams);

                // copy extra (meta?) bitstreams into zip, update manifest
                if (extraStreams != null)
                {
                    for (Map.Entry ment : extraStreams.getMap().entrySet())
                    {
                        MdRef ref = (MdRef)ment.getKey();
                        InputStream is = (InputStream)ment.getValue();
                        // hopefully unique filename within the Zip
                        String fname = gensym("metadata");
                        ref.setXlinkHref(fname);
                        if (log.isDebugEnabled())
                            log.debug("Writing EXTRA stream to Zip: "+fname);
                    ZipEntry ze = new ZipEntry(fname);
                        if (lmTime != 0)
                    ze.setTime(lmTime);
                    zip.putNextEntry(ze);
                        Utils.copy(is, zip);
                    zip.closeEntry();
                        is.close();
                    }
                }

                // write manifest after metadata.
                ZipEntry me = new ZipEntry(METSManifest.MANIFEST_FILE);
                if (lmTime != 0)
                    me.setTime(lmTime);
                zip.putNextEntry(me);

                // can only validate now after fixing up extraStreams
                manifest.validate(new MetsValidator());
                manifest.write(new MetsWriter(zip));
                zip.closeEntry();
                 
                // copy all non-meta bitstreams into zip
                if (dso.getType() == Constants.ITEM)
                {
                    Item item = (Item)dso;
                 
                Bundle bundles[] = item.getBundles();
                for (int i = 0; i < bundles.length; i++)
                {
                        if (includeBundle(bundles[i]))
                    {
                        // unauthorized bundle?
                        if (!AuthorizeManager.authorizeActionBoolean(context,
                                    bundles[i], Constants.READ))
                        {
                            if (unauth != null &&
                                (unauth.equalsIgnoreCase("skip")))
                            {
                                log.warn("Skipping Bundle[\""+bundles[i].getName()+"\"] because you are not authorized to read it.");
                                continue;
                            }
                            else
                                throw new AuthorizeException("Not authorized to read Bundle named \""+bundles[i].getName()+"\"");
                        }
                        Bitstream[] bitstreams = bundles[i].getBitstreams();
                        for (int k = 0; k < bitstreams.length; k++)
                        {
                            boolean auth = AuthorizeManager.authorizeActionBoolean(context,
                                    bitstreams[k], Constants.READ);
                            if (auth ||
                                (unauth != null && unauth.equalsIgnoreCase("zero")))
                            {
                                    String zname = makeBitstreamURL(bitstreams[k], params);
                                    ZipEntry ze = new ZipEntry(zname);
                                    if (log.isDebugEnabled())
                                        log.debug("Writing CONTENT stream of bitstream("+String.valueOf(bitstreams[k].getID())+") to Zip: "+zname+
                                                    ", size="+String.valueOf(bitstreams[k].getSize()));
                                    if (lmTime != 0)
                                ze.setTime(lmTime);
                                ze.setSize(auth ? bitstreams[k].getSize() : 0);
                                zip.putNextEntry(ze);
                                if (auth)
                                Utils.copy(bitstreams[k].retrieve(), zip);
                                else
                                    log.warn("Adding zero-length file for Bitstream, SID="+String.valueOf(bitstreams[k].getSequenceID())+", not authorized for READ.");
                                zip.closeEntry();
                            }
                            else if (unauth != null &&
                                     unauth.equalsIgnoreCase("skip"))
                            {
                                log.warn("Skipping Bitstream, SID="+String.valueOf(bitstreams[k].getSequenceID())+", not authorized for READ.");
                            }
                            else
                            {
                                throw new AuthorizeException("Not authorized to read Bitstream, SID="+String.valueOf(bitstreams[k].getSequenceID()));
                            }
                        }
                    }
                }
            }

                // Coll, Comm just add logo bitstream to content if there is one
                else if (dso.getType() == Constants.COLLECTION ||
                         dso.getType() == Constants.COMMUNITY)
                {
                    Bitstream logoBs = dso.getType() == Constants.COLLECTION ?
                                         ((Collection)dso).getLogo() :
                                         ((Community)dso).getLogo();
                    if (logoBs != null)
                    {
                        String zname = makeBitstreamURL(logoBs, params);
                        ZipEntry ze = new ZipEntry(zname);
                        if (log.isDebugEnabled())
                            log.debug("Writing CONTENT stream of bitstream("+String.valueOf(logoBs.getID())+") to Zip: "+zname+", size="+String.valueOf(logoBs.getSize()));
                        ze.setSize(logoBs.getSize());
                        zip.putNextEntry(ze);
                        Utils.copy(logoBs.retrieve(), zip);
                        zip.closeEntry();
        }
    }
                zip.close();
            }
        }
        catch (MetsException e)
    {
            // We don't pass up a MetsException, so callers don't need to
            // know the details of the METS toolkit
            log.error("METS error: ",e);
            throw new PackageValidationException(e);
        }
        finally
        {
            if (extraStreams != null)
                extraStreams.close();
        }
    }

    // set metadata type - if Mdtype.parse() gets exception,
    // that means it's not in the MDTYPE vocabulary, so use OTHER.
    private void setMdType(MdWrap mdWrap, String mdtype)
    {
        try
        {
            mdWrap.setMDTYPE(Mdtype.parse(mdtype));
        }
        catch (MetsException e)
        {
            mdWrap.setMDTYPE(Mdtype.OTHER);
            mdWrap.setOTHERMDTYPE(mdtype);
        }
    }

    // set metadata type - if Mdtype.parse() gets exception,
    // that means it's not in the MDTYPE vocabulary, so use OTHER.
    private void setMdType(MdRef  mdRef, String mdtype)
    {
        try
        {
            mdRef.setMDTYPE(Mdtype.parse(mdtype));
        }
        catch (MetsException e)
        {
            mdRef.setMDTYPE(Mdtype.OTHER);
            mdRef.setOTHERMDTYPE(mdtype);
        }
    }

    /**
     * Create an element wrapped around a metadata reference (either mdWrap
     * or mdRef); i.e. dmdSec, techMd, sourceMd, etc.  Checks for
     * XML-DOM oriented crosswalk first, then if not found looks for
     * stream crosswalk of the same name.
     *
     * @returns mdSec element or null if xwalk returns empty results.
     */
    private MdSec makeMdSec(Context context, DSpaceObject dso, Class mdSecClass, String typeSpec,
                            MdStreamCache extraStreams)
        throws SQLException, PackageValidationException, CrosswalkException,
               IOException, AuthorizeException
    {
        // for running a stream-oriented xwalk in a background thread.
        class BackgroundWriter implements Runnable
        {
            private Context bgContext;
            private StreamDisseminationCrosswalk bgSxwalk;
            private DSpaceObject bgDso;
            private PipedOutputStream os = null;
         
            BackgroundWriter(Context c, StreamDisseminationCrosswalk x, DSpaceObject d)
            {
                super();
                bgContext = c;
                bgSxwalk = x;
                bgDso = d;
            }
         
            // set up pipe, save output-stm end, return input-stm end.
            InputStream getInputStream()
                throws IOException
            {
                os = new PipedOutputStream();
                return new PipedInputStream(os);
            }
         
            // pipe the AIP into a bitstream in separate thread.
            public void run()
            {
                try
                {
                    bgSxwalk.disseminate(bgContext, bgDso, os);
                    os.close();
                }
                catch (IOException e)
                {
                    log.error("run(): Got IOException: ",e);
                }
                catch (SQLException e)
                {
                    log.error("run(): Got SQLException: ",e);
                }
                catch (CrosswalkException e)
                {
                    log.error("run(): Got SQLException: ",e);
                }
                catch (AuthorizeException e)
                {
                    log.error("run(): Got SQLException: ",e);
                }
            }
        } /* end class BackgroundWriter */

        try
            {
            MdSec mdSec = (MdSec) mdSecClass.newInstance();
            mdSec.setID(gensym(mdSec.getLocalName()));
            String parts[] = typeSpec.split(":", 2);
                String xwalkName, metsName;
                if (parts.length > 1)
                {
                    metsName = parts[0];
                    xwalkName = parts[1];
                }
                else
                xwalkName = metsName = typeSpec;

            // first look for DOM-type crosswalk:
                DisseminationCrosswalk xwalk = (DisseminationCrosswalk)
                  PluginManager.getNamedPlugin(DisseminationCrosswalk.class, xwalkName);
            if (xwalk != null)
            {
                // for wrapping an embedded XML model
                MdWrap mdWrap = new MdWrap();
                setMdType(mdWrap, metsName);
                XmlData xmlData = new XmlData();
                if (crosswalkToMetsElement(xwalk, dso, xmlData) != null)
                {
                mdWrap.getContent().add(xmlData);
                    mdSec.getContent().add(mdWrap);
                    return mdSec;
                }
                else
                    return null;
            }
         
            // next try looking for stream-oriented crosswalk:
            else
            {
                StreamDisseminationCrosswalk sxwalk = (StreamDisseminationCrosswalk)
                  PluginManager.getNamedPlugin(StreamDisseminationCrosswalk.class, xwalkName);
                if (sxwalk != null)
                {
                    if (sxwalk.canDisseminate(context, dso))
                    {
                        // start up slave thread to feed crosswalk to input stream
                        MdRef mdRef = new MdRef();
                        BackgroundWriter bgw = new BackgroundWriter(context, sxwalk, dso);
                        extraStreams.addStream(mdRef, bgw.getInputStream());
                        Thread slave = new Thread(bgw);
                        slave.setDaemon(true);
                        slave.start();
                         
                        mdRef.setMIMETYPE(sxwalk.getMIMEType());
                        setMdType(mdRef, metsName);
                        mdRef.setLOCTYPE(Loctype.URL);
                        mdSec.getContent().add(mdRef);
                        return mdSec;
            }
                    else
                        return null;
            }
                else
                    throw new PackageValidationException("Cannot find "+xwalkName+" crosswalk plugin, either DisseminationCrosswalk or StreamDisseminationCrosswalk");
            }
        }
        catch (InstantiationException e)
        {
            throw new PackageValidationException("Error instantiating Mdsec object: "+ e.toString());
        }
        catch (IllegalAccessException e)
        {
            throw new PackageValidationException("Error instantiating Mdsec object: "+ e.toString());
        }
            }

    // add either a techMd or sourceMd element to amdSec.
    // mdSecClass determines which type.
    // mdTypes[] is array of "[metsName:]PluginName" strings, maybe empty.
    private void addToAmdSec(AmdSec fAmdSec, String mdTypes[], Class mdSecClass,
                             Context context, DSpaceObject dso, MdStreamCache extraStreams)
        throws SQLException, PackageValidationException, CrosswalkException,
               IOException, AuthorizeException
    {
        for (int i = 0; i < mdTypes.length; ++i)
        {
            MdSec md = makeMdSec(context, dso, mdSecClass, mdTypes[i], extraStreams);
            if (md != null)
                fAmdSec.getContent().add(md);
        }
    }

    // Create amdSec for any tech md's, return its ID attribute.
    private String addAmdSec(Context context, DSpaceObject dso, PackageParameters params,
                             Mets mets, MdStreamCache extraStreams)
        throws SQLException, PackageValidationException, CrosswalkException,
               IOException, AuthorizeException
    {
        String techMdTypes[] = getTechMdTypes(context, dso, params);
        String rightsMdTypes[] = getRightsMdTypes(context, dso, params);
        String sourceMdTypes[] = getSourceMdTypes(context, dso, params);
        String digiprovMdTypes[] = getDigiprovMdTypes(context, dso, params);
         
        // only bother if there are any sections to add
        if ((techMdTypes.length+sourceMdTypes.length+
             digiprovMdTypes.length+rightsMdTypes.length) > 0)
            {
            String result = gensym("amd");
            AmdSec fAmdSec = new AmdSec();
            fAmdSec.setID(result);
            addToAmdSec(fAmdSec, techMdTypes, TechMD.class, context, dso, extraStreams);
            addToAmdSec(fAmdSec, rightsMdTypes, RightsMD.class, context, dso, extraStreams);
            addToAmdSec(fAmdSec, sourceMdTypes, SourceMD.class, context, dso, extraStreams);
            addToAmdSec(fAmdSec, digiprovMdTypes, DigiprovMD.class, context, dso, extraStreams);

            mets.getContent().add(fAmdSec);
            return result;
            }
            else
            return null;
    }

    // make the most "persistent" identifier possible, preferably a URN
    // based on the Handle.
    private String makePersistentID(DSpaceObject dso)
    {
        String handle = dso.getHandle();

        // If no Handle, punt to much-less-satisfactory database ID and type..
        if (handle == null)
            return "DSpace_DB_"+Constants.typeText[dso.getType()] + "_" + String.valueOf(dso.getID());
        else
            return getHandleURN(handle);
    }

    /**
     * Write out a METS manifest.
     * Mostly lifted from Rob Tansley's METS exporter.
     */
    private Mets makeManifest(Context context, DSpaceObject dso,
                              PackageParameters params,
                              MdStreamCache extraStreams)
        throws MetsException, PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException

    {
        // Create the METS manifest in memory
        Mets mets = new Mets();
        String typeStr = Constants.typeText[dso.getType()];

        // this ID should be globally unique
        mets.setID("dspace"+Utils.generateKey());

        // identifies the object described by this document
        mets.setOBJID(makePersistentID(dso));
        mets.setTYPE("DSpace "+typeStr);

        // this is the signature by which the ingester will recognize
        // a document it can expect to interpret.
        mets.setPROFILE(getProfile());
        
        MetsHdr metsHdr = makeMetsHdr(context, dso, params);
        if (metsHdr != null)
            mets.getContent().add(metsHdr);
        
        // add DMD sections
        // Each type element MAY be either just a MODS-and-crosswalk name, OR
        // a combination "MODS-name:crosswalk-name" (e.g. "DC:qDC").
        String dmdTypes[] = getDmdTypes(context, dso, params);

        // record of ID of each dmdsec to make DMDID in structmap.
        String dmdGroup = gensym("dmd_group");
        String dmdId[] = new String[dmdTypes.length];
        for (int i = 0; i < dmdTypes.length; ++i)
        {
            MdSec dmdSec = makeMdSec(context, dso, DmdSec.class, dmdTypes[i], extraStreams);
            if (dmdSec != null)
            {
                mets.getContent().add(dmdSec);
                dmdId[i] = dmdSec.getID();
            }
        }
        
        // add object-wide technical/source MD segments, get ID string:
        // Put that ID in ADMID of first div in structmap.
        String objectAMDID = addAmdSec(context, dso, params, mets, extraStreams);

        // Create simple structMap: initial div represents the Object's
        // contents, its children are e.g. Item bitstreams (content only),
        // Collection's members, or Community's members.
        StructMap structMap = new StructMap();
        structMap.setID(gensym("struct"));
        structMap.setTYPE("LOGICAL");
        structMap.setLABEL("DSpace Object");
        Div div0 = new Div();
        div0.setID(gensym("div"));
        div0.setTYPE("DSpace Object Contents");
        structMap.getContent().add(div0);

        // fileSec is optional, let object type create it if needed.
        FileSec fileSec = null;

        // Item-specific manifest - license, bitstreams as Files, etc.
        if (dso.getType() == Constants.ITEM)
        {
            // this tags file ID and group identifiers for bitstreams.
            String bitstreamIDstart = "bitstream_";
            Item item = (Item)dso;

            // how to handle unauthorized bundle/bitstream:
            String unauth = (params == null) ? null : params.getProperty("unauthorized");

            // fileSec - all non-metadata bundles go into fileGrp,
            // and each bitstream therein into a file.
            // Create the bitstream-level techMd and div's for structmap
            // at the same time so we can connec the IDREFs to IDs.
            fileSec = new FileSec();
            Bundle[] bundles = item.getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                if (!includeBundle(bundles[i]))
                    continue;

                // unauthorized bundle?
                // NOTE: This must match the logic in disseminate()
                if (!AuthorizeManager.authorizeActionBoolean(context,
                            bundles[i], Constants.READ))
                {
                    if (unauth != null &&
                        (unauth.equalsIgnoreCase("skip")))
                        continue;
                    else
                        throw new AuthorizeException("Not authorized to read Bundle named \""+bundles[i].getName()+"\"");
                }

                Bitstream[] bitstreams = bundles[i].getBitstreams();

                // Create a fileGrp, USE = permuted Bundle name
                FileGrp fileGrp = new FileGrp();
                String bName = bundles[i].getName();
                if ((bName != null) && !bName.equals(""))
                    fileGrp.setUSE(bundleToFileGrp(bName));
         
                // watch for primary bitstream
                int primaryBitstreamID = -1;
                boolean isContentBundle = false;
                if ((bName != null) && bName.equals("ORIGINAL"))
                {
                    isContentBundle = true;
                    primaryBitstreamID = bundles[i].getPrimaryBitstreamID();
                }

                for (int bits = 0; bits < bitstreams.length; bits++)
                {
                    // Check for authorization.  Handle unauthorized
                    // bitstreams to match the logic in disseminate(),
                    // i.e. "unauth=zero" means include a 0-length bitstream,
                    // "unauth=skip" means to ignore it (and exclude from
                    // manifest).
                    boolean auth = AuthorizeManager.authorizeActionBoolean(context,
                            bitstreams[bits], Constants.READ);
                    if (!auth)
                    {
                        if (unauth != null && unauth.equalsIgnoreCase("skip"))
                            continue;
                        else if (!(unauth != null && unauth.equalsIgnoreCase("zero")))
                            throw new AuthorizeException("Not authorized to read Bitstream, SID="+String.valueOf(bitstreams[bits].getSequenceID()));
                    }

                    String sid = String.valueOf(bitstreams[bits].getSequenceID());
                    String fileID = bitstreamIDstart + sid;
                    edu.harvard.hul.ois.mets.File file = new edu.harvard.hul.ois.mets.File();
                    file.setID(fileID);
                    file.setSEQ(bitstreams[bits].getSequenceID());
                    fileGrp.getContent().add(file);

                    // set primary bitstream in structMap
                    if (bitstreams[bits].getID() == primaryBitstreamID)
                    {
                        Fptr fptr = new Fptr();
                        fptr.setFILEID(fileID);
                        div0.getContent().add(0, fptr);
                    }

                    // if this is content, add to structmap too:
                    if (isContentBundle)
                        div0.getContent().add(makeFileDiv(fileID, "DSpace Content Bitstream"));
         
                    /*
                     * If we're in THUMBNAIL or TEXT bundles, the bitstream is
                     * extracted text or a thumbnail, so we use the name to work
                     * out which bitstream to be in the same group as
                     */
                    String groupID = "GROUP_" + bitstreamIDstart + sid;
                    if ((bundles[i].getName() != null)
                            && (bundles[i].getName().equals("THUMBNAIL") ||
                                bundles[i].getName().startsWith("TEXT")))
                    {
                        // Try and find the original bitstream, and chuck the
                        // derived bitstream in the same group
                        Bitstream original = findOriginalBitstream(item,
                                bitstreams[bits]);
                        if (original != null)
                        {
                                groupID = "GROUP_" + bitstreamIDstart
                                    + original.getSequenceID();
                        }
                    }
                    file.setGROUPID(groupID);
                    file.setMIMETYPE(bitstreams[bits].getFormat().getMIMEType());
         
                    // FIXME: CREATED: no date

                    file.setSIZE(auth ? bitstreams[bits].getSize() : 0);

                    // FIXME: need to translate checksum and type to METS, if available.
                    String csType = bitstreams[bits].getChecksumAlgorithm();
                    String cs = bitstreams[bits].getChecksum();
                    if (auth && cs != null && csType != null)
                    {
                        try
                        {
                            file.setCHECKSUMTYPE(Checksumtype.parse(csType));
                            file.setCHECKSUM(cs);
                        }
                        catch (MetsException e)
                        {
                            log.warn("Cannot set bitstream checksum type="+csType+" in METS.");
                        }
                    }
         
                    // FLocat: point to internal or external location of bitstream contents.
                    FLocat flocat = new FLocat();
                    flocat.setLOCTYPE(Loctype.URL);
                    flocat.setXlinkHref(makeBitstreamURL(bitstreams[bits], params));
                    file.getContent().add(flocat);

                    // technical metadata for bitstream
                    String techID = addAmdSec(context, bitstreams[bits], params, mets, extraStreams);
                    if (techID != null)
                    file.setADMID(techID);
                }
                fileSec.getContent().add(fileGrp);
            }
        }
        else if (dso.getType() == Constants.COLLECTION)
        {
            ItemIterator ii = ((Collection)dso).getItems();
            while (ii.hasNext())
            {
                Item item = ii.next();
                String hdl = item.getHandle();
                if (hdl == null)
                    log.warn("Collection packager is skipping Item without handle: "+item.toString());
                else
                    div0.getContent().add(makeHandleDiv(hdl, "DSpace Item"));
            }
            Bitstream logoBs = ((Collection)dso).getLogo();
            if (logoBs != null)
            {
                fileSec = new FileSec();
                addLogoBitstream(logoBs, fileSec, div0, params);
            }
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
            // make separate sub-divs for subcomm's and colls:
            Div subcommDiv = new Div();
            subcommDiv.setID(gensym("div"));
            subcommDiv.setTYPE("SUBCOMMUNITIES");
            div0.getContent().add(subcommDiv);
            Div collDiv = new Div();
            collDiv.setID(gensym("div"));
            collDiv.setTYPE("COLLECTIONS");
            div0.getContent().add(collDiv);
            Community subcomms[] = ((Community)dso).getSubcommunities();
            for (int i = 0; i < subcomms.length; ++i)
            {
                String hdl = subcomms[i].getHandle();
                if (hdl == null)
                    log.warn("Collection packager is skipping Subcommunity without handle: "+subcomms[i].toString());
                else
                    subcommDiv.getContent().add(makeHandleDiv(hdl, "DSpace Community"));
            }
            Collection colls[] = ((Community)dso).getCollections();
            for (int i = 0; i < colls.length; ++i)
            {
                String hdl = colls[i].getHandle();
                if (hdl == null)
                    log.warn("Collection packager is skipping Collection without handle: "+colls[i].toString());
                else
                    collDiv.getContent().add(makeHandleDiv(hdl, "DSpace Collection"));
            }
            Bitstream logoBs = ((Community)dso).getLogo();
            if (logoBs != null)
            {
                fileSec = new FileSec();
                addLogoBitstream(logoBs, fileSec, div0, params);
            }
        }
        if (fileSec != null)
            mets.getContent().add(fileSec);
        mets.getContent().add(structMap);
         
        // set links to metadata for object -- after type-specific
        // code since that can add to the object metadata.
            StringBuffer dmdIds = new StringBuffer();
            for (int i = 0; i < dmdId.length; ++i)
                dmdIds.append(" "+dmdId[i]);
            div0.setDMDID(dmdIds.substring(1));
        if (objectAMDID != null)
            div0.setADMID(objectAMDID);

            // Does subclass have something to add to structMap?
        addStructMap(context, dso, params, mets);

        return mets;
    }

    // Install logo bitstream into METS for Community, Collection.
    // Add a file element, and refer to it from an fptr in the first div
    // of the main structMap.
    private void addLogoBitstream(Bitstream logoBs, FileSec fileSec, Div div0, PackageParameters params)
    {
        edu.harvard.hul.ois.mets.File file = new edu.harvard.hul.ois.mets.File();
        String fileID = gensym("logo");
        file.setID(fileID);
        file.setMIMETYPE(logoBs.getFormat().getMIMEType());
        file.setSIZE(logoBs.getSize());
         
        // FIXME: need to translate checksum and type to METS, if available.
        String csType = logoBs.getChecksumAlgorithm();
        String cs = logoBs.getChecksum();
        if (cs != null && csType != null)
        {
            try
            {
                file.setCHECKSUMTYPE(Checksumtype.parse(csType));
                file.setCHECKSUM(cs);
        }
        catch (MetsException e)
        {
                log.warn("Cannot set bitstream checksum type="+csType+" in METS.");
            }
        }
        FLocat flocat = new FLocat();
        flocat.setLOCTYPE(Loctype.URL);
        flocat.setXlinkHref(makeBitstreamURL(logoBs, params));
        file.getContent().add(flocat);
        FileGrp fileGrp = new FileGrp();
        fileGrp.setUSE("LOGO");
        fileGrp.getContent().add(file);
        fileSec.getContent().add(fileGrp);

        // add fptr directly to div0 of structMap
        Fptr fptr = new Fptr();
        fptr.setFILEID(fileID);
        div0.getContent().add(0, fptr);
        }

    // create <div> element pointing to a file
    private Div makeFileDiv(String fileID, String type)
    {
        Div div = new Div();
        div.setID(gensym("div"));
        div.setTYPE(type);
        Fptr fptr = new Fptr();
        fptr.setFILEID(fileID);
        div.getContent().add(fptr);
        return div;
    }

    // create <div> element with mptr pointing to a Handle
    private Div makeHandleDiv(String handle, String type)
    {
        Div div = new Div();
        div.setID(gensym("div"));
        div.setTYPE(type);
        Mptr mptr = new Mptr();
        mptr.setID(gensym("mptr"));
        mptr.setLOCTYPE(Loctype.HANDLE);
        mptr.setXlinkHref(handle);
        div.getContent().add(mptr);
        return div;
    }

    // put handle in canonical URN format -- note that HandleManager's
    // canonicalize currently returns HTTP URL format.
    protected String getHandleURN(String handle)
    {
        if (handle.startsWith("hdl:"))
            return handle;
        return "hdl:"+handle;
    }

    /**
     * For a bitstream that's a thumbnail or extracted text, find the
     * corresponding bitstream it was derived from, in the ORIGINAL bundle.
     *
     * @param item
     *            the item we're dealing with
     * @param derived
     *            the derived bitstream
     *
     * @return the corresponding original bitstream (or null)
     */
    protected static Bitstream findOriginalBitstream(Item item, Bitstream derived)
        throws SQLException
    {
        Bundle[] bundles = item.getBundles();

        // Filename of original will be filename of the derived bitstream
        // minus the extension (last 4 chars - .jpg or .txt)
        String originalFilename = derived.getName().substring(0,
                derived.getName().length() - 4);

        // First find "original" bundle
        for (int i = 0; i < bundles.length; i++)
        {
            if ((bundles[i].getName() != null)
                    && bundles[i].getName().equals("ORIGINAL"))
            {
                // Now find the corresponding bitstream
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                for (int bsnum = 0; bsnum < bitstreams.length; bsnum++)
                {
                    if (bitstreams[bsnum].getName().equals(originalFilename))
                    {
                        return bitstreams[bsnum];
                    }
                }
            }
        }

        // Didn't find it
        return null;
    }

    // Get result from crosswalk plugin and add it to the document,
    // including namespaces and schema.
    // returns the new/modified element upon success.
    private MetsElement crosswalkToMetsElement(DisseminationCrosswalk xwalk,
                                 DSpaceObject dso, MetsElement me)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        try
        {
        // add crosswalk's namespaces and schemaLocation to this element:
        String raw = xwalk.getSchemaLocation();
        String sloc[] = raw == null ? null : raw.split("\\s+");
        Namespace ns[] = xwalk.getNamespaces();
        for (int i = 0; i < ns.length; ++i)
        {
            String uri = ns[i].getURI();
            if (sloc != null && sloc.length > 1 && uri.equals(sloc[0]))
                me.setSchema(ns[i].getPrefix(), uri, sloc[1]);
            else
                me.setSchema(ns[i].getPrefix(), uri);
        }

        // add result of crosswalk
            PreformedXML pXML = null;
            if (xwalk.preferList())
            {
                List res = xwalk.disseminateList(dso);
                if (!(res == null || res.isEmpty()))
                    pXML = new PreformedXML(outputter.outputString(res));
            }
            else
            {
                Element res = xwalk.disseminateElement(dso);
                if (res != null)
                    pXML = new PreformedXML(outputter.outputString(res));
            }
            if (pXML != null)
            {
        me.getContent().add(pXML);
                return me;
            }
            return null;
    }
        catch (CrosswalkObjectNotSupported e)
        {
            // ignore this xwalk if object is unsupported.
            if (log.isDebugEnabled())
                log.debug("Skipping MDsec because of CrosswalkObjectNotSupported: dso="+dso.toString()+", xwalk="+xwalk.getClass().getName());
            return null;
        }
    }

    /**
     * Return identifier for bitstream in an Item; when making a package,
     * this is the archive member name (e.g. in Zip file).  In a bare
     * manifest, it might be an external URL.  The name should be in URL
     * format ("file:" may be elided for in-archive filenames).  It should
     * be deterministic, since this gets called twice for each bitstream
     * when building archive.
     */
    abstract public String makeBitstreamURL(Bitstream bitstream, PackageParameters params);

    /**
     * Create metsHdr element - separate so subclasses can override.
     */
    abstract public MetsHdr makeMetsHdr(Context context, DSpaceObject dso,
                               PackageParameters params);
    /**
     * Returns name of METS profile to which this package conforms, e.g.
     *  "DSpace METS DIP Profile 1.0"
     * @return string name of profile.
     */
    abstract public String getProfile();

    /**
     * Returns fileGrp's USE attribute value corresponding to a DSpace bundle name.
     *
     * @param bname name of DSpace bundle.
     * @return string name of fileGrp
     */
    abstract public String bundleToFileGrp(String bname);

    /**
     * Get the types of Item-wide DMD to include in package.
     * Each element of the returned array is a String, which
     * MAY be just a simple name, naming both the Crosswalk Plugin and
     * the METS "MDTYPE", <em>or</em> a colon-separated pair consisting of
     * the METS name followed by a colon and the Crosswalk Plugin name.
     * E.g. the type string <code>"DC:qualifiedDublinCore"</code> tells it to
     * create a METS section with <code>MDTYPE="DC"</code> and use the plugin
     * named "qualifiedDublinCore" to obtain the data.
     * @param params the PackageParameters passed to the disseminator.
     * @return array of metadata type strings, never null.
     */
    abstract public String [] getDmdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException;

    /**
     * Get the type string of the technical metadata to create for each
     * object and each Bitstream in an Item.  The type string may be a
     * simple name or colon-separated compound as specified for
     *  <code>getDmdTypes()</code> above.
     * @param params the PackageParameters passed to the disseminator.
     * @return array of metadata type strings, never null.
     */
    abstract public String[] getTechMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException;

    /**
     * Get the type string of the source metadata to create for each
     * object and each Bitstream in an Item.  The type string may be a
     * simple name or colon-separated compound as specified for
     * <code>getDmdTypes()</code> above.
     * @param params the PackageParameters passed to the disseminator.
     * @return array of metadata type strings, never null.
     */
    abstract public String[] getSourceMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException;

    /**
     * Get the type string of the "digiprov" (digital provenance)
     * metadata to create for each object and each Bitstream in an Item.
     * The type string may be a simple name or colon-separated compound
     * as specified for <code>getDmdTypes()</code> above.
     *
     * @param params the PackageParameters passed to the disseminator.
     * @return array of metadata type strings, never null.
     */
    abstract public String[] getDigiprovMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException;

    /**
     * Get the type string of the "rights" (permission and/or license)
     * metadata to create for each object and each Bitstream in an Item.
     * The type string may be a simple name or colon-separated compound
     * as specified for <code>getDmdTypes()</code> above.
     *
     * @param params the PackageParameters passed to the disseminator.
     * @return array of metadata type strings, never null.
     */
    abstract public String[] getRightsMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException;

    /**
     * Add any additional <code>structMap</code> elements to the
     * METS document, as required by this subclass.  A simple default
     * structure map which fulfills the minimal DSpace METS DIP/SIP
     * requirements is already present, so this does not need to do anything.
     * @param mets the METS document to which to add structMaps
     */
    abstract public void addStructMap(Context context, DSpaceObject dso,
                               PackageParameters params, Mets mets)
        throws SQLException, IOException, AuthorizeException, MetsException;

    /**
     * @return true when this bundle should be included as "content"
     *  in the package.. e.g. DSpace SIP does not include metadata bundles.
     */
    abstract public boolean includeBundle(Bundle bundle);
}
