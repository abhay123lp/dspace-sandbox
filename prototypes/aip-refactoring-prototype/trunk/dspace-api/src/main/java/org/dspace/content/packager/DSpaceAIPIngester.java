/*
 * DSpaceAIPIngester
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
import java.io.FilterInputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.jdom.Element;

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
import org.dspace.core.Utils;
import org.dspace.core.PluginManager;
import org.dspace.license.CreativeCommons;

/**
 * Subclass of the METS packager framework to ingest a DSpace
 * Archival Information Package (AIP).  The AIP is intended to be, foremost,
 * a _complete_ and _accurate_ representation of one object in the DSpace
 * object model.  An AIP contains all of the information needed to restore
 * the object precisely in another DSpace archive instance.
 * <p>
 * This ingester recognizes two distinct types of AIPs: "Internal" and "External".
 * The internal AIP, which is selected by specifying a PackageParameters
 * key "internal" with the value "true", is intended to preserve a complete
 * record of an object within its own archive, as a file in the asset store.
 * Thus, it can refer to other files in the asset store by location instead
 * of finding them in the "package", so its package format is simply a
 * METS XML document serialized into a file.
 * <p>
 * An "external" AIP (the default), is a conventional Zip-file based package
 * that includes copies of all bitstreams referenced by the object as well
 * as a serialized METS XML document in the path "mets.xml".
 *
 * Configuration keys:
 *
 *  # instructs which xwalk plugin to use for a given type of metadata
 *  mets.dspaceAIP.ingest.crosswalk.{mdSecName} = {pluginName}
 *  mets.dspaceAIP.ingest.crosswalk.DC = QDC
 *  mets.dspaceAIP.ingest.crosswalk.DSpaceDepositLicense = NULLSTREAM
 *
 *  # Option to save METS manifest in the item: (default is false)
 *  mets.submission.preserveManifest = false
 *
 *  # local copies of XML schema documents to save time on ingest:
 *  # file is relative to ${dspace.dir}/config/crosswalks
 *  mets.xsd.{prefix} = {namespaceURI} {file}
 *  mets.xsd.mets = http://www.loc.gov/METS/  mets.xsd
 *
 *
 * @author Larry Stone
 * @version $Revision: 1.1 $
 *
 * @see AbstractMETSDisseminator
 * @see AIPIngester
 * @see org.dspace.content.packager.METSManifest
 */
public class DSpaceAIPIngester
       extends AbstractMETSIngester
       implements AIPIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceAIPIngester.class);

    // standard URI of METS namespace
    private static final String METS_NS_URI = METSManifest.metsNS.getURI();

    // standard URI of xlink namespace
    private static final String XLINK_NS_URI = METSManifest.xlinkNS.getURI();

    /**
     * Ensure it's an AIP generated by the complementary AIP disseminator.
     */
    void checkManifest(METSManifest manifest)
        throws MetadataValidationException
    {
        String profile = manifest.getProfile();
        if (profile == null)
            throw new MetadataValidationException("Cannot accept METS with no PROFILE attribute!");
        else if (!profile.equals(DSpaceAIPDisseminator.PROFILE_1_0))
            throw new MetadataValidationException("METS has unacceptable PROFILE attribute, profile="+profile);
    }

    /**
     * Subclass-specific rules on accounting for package files:
     * If this is an internal AIP, there is no "package", so disregard
     * file checking.  On an external AIP, the built-in mechanisms
     * that recognize Bitstream and metadata files will account for all
     * AIP files.
     */
    public void checkPackageFiles(Set packageFiles, Set missingFiles,
                                  METSManifest manifest, PackageParameters params)
        throws PackageValidationException, CrosswalkException
    {
        // an internal AIP may end up with references to files, but no
        // conventional "package" so they appear missing; thus, ignore them.
        if (params.getBooleanProperty("internal", false))
            missingFiles.clear();
    }

    /**
     * Choose DMD section(s) to crosswalk.
     * <p>
     * The algorithm is:<br>
     * 1. Find DIM (preferably) or MODS as primary DMD.<br>
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

        // DIM is preferred for AIP
        for (int i = 0; i < dmds.length; ++i)
            if ("DIM".equals(manifest.getMdType(dmds[i])))
                found = i;

        // MODS is acceptable otehrwise..
        if (found == -1)
        {
            for (int i = 0; i < dmds.length; ++i)
                if ("MODS".equals(manifest.getMdType(dmds[i])))
                    found = i;
        }

        String groupID = null;
        if (found >= 0)
        {
            manifest.crosswalkItemDmd(context, dso, dmds[found], callback);
            groupID = dmds[found].getAttributeValue("GROUPID");

            if (groupID != null)
            {
                for (int i = 0; i < dmds.length; ++i)
                {
                    String g = dmds[i].getAttributeValue("GROUPID");
                    if (g != null && !g.equals(groupID))
                        manifest.crosswalkItemDmd(context, dso, dmds[i], callback);
                }
            }
        }

        // otherwise take the first.  Don't xwalk more than one because
        // each xwalk _adds_ metadata, and could add duplicate fields.
        else if (dmds.length > 0)
        {
            manifest.crosswalkItemDmd(context, dso, dmds[0], callback);
        }

        // it's an error if there is nothing to crosswalk:
        else
            throw new MetadataValidationException("DSpaceAIPIngester: Could not find an acceptable object-wide DMD section in manifest.");
    }


    /**
     * Ignore license when restoring an internal AIP, since it should
     * be a bitstream in the AIP already.
     * Otherwise:  Check item for license first; then, take deposit
     * license supplied by explicit argument next, else use collection's
     * default deposit license.
     * Normally the rightsMD crosswalks should provide a license.
     */
    public void addLicense(Context context, Item item, String license,
                                    Collection collection, PackageParameters params)
        throws PackageValidationException,
               AuthorizeException, SQLException, IOException
    {
        // only add deposit license if there isn't one in the object,
        // and it's not a restoration of an "internal" AIP:
        if (!params.getBooleanProperty("internal", false) &&
            PackageUtils.findDepositLicense(context, item) == null)
            PackageUtils.addDepositLicense(context, license, item, collection);
    }

    // last change to fix up Item.
    /**
     * Hook to post-process a new submission.
     */
    public void finishObject(Context context, DSpaceObject dso)
        throws PackageValidationException, CrosswalkException,
         AuthorizeException, SQLException, IOException
    {
        // nothing to do.
    }

    /**
     * Nothing extra to do to bitstream after ingestion.
     */
    public void finishBitstream(Context context,
                                                Bitstream bs,
                                                Element mfile,
                                                METSManifest manifest,
                                                PackageParameters params)
        throws MetadataValidationException, SQLException, AuthorizeException, IOException
    {
        // nothing to do.
    }

    /**
     * Return the type of DSpaceObject in this package; it is
     * in the TYPE attribute of the mets:mets element.
     */
    public int getObjectType(METSManifest manifest)
        throws PackageValidationException
    {
        Element mets = manifest.getMets();
        String typeStr = mets.getAttributeValue("TYPE");
        if (typeStr == null || typeStr.length() == 0)
            throw new PackageValidationException("Manifest is missing the required mets@TYPE attribute.");
        if (typeStr.startsWith("DSpace "))
            typeStr = typeStr.substring(7);
        int type = Constants.getTypeID(typeStr);
        if (type < 0)
            throw new PackageValidationException("Manifest has unrecognized value in mets@TYPE attribute: "+typeStr);
        return type;
    }

    /**
     * Interpret a Bitstream URI in the manifest.  If this is an
     * "internal" AIP, it refers directly to a file in the assetstore.
     * Otherwise, it will be match the relative path of an entry in the package.
     */
    public Bitstream resolveBitstream(Context context, String path,
                                      Element mfile, Map packageFiles,
                                      PackageParameters params)
        throws SQLException, PackageValidationException
    {
        // to ingest an internal AIP, just hook up the bitstreams
        // referenced by bitstream URIs --
        if (params.getBooleanProperty("internal", false))
        {
            try
            {
                Bitstream result = Bitstream.dereferenceAbsoluteURI(context, new URI(path));
                if (result == null)
                    throw new PackageValidationException("Package refers to a Bitstream that cannot be found (check assetstore), URI="+path);
                else if (result.isDeleted())
                    throw new PackageValidationException("Package refers to a Bitstream that has been deleted: "+result);
                return result;
            }
            catch (URISyntaxException e)
            {
                log.error("bad bitstream path URI: ", e);
                return null;
            }
        }

        // in a conventional package, content files are in the Zip and
        // were already put into bitstreams as the Zip was read:
        else if (packageFiles != null && packageFiles.containsKey(path))
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

    /**
     * Name used to distinguish DSpace Configuration entries for this subclass.
     */
    public String getConfigurationName()
    {
        return "dspaceAIP";
    }

    /**
     * These inner classes are used by probe() and getHandle():
     *
     * ProbeFailedException -- Marker to signal a probe that did not
     *   find the right kind of METS.
     */
    class ProbeFailedException
        extends SAXException
    {
        public ProbeFailedException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Marker to signal that probe found a conforming AIP.
     */
    class ProbeSucceededException
        extends SAXException
    {
        public ProbeSucceededException(String msg)
        {
            super(msg);
        }
    }

    /**
     * We use SAX for quicker parsing in the following methods that just
     * scan the AIP to extract some single bit of information.  Most applications
     * only call for one of these so it isn't worth caching all results.
     */

    /**
     * SAX handler framework for all probes
     */
    abstract class ProbeManifestHandler
        extends DefaultHandler
    {
        // statement of what this handler was seeking, for diagnostic
        abstract String lookingFor();

        public void error(SAXParseException exception)
            throws SAXException
        {
            throw new ProbeFailedException("error");
        }

        public void fatalError(SAXParseException exception)
            throws SAXException
        {
            throw new ProbeFailedException("fatalError");
        }
    }

    /**
     * Handler to get OBJID attribute from root <mets> element
     * PROFILE attribute must also match AIP profile.
     */
    class AIPGetObjidHandler
        extends ProbeManifestHandler
    {
        String lookingFor()
        {
            return "mets:mets element with OBJID attribute";
        }

        // this should only ever get called once, for the root
        // element.  that's enough for us to judge if the
        // document is an AIP or not.
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            // if it's a METS root element and profile matches,
            // then assume it's an AIP manifest.
            if (localName.equals("mets") && namespaceURI.equals(METS_NS_URI))
            {
                String profile = atts.getValue("PROFILE");

                if (profile == null)
                {
                    log.warn("Got METS record without a PROFILE attribute.");
                    throw new ProbeFailedException("NO PROFILE");
                }

                // upon success, stash Handle in the exception's message.
                else if (profile.equals(DSpaceAIPDisseminator.PROFILE_1_0))
                {
                    String objid = atts.getValue("OBJID");
                    if (objid == null)
                        objid = "";
                    else if (objid.startsWith("hdl:"))
                        objid = objid.substring(4);
                    if (log.isDebugEnabled())
                        log.debug("Got METS with AIP Profile, throwing ProbeSucceededException.");
                    throw new ProbeSucceededException(objid);
                }
                else
                {
                    if (log.isDebugEnabled())
                        log.debug("Rejecting METS with non-AIP profile, PROFILE="+profile);
                    throw new ProbeFailedException("WRONG PROFILE");
                }
            }
            else
            {
                if (log.isDebugEnabled())
                    log.debug("Rejecting element NS="+namespaceURI+", localname="+localName);
                throw new ProbeFailedException("NOT METS");
            }
        }
    }

    /**
     * Handler get Created Date, i.e. CREATEDATE attr from <metsHdr> element
     */
    class AIPCreateDateHandler
        extends ProbeManifestHandler
    {
        // statement of what this handler was seeking, for diagnostic
        String lookingFor()
        {
            return "mets:metsHdr element with CREATEDATE attribute";
        }

        // this should only ever get called once, for the root
        // element.  that's enough for us to judge if the
        // document is an AIP or not.
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            // ignore root element, since we're looking for its first
            // child, the metsHdr.
            if (localName.equals("mets") && namespaceURI.equals(METS_NS_URI))
                return;

            // if start of <metsHdr>, grab the CREATEDATE attr.
            else if (localName.equals("metsHdr") && namespaceURI.equals(METS_NS_URI))
            {
                String cd = atts.getValue("CREATEDATE");
                if (cd == null)
                {
                    log.warn("Got METS record without a CREATEDATE attribute.");
                    throw new ProbeFailedException("NO CREATEDATE");
                }

                // upon success, stash value in the exception's message.
                else
                {
                    throw new ProbeSucceededException(cd);
                }
            }

            // if no metsHdr, it's an error.
            else
            {
                if (log.isDebugEnabled())
                    log.debug("Rejecting element NS="+namespaceURI+", localname="+localName);
                throw new ProbeFailedException("NOT METS");
            }
        }
    }

    /**
     * Handler to look for handle of parent object in the AIP.
     * This will be found in a structmap of one
     * div, which contains an mptr indicating the Handle of the parent
     * of this object in the archive.  The div has a unique TYPE attribute
     * value, "AIP Parent Link", and the mptr has a LOCTYPE of "HANDLE"
     * and an xlink:href containing the raw Handle value.
     */
    class AIPParentHandleHandler
        extends ProbeManifestHandler
    {
        // are we within a <div> with the right TYPE to be an AIP parent link?
        boolean inParentDiv = false;

        String lookingFor()
        {
            return "mets:structMap/mets:div/mets:mptr element with div@type==\""+DSpaceAIPDisseminator.PARENT_DIV_TYPE+"\"";
        }

        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            if (localName.equals("div") && namespaceURI.equals(METS_NS_URI))
            {
                String type = atts.getValue("TYPE");
                inParentDiv = (type != null && type.equals(DSpaceAIPDisseminator.PARENT_DIV_TYPE));
            }
            else if (inParentDiv && localName.equals("mptr") && namespaceURI.equals(METS_NS_URI))
            {
                String ltype = atts.getValue("LOCTYPE");
                if (ltype != null && ltype.equalsIgnoreCase("HANDLE"))
                {
                    String href = atts.getValue(XLINK_NS_URI, "href");
                    if (href != null)
                        throw new ProbeSucceededException(href);
                    else
                        throw new ProbeFailedException("NO xlink:href in Parent div/mptr, although LOCTYPE==HANDLE.");
                }
                else
                    throw new ProbeFailedException("NO LOCTYPE==HANDLE in Parent div/mptr.");
            }
        }
    }

    /**
     * Wrapper for InputStream that lets an application "peek" at the
     * first characters to be read, and then put them back so another
     * reader will see them as if they hadn't been already read.  This
     * is used to probe a stream to test if e.g. it starts with the XML
     * header, before turning it over to a parser that expects the whole
     * stream from the beginning.
     */
    class PeekableInputStream
        extends FilterInputStream
    {
        private byte peekBuf[] = null;
        private int peekStart = 0;
        private int peekLen = 0;

        public PeekableInputStream(InputStream wrapped)
        {
            super(wrapped);
        }

        // must call this before any read() calls.
        public int peek(byte buf[])
            throws IOException
        {
            if (peekBuf != null)
                throw new IOException("Can only call peek() once, before reading.");
            peekBuf = buf;
            peekLen = in.read(peekBuf);
            peekStart = 0;
            return peekLen;
        }

        public int read()
            throws IOException
        {
            if (peekBuf != null && peekStart < peekBuf.length)
            {
                int result = peekBuf[peekStart];
                if (++peekStart >= peekLen)
                    peekBuf = null;
                return result;
            }
            else
                return in.read();
        }

        public int read(byte[] b)
            throws IOException
        {
            if (peekBuf != null && peekStart < peekLen)
                return read(b, 0, b.length);
            else
                return in.read(b);
        }

        public int read(byte[] b, int off, int len)
            throws IOException
        {
            if (peekBuf != null && peekStart < peekLen)
            {
                int result = 0;
                for (int i = off; i < len; ++i)
                {
                    int c = read();
                    if (c >= 0)
                    {
                        b[i] = (byte)c;
                        ++result;
                    }
                }
                return result;
            }
            else
                return in.read(b, off, len);
        }
    }

    /**
     * Test whether a stream contains an AIP METS document.
     * This needs to be as efficient as possible since it gets called
     * for every file in the asset store, when crawling over all bitstreams
     * to find the internal AIPs to rebuild an archive.
     * @return true if stream contains AIP METS manifest.
     */
    public boolean probe(Context context, InputStream is, PackageParameters params)
    {
        try
        {
            // 1. Check that head of file is "<?xml"
            PeekableInputStream pis = new PeekableInputStream(is);
            byte head[] = new byte[5];
            int headLen = pis.peek(head);
            if (headLen <= 0)
            {
                if (log.isDebugEnabled())
                    log.debug("Got an empty stream.");
                return false;
            }
            else
            {
                String headStr = new String(head, 0, headLen);
                if (!headStr.equals("<?xml"))
                {
                    if (log.isDebugEnabled())
                        log.debug("Does not start with XML header: \""+headStr+"\"");
                    return false;
                }
                 
                // 2. If head looks like XML, actually try to parse the root elt.
                startManifestParse(pis, new AIPGetObjidHandler());
                return false;
            }
        }
        catch (IOException e)
        {
            log.error("Got IOException while parsing XML in probe(): ", e);
            return false;
        }
        catch (ProbeSucceededException e)
        {
            return true;
        }
        catch (ProbeFailedException e)
        {
            log.debug("ProbeFailedException: ",e);
            return false;
        }
    }

    /**
     * Scan a manifest and return the Handle of the object it is about.
     * Handle is a bare string like "1721.2/13", not a URN.
     * @return handle or null if none is found.
     */
    public String getHandle(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        try
        {
            startManifestParse(in, new AIPGetObjidHandler());
        }
        catch (ProbeSucceededException e)
        {
            return e.getMessage();
        }
        catch (ProbeFailedException e)
        {
            return null;
        }
        return null;
    }

    /**
     * Scan a manifest and return the Handle of the parent of the object it is about.
     * Handle is a bare string like "1721.2/13", not a URN.
     * @return parent handle or null if none is found.
     */
    public String getParentHandle(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        String result = null;

        try
        {
            startManifestParse(in, new AIPParentHandleHandler());
        }
        catch (ProbeSucceededException e)
        {
            result = e.getMessage();
        }
        catch (ProbeFailedException e)
        {
            throw new PackageException("Parent handle not found in manifest.");
        }
        return result;
    }


    /**
     * Scan a manifest and return the CREATEDATE attribute (date AIP was created)
     * @return created date or null if none is found.
     */
    public Date getCreateDate(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        Date result = null;

        try
        {
            startManifestParse(in, new AIPCreateDateHandler());
        }
        catch (ProbeSucceededException e)
        {
            result = Utils.parseISO8601Date(e.getMessage());
            if (result == null)
                throw new PackageException("Failed to parse created date value: \""+e.getMessage()+"\"");
        }
        catch (ProbeFailedException e)
        {
            throw new PackageException("Create Date not found.");
        }
        return result;
    }

    /**
     * Start SAX parse of the manifest, throws a "success" exception
     * when it finds what the handler is looking for.  This *should*
     * never get to the end of the docuemnt so it logs error if it does.
     */
    private void startManifestParse(InputStream in, ProbeManifestHandler handler)
        throws ProbeSucceededException, ProbeFailedException
    {
        try
        {
            SAXParserFactory pf = SAXParserFactory.newInstance();
            SAXParser sp = pf.newSAXParser();
            XMLReader pkgReader = sp.getXMLReader();

            // XXX FIXME: should turn off validation here explicitly, but
            //  it seems to be off by default.

            pkgReader.setFeature("http://xml.org/sax/features/namespaces", true);
            pkgReader.setContentHandler(handler);
            pkgReader.setErrorHandler(handler);
            pkgReader.parse(new InputSource(in));

            // should NEVER get here when document is valid and has
            // the element we're looking for..
            log.error("Parsed whole document without finding "+handler.lookingFor());
        }
        // must catch and re-throw these, since they are subclass of SAXException
        catch (ProbeSucceededException e)
        {
            throw e;
        }
        catch (ProbeFailedException e)
        {
            throw e;
        }
        // log other errors for diagnostic purposes.
        catch (SAXException e)
        {
            log.error("Failed parsing XML when probing: "+e.toString(), e);
        }
        catch (IOException e)
        {
            log.error("Got IOException while parsing XML in probe(): "+e.toString(), e);
        }
        catch (ParserConfigurationException e)
        {
            log.error("SAX Parser configuration problem in probe(): "+e.toString(), e);
        }
    }

    /**
     * Gets list of bitstreams referenced by AIP manifest; this only
     * makes sense for an internal AIP.
     * @return array of bitstreams found, empty array if none.
     */
    public Bitstream[] getMetadataBitstreams(Context context, InputStream in, PackageParameters params)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        List<Bitstream> result = new ArrayList<Bitstream>();
        if (params.getBooleanProperty("internal", false))
        {
            METSManifest mf = METSManifest.create(in, false, getConfigurationName());
            for (Iterator mi = mf.getMdFiles().iterator(); mi.hasNext();)
            {
                Element mdRef = (Element)mi.next();
                Bitstream mbs = resolveBitstream(context,
                                  METSManifest.getFileName(mdRef),
                                  mdRef, null, params);
                if (mbs == null)
                    log.error("Cannot find metadata bitstream path="+METSManifest.getFileName(mdRef));
                else
                    result.add(mbs);
            }
        }
        return result.toArray(new Bitstream[result.size()]);
    }
}
