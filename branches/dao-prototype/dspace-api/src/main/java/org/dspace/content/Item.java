/*
 * Item.java
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
package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Browse;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing an item in DSpace. Note that everything is held in memory
 * until update() is called on the ItemDAO.
 * 
 * @author James Rutherford
 * @version $Revision$
 */
public class Item extends DSpaceObject
{
    private static Logger log = Logger.getLogger(Item.class);

    public static final String ANY = "*";

    protected Context context;
    protected ItemDAO dao;
    protected BundleDAO bundleDAO;
    protected CollectionDAO collectionDAO;
    protected CommunityDAO communityDAO;
    protected EPersonDAO epersonDAO;

    protected String identifier;
    protected boolean inArchive;
    protected boolean withdrawn;
    protected Date lastModified;

    /** The bundles in this item - kept in sync with DB */
    private List<Bundle> bundles;

    /** The Dublin Core metadata - a list of DCValue objects. */
    private List<DCValue> dublinCore;

    protected boolean metadataChanged;

    /**
     * True if the Dublin Core has changed since reading from the DB or the last
     * update()
     */
    private boolean dublinCoreChanged;

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    private boolean modified;

    /**
     * Construct an item with the given table row
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
    Item(Context context, TableRow row) throws SQLException
    {
        ourContext = context;
        itemRow = row;
        dublinCoreChanged = false;
        modified = false;
        dublinCore = new ArrayList<DCValue>();
        clearDetails();
 
        // Get Dublin Core metadata
        TableRowIterator tri = DatabaseManager.queryTable(ourContext, "MetadataValue",
                "SELECT * FROM MetadataValue WHERE item_id= ? " +
                " ORDER BY metadata_field_id, place",
                itemRow.getIntColumn("item_id"));

        dao = ItemDAOFactory.getInstance(context);
        bundleDAO = BundleDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
        communityDAO = CommunityDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);

        identifiers = new ArrayList<ExternalIdentifier>();
        bundles = new ArrayList<Bundle>();
        metadata = new ArrayList<DCValue>();
        metadataChanged = false;

        context.cache(this, id);
    }

    /**
     * Find out if the item is part of the main archive
     * 
     * @return true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return inArchive;
    }

    /**
     * Only <code>WorkflowItem.archive()</code> should really set this.
     * 
     * @param inArchive new value for the flag
     */
    public void setArchived(boolean inArchive)
    {
        TableRow row = DatabaseManager.create(context, "item");
        Item i = new Item(context, row);

        // Call update to give the item a last modified date. OK this isn't
        // amazingly efficient but creates don't happen that often.
        context.setIgnoreAuthorization(true);
        i.update();
        context.setIgnoreAuthorization(false);

        context.addEvent(new Event(Event.CREATE, Constants.ITEM, i.getID(), null));

        log.info(LogManager.getHeader(context, "create_item", "item_id="
                + row.getIntColumn("item_id")));

        return i;
    }

    /**
     * Find out if the item has been withdrawn
     * 
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn()
    {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn)
    {
        this.withdrawn = withdrawn;
    }

    /**
     * Get the internal ID of this item. In general, this shouldn't be exposed
     * to users
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return itemRow.getIntColumn("item_id");
    }

    /**
     * @see org.dspace.content.DSpaceObject#getHandle()
     */
    public String getHandle()
    {
        if(handle == null) {
        	try {
				handle = HandleManager.findHandle(this.ourContext, this);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
        }
    	return handle;
    }

    /**
     * Find out if the item is part of the main archive
     * 
     * @return true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return itemRow.getBooleanColumn("in_archive");
    }

    /**
     * Find out if the item has been withdrawn
     * 
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn()
    {
        return itemRow.getBooleanColumn("withdrawn");
    }

    /**
     * Get the date the item was last modified, or the current date if
     * last_modified is null
     * 
     * @return the date the item was last modified, or the current date if the
     *         column is null.
     */
    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date date)
    {
        // FIXME: Sanity checks!
        lastModified = date;
    }

    /**
     * Get the owning Collection for the item
     * 
     * @return Collection that is the owner of the item
     */
    public Collection getOwningCollection()
    {
        return owningCollection;
        modified = true;
    }

    /**
     * List the owning Collection for the item
     * 
     * @param c Collection
     */
    public void setOwningCollection(Collection owningCollection)
    {
        this.owningCollection = owningCollection;
        modified = true;
    }

    public void setOwningCollectionId(int owningCollectionId)
    {
        this.owningCollectionId = owningCollectionId;
    }

    public List<DCValue> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<DCValue> metadata)
    {
        this.metadata = metadata;
    }

    /**
     * Get metadata for the item in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>item.getMetadata("dc", "title", null, Item.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getMetadata("dc, "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element name. <code>Item.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            metadata must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>Item.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return metadata fields that match the parameters
     */
    public DCValue[] getMetadata(String schema, String element, String qualifier,
            String lang)
    {
        // Build up list of matching values
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : dublinCore)
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;
                copy.schema = dcv.schema;

                values.add(copy);
            }
        }

        return (DCValue[]) values.toArray(new DCValue[0]);
    }
    
    /**
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     */
    public DCValue[] getMetadata(String mdString)
    {
        StringTokenizer st = new StringTokenizer(mdString, ".");
        
        String[] tokens = { "", "", "" };
        int i = 0;
        while(st.hasMoreTokens())
        {
            tokens[i] = st.nextToken().toLowerCase().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];
        
        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = getMetadata(schema, element, qualifier, Item.ANY);
        }
        
        return values;
    }
    
    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifer name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String... values)
    {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;
            if (values[i] != null)
            {
                // remove control unicode char
                String temp = values[i].trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++)
                {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                        !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                        !String.valueOf(dcvalue[charPos]).equals("\n") &&
                        !String.valueOf(dcvalue[charPos]).equals("\r"))
                    {
                        dcvalue[charPos] = ' ';
                    }
                }
                dcv.value = String.valueOf(dcvalue);
            }
            else
            {
                dcv.value = null;
            }
            if (!metadata.contains(dcv))
            {
                metadata.add(dcv);
                metadataChanged = true;
            }
            addDetails(schema+"."+element+((qualifier==null)? "": "."+qualifier));

        }
    }

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an item.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>Item.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    public void clearMetadata(String schema, String element, String qualifier,
            String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : dublinCore)
        {
            if (!match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        Iterator<DCValue> i = metadata.iterator();
        for (DCValue dcv = i.next(); i.hasNext(); )
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                i.remove();
                metadataChanged = true;
            }
        }
    }

    /**
     * Get the e-person that originally submitted this item
     * 
     * @return the submitter
     */
    public EPerson getSubmitter()
    {
        return submitter;
    }

    /**
     * List the e-person that originally submitted this item. This is a public
     * method since it is handled by the WorkspaceItem class in the ingest
     * package. <code>update</code> must be called to write the change to the
     * database.
     * 
     * @param sub
     *            the submitter
     */
    public void setSubmitter(EPerson submitter)
    {
        this.submitter = submitter;
        modified = true;
    }

    public void setSubmitter(int submitterId)
    {
        List<Collection> collections = new ArrayList<Collection>();

        submitter = epersonDAO.retrieve(submitterId);
    }

    /**
     * Get the communities this item is in. Returns an unordered array of the
     * communities that house the collections this item is in, including parent
     * communities of the owning collections.
     * 
     * @return the communities this item is in.
     * @throws SQLException
     */
    public Community[] getCommunities() throws SQLException
    {
        List<Community> communities = new ArrayList<Community>();

        // Get community table rows
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,"community",
                        "SELECT community.* FROM community, community2item " +
                        "WHERE community2item.community_id=community.community_id " +
                        "AND community2item.item_id= ? ",
                        itemRow.getIntColumn("item_id"));

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community owner = (Community) ourContext.fromCache(Community.class,
                    row.getIntColumn("community_id"));

            if (owner == null)
            {
                owner = new Community(ourContext, row);
            }

            communities.add(owner);

            // now add any parent communities
            Community[] parents = owner.getAllParents();

            for (int i = 0; i < parents.length; i++)
            {
                communities.add(parents[i]);
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get the bundles in this item.
     * 
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles()
    {
    	if (bundles == null)
    	{
                bundles = new ArrayList<Bundle>();
    		// Get bundles
    		TableRowIterator tri = DatabaseManager.queryTable(ourContext, "bundle",
    					"SELECT bundle.* FROM bundle, item2bundle WHERE " +
    					"item2bundle.bundle_id=bundle.bundle_id AND " +
    					"item2bundle.item_id= ? ",
                        itemRow.getIntColumn("item_id"));

    public void setBundles(List<Bundle> bundles)
    {
        this.bundles = bundles;
    }

    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     * 
     * @param name
     *            name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     * 
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles(String name)
    {
        List<Bundle> matchingBundles = new ArrayList<Bundle>();

        // now only keep bundles with matching names
        Bundle[] bunds = getBundles();
        for (int i = 0; i < bunds.length; i++ )
        {
            if (name.equals(bundle.getName()))
            {
                tmp.add(bundle);
            }
        }
        return (Bundle[]) tmp.toArray(new Bundle[0]);
    }

    /**
     * Create a bundle in this item, with immediate effect
     * 
     * @param name
     *            bundle name (ORIGINAL/TEXT/THUMBNAIL)
     * @return the newly created bundle
     * @throws AuthorizeException
     */
    public Bundle createBundle(String name) throws AuthorizeException
    {
        if ((name == null) || "".equals(name))
        {
            throw new RuntimeException("Bundle must be created with non-empty name");
        }

        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

        // FIXME: Ideally, we wouldn't reach into the DAO layer here, we'd just
        // let everything fall into place when item.update() is called, but I
        // haven't quite worked out the logistics of that yet. Basically, we
        // need the behaviour to propogate downwards (ie: through bundles into
        // bitstreams) because bitstreams don't yet use DAOs, they do the
        // creation immediately. Once Bitstreams use DAOs, we *should* be able
        // to replace this code with the following:
        /*
        Bundle b = new Bundle();
        b.setName(name);
        addBundle(b);
        return b;
        */
        Bundle b = bundleDAO.create();
        b.setName(name);
        bundleDAO.update(b);

        addBundle(b);

        return b;
    }

    /**
     * Add an existing bundle to this item. This has immediate effect.
     * 
     * @param b
     *            the bundle to add
     */
    public void addBundle(Bundle b) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        // Check it's not already there
        for (Bundle bundle : getBundles())
        {
            if ((b.getName() == bundle.getName()) ||
                (b.getID() == bundle.getID()))
            {
                // Bundle is a duplicate, do nothing
                return;
            }
        }

        bundles.add(b);

        ourContext.addEvent(new Event(Event.ADD, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));
    }

    /**
     * Remove a bundle. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * FIXME: Will this ever not be the case? Can multiple Items own the same
     * Bundle?
     * 
     * @param b
     *            the bundle to remove
     */
    public void removeBundle(Bundle b) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        Iterator<Bundle> i = bundles.iterator();
        while (i.hasNext())
        {
            if (i.next().getID() == b.getID())
            {
                i.remove();
            }
        }

        // Remove mapping from DB
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM item2bundle WHERE item_id= ? " +
                "AND bundle_id= ? ",
                getID(), b.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));

        // If the bundle is orphaned, it's removed
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM item2bundle WHERE bundle_id= ? ",
                b.getID());

        if (!tri.hasNext())
        {
            //make the right to remove the bundle explicit because the implicit
            // relation
            //has been removed. This only has to concern the currentUser
            // because
            //he started the removal process and he will end it too.
            //also add right to remove from the bundle to remove it's
            // bitstreams.
            AuthorizeManager.addPolicy(ourContext, b, Constants.DELETE,
                    ourContext.getCurrentUser());
            AuthorizeManager.addPolicy(ourContext, b, Constants.REMOVE,
                    ourContext.getCurrentUser());

            // The bundle is an orphan, delete it
            b.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Create a single bitstream in a new bundle. Provided as a convenience
     * method for the most common use.
     * 
     * @param is
     *            the stream to create the new bitstream from
     * @param name
     *            is the name of the bundle (ORIGINAL, TEXT, THUMBNAIL)
     * @return Bitstream that is created
     * @throws AuthorizeException
     * @throws IOException
     */
    public Bitstream createSingleBitstream(InputStream is, String name)
            throws AuthorizeException, IOException
    {
        // Authorisation is checked by methods below
        // Create a bundle
        Bundle bnd = createBundle(name);
        Bitstream bitstream = bnd.createBitstream(is);
        addBundle(bnd);

        // FIXME: Create permissions for new bundle + bitstream
        return bitstream;
    }

    /**
     * Convenience method, calls createSingleBitstream() with name "ORIGINAL"
     * 
     * @param is
     *            InputStream
     * @return created bitstream
     * @throws AuthorizeException
     * @throws IOException
     */
    public Bitstream createSingleBitstream(InputStream is)
            throws AuthorizeException, IOException
    {
        return createSingleBitstream(is, "ORIGINAL");
    }

    /**
     * Get all non-internal bitstreams in the item. This is mainly used for
     * auditing for provenance messages and adding format.* DC values. The order
     * is indeterminate.
     * 
     * @return non-internal bitstreams.
     */
    public Bitstream[] getNonInternalBitstreams()
    {
        List<Bitstream> bitstreamList = new ArrayList<Bitstream>();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        for (Bundle b : getBundles())
        {
            Bitstream[] bitstreams = b.getBitstreams();

            for (int j = 0; j < bitstreams.length; j++)
            {
                if (!bitstreams[j].getFormat().isInternal())
                {
                    // Bitstream is not of an internal format
                    bitstreamList.add(bitstreams[j]);
                }
            }
        }

        Bitstream[] bsArray = new Bitstream[bitstreamList.size()];
        bsArray = (Bitstream[]) bitstreamList.toArray(bsArray);

        return bsArray;
    }

    /**
     * Store a copy of the license a user granted in this item.
     * 
     * @param license
     *            the license the user granted
     * @param eperson
     *            the eperson who granted the license
     * @throws IOException
     * @throws AuthorizeException
     */
    public void licenseGranted(String license, EPerson eperson)
            throws IOException, AuthorizeException
    {
        // Put together text to store
        String licenseText = "License granted by " + eperson.getFullName()
                + " (" + eperson.getEmail() + ") on "
                + DCDate.getCurrent().toString() + " (GMT):\n\n" + license;

        // Store text as a bitstream
        byte[] licenseBytes = licenseText.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(licenseBytes);
        Bitstream b = createSingleBitstream(bais, "LICENSE");

        // Now set the format and name of the bitstream
        b.setName("license.txt");
        b.setSource("Written by org.dspace.content.Item");

        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context,
                "License");
        b.setFormat(bf);

        b.update();
    }

    /**
     * Remove all licenses from an item - it was rejected
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeLicenses() throws AuthorizeException,
            IOException
    {
        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context,
                "License");
        int licensetype = bf.getID();

        // search through bundles, looking for bitstream type license
        for (Bundle bundle : getBundles())
        {
            boolean removethisbundle = false;

            for (Bitstream bitstream : bundle.getBitstreams())
            {
                BitstreamFormat format = bitstream.getFormat();

                if (format.getID() == licensetype)
                {
                    removethisbundle = true;
                }
            }

            // probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            if (removethisbundle)
            {
                removeBundle(bundle);
            }
        }
    }

    /**
     * Update the item "in archive" flag and Dublin Core metadata in the
     * database
     * 
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        // only do write authorization if user is not an editor
        if (!canEdit())
        {
            AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        }

        log.info(LogManager.getHeader(ourContext, "update_item", "item_id="
                + getID()));

        // Set the last modified date
        itemRow.setColumn("last_modified", new Date());

        // Set sequence IDs for bitstreams in item
        int sequence = 0;
        Bundle[] bunds = getBundles();

        // find the highest current sequence number
        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() > sequence)
                {
                    sequence = streams[k].getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;

        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() < 0)
                {
                    streams[k].setSequenceID(sequence);
                    sequence++;
                    streams[k].update();
                }
            }
        }

        // Make sure that withdrawn and in_archive are non-null
        if (itemRow.isColumnNull("in_archive"))
        {
            itemRow.setColumn("in_archive", false);
        }

        if (itemRow.isColumnNull("withdrawn"))
        {
            itemRow.setColumn("withdrawn", false);
        }

        // Map counting number of values for each element/qualifier.
        // Keys are Strings: "element" or "element.qualifier"
        // Values are Integers indicating number of values written for a
        // element/qualifier
        Map<String,Integer> elementCount = new HashMap<String,Integer>();

        DatabaseManager.update(ourContext, itemRow);

        // Redo Dublin Core if it's changed
        if (dublinCoreChanged)
        {
            // Remove existing DC
            removeMetadataFromDatabase();

            // Add in-memory DC
            for (DCValue dcv : dublinCore)
            {
                // Get the DC Type
                int schemaID;
                MetadataSchema schema = MetadataSchema.find(ourContext,dcv.schema);
                if (schema == null) {
                    schemaID = MetadataSchema.DC_SCHEMA_ID;
                } else {
                    schemaID = schema.getSchemaID();
                }

                MetadataField field = MetadataField.findByElement(ourContext,
                        schemaID, dcv.element, dcv.qualifier);

                if (field == null)
                {
                    // Bad DC field, log and throw exception
                    log.warn(LogManager
                            .getHeader(ourContext, "bad_dc",
                                    "Bad DC field. SchemaID="+String.valueOf(schemaID)
                                            + ", element: \""
                                            + ((dcv.element == null) ? "null"
                                                    : dcv.element)
                                            + "\" qualifier: \""
                                            + ((dcv.qualifier == null) ? "null"
                                                    : dcv.qualifier)
                                            + "\" value: \""
                                            + ((dcv.value == null) ? "null"
                                                    : dcv.value) + "\""));

                    throw new SQLException("bad_dublin_core "
                            + "SchemaID="+String.valueOf(schemaID)+", "
                            + dcv.element
                            + " " + dcv.qualifier);
                }

                // Work out the place number for ordering
                int current = 0;

                // Key into map is "element" or "element.qualifier"
                String key = dcv.element
                        + ((dcv.qualifier == null) ? "" : ("." + dcv.qualifier));

                Integer currentInteger = (Integer) elementCount.get(key);

                if (currentInteger != null)
                {
                    current = currentInteger.intValue();
                }

                current++;
                elementCount.put(key, new Integer(current));

                // Write DCValue
                MetadataValue metadata = new MetadataValue();
                metadata.setItemId(getID());
                metadata.setFieldId(field.getFieldID());
                metadata.setValue(dcv.value);
                metadata.setLanguage(dcv.language);
                metadata.setPlace(current);
                metadata.create(ourContext);
            }

            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.ITEM, getID(), getDetails()));
            dublinCoreChanged = false;
            clearDetails();
        }

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null));
            modified = false;
        }
    }

    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    @Deprecated
    public void withdraw() throws AuthorizeException, IOException
    {
        String timestamp = DCDate.getCurrent().toString();

        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getID() + ")\n";
        }

        // Check permission. User either has to have REMOVE on owning collection
        // or be COLLECTION_EDITOR of owning collection
        if (AuthorizeManager.authorizeActionBoolean(ourContext,
                getOwningCollection(), Constants.COLLECTION_ADMIN)
                || AuthorizeManager.authorizeActionBoolean(ourContext,
                        getOwningCollection(), Constants.REMOVE))
        {
            // authorized
        }
        else
        {
            throw new AuthorizeException(
                    "To withdraw item must be COLLECTION_ADMIN or have REMOVE authorization on owning Collection");
        }

        // Set withdrawn flag. timestamp will be set; last_modified in update()
        itemRow.setColumn("withdrawn", true);

        // in_archive flag is now false
        itemRow.setColumn("in_archive", false);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        String prov = "Item withdrawn by " + e.getFullName() + " ("
                + e.getEmail() + ") on " + timestamp + "\n"
                + "Item was in collections:\n" + collectionProv
                + InstallItem.getBitstreamProvenanceMessage(this);

        addDC("description", "provenance", "en", prov);

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "WITHDRAW"));

        // and all of our authorization policies
        // FIXME: not very "multiple-inclusion" friendly
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Write log
        log.info(LogManager.getHeader(ourContext, "withdraw_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }

    /**
     * Reinstate a withdrawn item
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    @Deprecated
    public void reinstate() throws AuthorizeException, IOException
    {
        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getID() + ")\n";
            AuthorizeManager.authorizeAction(ourContext, colls[i],
                    Constants.ADD);
        }

        // Clear withdrawn flag
        itemRow.setColumn("withdrawn", false);

        // in_archive flag is now true
        itemRow.setColumn("in_archive", true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        String prov = "Item reinstated by " + e.getFullName() + " ("
                + e.getEmail() + ") on " + timestamp + "\n"
                + "Item was in collections:\n" + collectionProv
                + InstallItem.getBitstreamProvenanceMessage(this);

        addDC("description", "provenance", "en", prov);

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "REINSTATE"));

        // authorization policies
        if (colls.length > 0)
        {
            // FIXME: not multiple inclusion friendly - just apply access
            // policies from first collection
            // remove the item's policies and replace them with
            // the defaults from the collection
            inheritCollectionDefaultPolicies(colls[0]);
        }

        // Write log
        log.info(LogManager.getHeader(ourContext, "reinstate_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }

    /**
     * Return true if the given Collection 'owns' this item.
     * 
     * @param c Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Collection c)
    {
        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, getID(), getHandle()));

        log.info(LogManager.getHeader(ourContext, "delete_item", "item_id="
                + getID()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove from browse indices, if appropriate
        /** XXX FIXME
         ** Although all other Browse index updates are managed through
         ** Event consumers, removing an Item *must* be done *here* (inline)
         ** because otherwise, tables are left in an inconsistent state
         ** and the DB transaction will fail.
         ** Any fix would involve too much work on Browse code that
         ** is likely to be replaced soon anyway.   --lcs, Aug 2006
         **/
        if (isArchived())
        {
            // Remove from Browse indices
            Browse.itemRemoved(ourContext, getID());
        }
        else if (owningCollection != null)
        {
            removeBundle(bunds[i]);
        }

        // remove all of our authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Finally remove item row
        DatabaseManager.delete(ourContext, itemRow);
    }
    
    /**
     * Remove item and all its sub-structure from the context cache.
     * Useful in batch processes where a single context has a long,
     * multi-item lifespan
     */
    public void decache() throws SQLException
    {
        // Remove item and it's submitter from cache
        ourContext.removeCached(this, getID());
        if (submitter != null)
        {
        	ourContext.removeCached(submitter, submitter.getID());
        }
        // Remove bundles & bitstreams from cache if they have been loaded
        if (bundles != null)
        {
        	Bundle[] bunds = getBundles();
        	for (int i = 0; i < bunds.length; i++)
        	{
        		ourContext.removeCached(bunds[i], bunds[i].getID());
        		Bitstream[] bitstreams = bunds[i].getBitstreams();
        		for (int j = 0; j < bitstreams.length; j++)
        		{
        			ourContext.removeCached(bitstreams[j], bitstreams[j].getID());
        		}
        	}
        }
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * @return <code>true</code> if object passed in represents the same item
     *         as this object
     */
    public boolean equals(DSpaceObject other)
    {
        if (this.getType() == other.getType())
        {
            if (this.getID() == other.getID())
            {
                return true;
            }
        }

        // not the owner
        return false;
    }

    /**
     * Return type found in Constants
     * 
     * @return int Constants.ITEM
     */
    public int getType()
    {
        return Constants.ITEM;
    }

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     * 
     * @param newpolicies -
     *            this will be all of the new policies for the item and its
     *            contents
     * @throws SQLException
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void replaceAllItemPolicies(List newpolicies) throws SQLException,
            AuthorizeException
    {
        // remove all our policies, add new ones
        AuthorizeManager.removeAllPolicies(context, this);
        AuthorizeManager.addPolicies(context, newpolicies, this);
    }

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
     * them with a new list of policies
     * 
     * @param newpolicies -
     *            this will be all of the new policies for the bundle and
     *            bitstream contents
     * @throws SQLException
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void replaceAllBitstreamPolicies(List newpolicies)
            throws SQLException, AuthorizeException
    {
        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                Bitstream mybitstream = bs[j];

                // change bitstream policies
                AuthorizeManager.removeAllPolicies(context, bs[j]);
                AuthorizeManager.addPolicies(context, newpolicies, bs[j]);
            }

            // change bundle policies
            AuthorizeManager.removeAllPolicies(context, mybundle);
            AuthorizeManager.addPolicies(context, newpolicies, mybundle);
        }
    }

    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     * 
     * @param g
     *            Group referenced by policies that needs to be removed
     * @throws SQLException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void removeGroupPolicies(Group g) throws SQLException
    {
        // remove Group's policies from Item
        AuthorizeManager.removeGroupPolicies(context, this, g);

        // remove all policies from bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                Bitstream mybitstream = bs[j];

                // remove bitstream policies
                AuthorizeManager.removeGroupPolicies(context, bs[j], g);
            }

            // change bundle policies
            AuthorizeManager.removeGroupPolicies(context, mybundle, g);
        }
    }

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
     * 
     * @param c
     *            Collection
     * @throws java.sql.SQLException
     *             if an SQL error or if no default policies found. It's a bit
     *             draconian, but default policies must be enforced.
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void inheritCollectionDefaultPolicies(Collection c)
            throws java.sql.SQLException, AuthorizeException
    {
        // remove the submit authorization policies
        // and replace them with the collection's default READ policies
        List policies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_ITEM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator i = policies.iterator();

        // MUST have default policies
        if (!i.hasNext())
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default item READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllItemPolicies(policies);

        policies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        i = policies.iterator();

        if (!i.hasNext())
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default bitstream READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllBitstreamPolicies(policies);
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * return TRUE if context's user can edit item, false otherwise
     * 
     * @return boolean true = current user can edit item
     */
    public boolean canEdit()
    {
        // can this person write to the item?
        if (AuthorizeManager.authorizeActionBoolean(context, this,
                Constants.WRITE))
        {
            return true;
        }

        // is this collection not yet created, and an item template is created
        if (getOwningCollection() == null)
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (getOwningCollection().canEditBoolean())
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (AuthorizeManager.authorizeActionBoolean(context,
                getOwningCollection(), Constants.COLLECTION_ADMIN))
        {
            return true;
        }

        return false;
    }
    public String getName()
    {
        DCValue t[] = getMetadata("dc", "title", null, Item.ANY);
        return (t.length >= 1) ? t[0].value : null;
    }
}
