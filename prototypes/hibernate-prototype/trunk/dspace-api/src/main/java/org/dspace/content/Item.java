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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Thumbnail;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.dao.BitstreamFormatDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory; // import
												// org.dspace.content.uri.ExternalIdentifier;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.event.Event;
import org.dspace.content.factory.BundleFactory;

/**
 * Class representing an item in DSpace. Note that everything is held in memory
 * until update() is called on the ItemDAO.
 * 
 * @author James Rutherford
 * @version $Revision$
 */
@Entity
public class Item extends DSpaceObject {

	/*----------------------- OLD FIELDS ------------------------*/
	private static Logger log = Logger.getLogger(Item.class);

	public static final String ANY = "*";

	private String identifier;
	private boolean inArchive;
	private boolean withdrawn;
	private Date lastModified;

	// protected int owningCollectionId;
	private Collection owningCollection;
	// protected int submitterId;
	private EPerson submitter;

	private List<Collection> collections;
	private List<Bundle> bundles;
	private List<MetadataValue> metadata;

	private boolean metadataChanged;

	/**
	 * True if anything else was changed since last update() (to drive event
	 * mechanism)
	 */
	private boolean modified;
	/*-----------------------------------------------------*/

	private Context context;

	public Item(Context context) {
		this.context = context;
		this.collections = new ArrayList<Collection>();
		this.bundles = new ArrayList<Bundle>();
		this.metadata = new ArrayList<MetadataValue>();
		metadataChanged = modified = false;
	}

	protected Item() {
		this.collections = new ArrayList<Collection>();
		this.bundles = new ArrayList<Bundle>();
		this.metadata = new ArrayList<MetadataValue>();
	}

	public Bundle createBundle() {
		Bundle bundle = BundleFactory.getInstance(context);
		bundle.setItem(this);
		getBundles().add(bundle);
		return bundle;
	}

	public void addBundle(Bundle b) throws AuthorizeException {
		// Check it's not already there
		for (Bundle bundle : getBundles()) {
			if ((b.getName().equals(bundle.getName()))
					|| (b.getId() == bundle.getId())) {
				// Bundle is a duplicate, do nothing
				return;
			}
		}

		AuthorizeManager.inheritPolicies(context, this, b);

		bundles.add(b);

		context.addEvent(new Event(Event.ADD, Constants.ITEM, getId(),
				Constants.BUNDLE, b.getId(), b.getName()));
	}

	public void removeBundle(Bundle b) throws AuthorizeException {
		AuthorizeManager.authorizeAction(context, this, Constants.REMOVE);

		log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
				+ getId() + ",bundle_id=" + b.getId()));

		Iterator<Bundle> i = bundles.iterator();
		while (i.hasNext()) {
			if (i.next().getId() == b.getId()) {
				i.remove();
			}
		}

		context.addEvent(new Event(Event.REMOVE, Constants.ITEM, getId(),
				Constants.BUNDLE, b.getId(), b.getName()));

	}

	@Transient
	protected String getidentifier() {
		return identifier;
	}

	protected void setidentifier(String identifier) {
		this.identifier = identifier;
	}

	@Column(name = "in_archive")
	public boolean isArchived() {
		return inArchive;
	}

	public void setArchived(boolean inArchive) {
		this.inArchive = inArchive;
	}

	@Column(name = "withdrawn")
	public boolean isWithdrawn() {
		return withdrawn;
	}

	public void setWithdrawn(boolean withdrawn) {
		this.withdrawn = withdrawn;
	}

	@Column(name = "last_modified")
	@Temporal(value=TemporalType.DATE)
	public Date getLastModified() {
		return lastModified;
	}

	/* Returns the owning collection of this item */
	@ManyToOne
	@JoinColumn(name = "owning_collection_id")
	public Collection getOwningCollection() {
		return owningCollection;
	}

	public void setOwningCollection(Collection owningCollection) {
		this.owningCollection = owningCollection;
	}

	@OneToMany
	@JoinTable(name = "item2metadatavalue")
	public List<MetadataValue> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<MetadataValue> metadata) {
		this.metadata = metadata;
	}

	@Transient
	public MetadataValue[] getMetadata(String schema, String element,
			String qualifier, String lang) {
		// Build up list of matching values
		List<MetadataValue> values = new ArrayList<MetadataValue>();

		for (MetadataValue mdv : metadata) {
			if (match(schema, element, qualifier, lang, mdv)) {
				values.add(mdv);
			}
		}

		return values.toArray(new MetadataValue[0]);
	}

	@Transient
	public MetadataValue[] getMetadata(String mdString) {
		StringTokenizer st = new StringTokenizer(mdString, ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (st.hasMoreTokens()) {
			tokens[i] = st.nextToken().toLowerCase().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		MetadataValue[] values;
		if ("*".equals(qualifier)) {
			values = getMetadata(schema, element, Item.ANY, Item.ANY);
		} else if ("".equals(qualifier)) {
			values = getMetadata(schema, element, null, Item.ANY);
		} else {
			values = getMetadata(schema, element, qualifier, Item.ANY);
		}

		return values;
	}

	public void addMetadata(MetadataField field, String lang, String... values) {
		for (String value : values) {
			if (value != null && !value.trim().equals("")) {
				MetadataValue mdv = new MetadataValue(field);
				mdv.setLanguage(lang);

				// remove control unicode char
				String temp = value.trim();
				char[] dcvalue = temp.toCharArray();
				for (int charPos = 0; charPos < dcvalue.length; charPos++) {
					if (Character.isISOControl(dcvalue[charPos])
							&& !String.valueOf(dcvalue[charPos]).equals(
									"\u0009")
							&& !String.valueOf(dcvalue[charPos]).equals("\n")
							&& !String.valueOf(dcvalue[charPos]).equals("\r")) {
						dcvalue[charPos] = ' ';
					}
				}
				mdv.setValue(String.valueOf(dcvalue));
				if (!metadata.contains(mdv)) {
					metadata.add(mdv);
					metadataChanged = true;
				}

				addDetails(field.getSchema().getName()
						+ "."
						+ field.getElement()
						+ ((field.getQualifier() == null) ? "" : "."
								+ field.getQualifier()));
			}
		}
	}

	public void clearMetadata(String schema, String element, String qualifier,
			String lang) {
		if (metadata.size() == 0) {
			return;
		}

		Iterator<MetadataValue> i = metadata.iterator();
		while (i.hasNext()) {
			if (match(schema, element, qualifier, lang, i.next())) {
				i.remove();
				metadataChanged = true;
			}
		}
	}

	public void setLastModified(Date date) {

		lastModified = date;
	}

	public void setSubmitter(EPerson submitter) {
		this.submitter = submitter;
		// submitterId = submitter.getId();
	}

	/* FIXME: responsabilità del dao prendere le info? */
	/*
	 * public void setSubmitter(int submitterId) { //
	 * setSubmitter(epersonDAO.retrieve(submitterId)); }
	 */@OneToOne
	public EPerson getSubmitter() {
		return submitter;
	}

	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "item")
	public List<Bundle> getBundles() {
		return bundles;
	}

	@Transient
	public List<Bundle> getBundles(String name) {
		List<Bundle> tmp = new ArrayList<Bundle>();
		for (Bundle bundle : getBundles()) {
			if (name.equals(bundle.getName())) {
				tmp.add(bundle);
			}
		}
		return tmp;
	}

	public void setBundles(List<Bundle> bundles) {
		this.bundles = bundles;
	}

	public Bundle createBundle(String name) throws AuthorizeException {
		if ((name == null) || "".equals(name)) {
			throw new RuntimeException(
					"Bundle must be created with non-empty name");
		}

		AuthorizeManager.authorizeAction(context, this, Constants.ADD);

		Bundle b = BundleFactory.getInstance(context);
		b.setName(name);

		addBundle(b);

		return b;
	}

	public Bitstream createSingleBitstream(InputStream is, String name)
			throws AuthorizeException, IOException {
		// Authorisation is checked by methods below
		// Create a bundle

		Bundle bundle = createBundle();

		Bitstream bitstream = bundle.createBitstream(is);

		addBundle(bundle);

		// FIXME: Create permissions for new bundle + bitstream
		return bitstream;
	}

	public Bitstream createSingleBitstream(InputStream is)
			throws AuthorizeException, IOException {
		return createSingleBitstream(is, "ORIGINAL");
	}

	@Transient
	public Bitstream[] getNonInternalBitstreams() {
		List<Bitstream> bitstreamList = new ArrayList<Bitstream>();

		// Go through the bundles and bitstreams picking out ones which aren't
		// of internal formats
		for (Bundle b : getBundles()) {
			for (Bitstream bitstream : b.getBitstreams()) {
				if (bitstream.getFormat().isInternal()) {
					// Bitstream is not of an internal format
					bitstreamList.add(bitstream);
				}
			}
		}

		Bitstream[] bsArray = new Bitstream[bitstreamList.size()];
		bsArray = (Bitstream[]) bitstreamList.toArray(bsArray);

		return bsArray;
	}

	public void licenseGranted(String license, EPerson eperson)
			throws IOException, AuthorizeException {
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

		/* FIXME: responsabilità? */
		// Find the License format
		/*
		 * BitstreamFormat bf = bitstreamFormatDAO
		 * .retrieveByShortDescription("License"); b.setFormat(bf);
		 * 
		 * bitstreamDAO.update(b);
		 */}

	public void removeDSpaceLicense() throws AuthorizeException, IOException {
		// get all bundles with name "LICENSE" (these are the DSpace license
		// bundles)
		List<Bundle> bunds = getBundles("LICENSE");

		for (Bundle bundle : bunds) {
			// FIXME: probably serious troubles with Authorizations
			// fix by telling system not to check authorization?
			removeBundle(bundle);
		}
	}

	/* FIXME: riscrivere in base alle nuove responsabilità */
	public void removeLicenses() throws AuthorizeException, IOException {
		// Find the License format
		/*
		 * BitstreamFormat bf = bitstreamFormatDAO
		 * .retrieveByShortDescription("License"); int licensetype = bf.getID(); //
		 * search through bundles, looking for bitstream type license for
		 * (Bundle bundle : getBundles()) { boolean removethisbundle = false;
		 * 
		 * for (Bitstream bitstream : bundle.getBitstreams()) { BitstreamFormat
		 * format = bitstream.getFormat();
		 * 
		 * if (format.getID() == licensetype) { removethisbundle = true; } } //
		 * probably serious troubles with Authorizations // fix by telling
		 * system not to check authorization? if (removethisbundle) {
		 * removeBundle(bundle); } }
		 */}

	/*
	 * FIXME: perchè chiederlo all'item e non chiedere alla collection se è
	 * owning dell'item?
	 */
	@Transient
	public boolean isOwningCollection(Collection c) {

		if (getOwningCollection().getId() > 0) {
			if (c.getId() == getOwningCollection().getId()) {
				return true;
			}
		} else if (owningCollection != null) {
			if (c.getId() == owningCollection.getId()) {
				return true;
			}
		}

		// not the owner
		return false;
	}

	@Transient
	public int getType() {
		return Constants.ITEM;
	}

	public void replaceAllItemPolicies(List newpolicies)
			throws AuthorizeException {
		// remove all our policies, add new ones
		AuthorizeManager.removeAllPolicies(context, this);
		AuthorizeManager.addPolicies(context, newpolicies, this);
	}

	public void replaceAllBitstreamPolicies(List newpolicies)
			throws AuthorizeException {
		// remove all policies from bundles, add new ones
		// Remove bundles
		for (Bundle mybundle : getBundles()) {
			for (Bitstream mybitstream : mybundle.getBitstreams()) {

				// change bitstream policies
				AuthorizeManager.removeAllPolicies(context, mybitstream);
				AuthorizeManager.addPolicies(context, newpolicies, mybitstream);
			}

			// change bundle policies
			AuthorizeManager.removeAllPolicies(context, mybundle);
			AuthorizeManager.addPolicies(context, newpolicies, mybundle);
		}
	}

	public void removeGroupPolicies(Group g) {
		// remove Group's policies from Item
		AuthorizeManager.removeGroupPolicies(context, this, g);

		// remove all policies from bundles

		for (Bundle mybundle : getBundles()) {
			for (Bitstream mybitstream : mybundle.getBitstreams()) {
				// remove bitstream policies
				AuthorizeManager.removeGroupPolicies(context, mybitstream, g);
			}

			// change bundle policies
			AuthorizeManager.removeGroupPolicies(context, mybundle, g);
		}
	}

	public void inheritCollectionDefaultPolicies(Collection c)
			throws AuthorizeException {
		// remove the submit authorization policies
		// and replace them with the collection's default READ policies
		List policies = AuthorizeManager.getPoliciesActionFilter(context, c,
				Constants.DEFAULT_ITEM_READ);

		// change the action to just READ
		// just don't call update on the resourcepolicies!!!
		Iterator i = policies.iterator();

		// MUST have default policies
		if (!i.hasNext()) {
			throw new RuntimeException("Collection " + c.getId()
					+ " has no default item READ policies");
		}

		while (i.hasNext()) {
			ResourcePolicy rp = (ResourcePolicy) i.next();
			rp.setAction(Constants.READ);
		}

		replaceAllItemPolicies(policies);

		policies = AuthorizeManager.getPoliciesActionFilter(context, c,
				Constants.DEFAULT_BITSTREAM_READ);

		// change the action to just READ
		// just don't call update on the resourcepolicies!!!
		i = policies.iterator();

		if (!i.hasNext()) {
			throw new RuntimeException("Collection " + c.getId()
					+ " has no default bitstream READ policies");
		}

		while (i.hasNext()) {
			ResourcePolicy rp = (ResourcePolicy) i.next();
			rp.setAction(Constants.READ);
		}

		replaceAllBitstreamPolicies(policies);
	}

	public boolean canEdit() {
		// can this person write to the item?
		if (AuthorizeManager.authorizeActionBoolean(context, this,
				Constants.WRITE)) {
			return true;
		}
		/* FIXME controllare derivazioni di owning collection */
		// is this collection not yet created, and an item template is created
		/*
		 * if (getOwningCollection() == null) { return true; }
		 *  // is this person an COLLECTION_EDITOR for the owning collection? if
		 * (getOwningCollection().canEditBoolean()) { return true; }
		 */
		// is this person an COLLECTION_EDITOR for the owning collection?
		// return AuthorizeManager.authorizeActionBoolean(context,
		// getOwningCollection(), Constants.COLLECTION_ADMIN);
		return true; // da togliere

	}

	@Transient
	public String getName() {
		MetadataValue t[] = getMetadata("dc", "title", null, Item.ANY);
		return (t.length >= 1) ? t[0].getValue() : null;
	}

	/**
	 * Utility method for pattern-matching metadata elements. This method will
	 * return <code>true</code> if the given schema, element, qualifier and
	 * language match the schema, element, qualifier and language of the
	 * <code>DCValue</code> object passed in. Any or all of the elemenent,
	 * qualifier and language passed in can be the <code>Item.ANY</code>
	 * wildcard.
	 * 
	 * It's a bit filthy and horrid to make this protected, but I need to access
	 * from the ItemProxy subclass. Really, it should exist somewhere else,
	 * possibly in DCValue.
	 * 
	 * @param schema
	 *            the schema for the metadata field. <em>Must</em> match the
	 *            <code>name</code> of an existing metadata schema.
	 * @param element
	 *            the element to match, or <code>Item.ANY</code>
	 * @param qualifier
	 *            the qualifier to match, or <code>Item.ANY</code>
	 * @param language
	 *            the language to match, or <code>Item.ANY</code>
	 * @param dcv
	 *            the Dublin Core value
	 * @return <code>true</code> if there is a match
	 */
	protected boolean match(String schema, String element, String qualifier,
			String language, MetadataValue mdv) {
		// We will attempt to disprove a match - if we can't we have a match
		if (!element.equals(Item.ANY)
				&& !element.equals(mdv.getMetadataField().getElement())) {
			// Elements do not match, no wildcard
			return false;
		}

		if (qualifier == null) {
			// Value must be unqualified
			if (mdv.getValue() != null) {
				// Value is qualified, so no match
				return false;
			}
		} else if (!qualifier.equals(Item.ANY)) {
			// Not a wildcard, so qualifier must match exactly
			if (!qualifier.equals(mdv.getMetadataField().getQualifier())) {
				return false;
			}
		}

		if (language == null) {
			// Value must be null language to match
			if (mdv.getLanguage() != null) {
				// Value is qualified, so no match
				return false;
			}
		} else if (!language.equals(Item.ANY)) {
			// Not a wildcard, so language must match exactly
			if (!language.equals(mdv.getValue())) {
				return false;
			}
		} else if (!schema.equals(Item.ANY)) {
			if (mdv.getMetadataField().getSchema() != null
					&& !mdv.getMetadataField().getSchema().getName().equals(
							schema)) {
				// The namespace doesn't match
				return false;
			}
		}

		// If we get this far, we have a match
		return true;
	}

	// //////////////////////////////////////////////////////////////////
	// Stuff from BrowseItem
	// //////////////////////////////////////////////////////////////////

	/**
	 * Get a thumbnail object out of the item.
	 * 
	 * Warning: using this method actually instantiates an Item, which has a
	 * corresponding performance hit on the database during browse listing
	 * rendering. That's your own fault for wanting to put images on your browse
	 * page!
	 * 
	 * @return
	 */
	@Transient
	public Thumbnail getThumbnail() {
		// if there's no original, there is no thumbnail
		List<Bundle> original = getBundles("ORIGINAL");
		if (original.size() == 0) {
			return null;
		}

		// if multiple bitstreams, check if the primary one is HTML
		boolean html = false;
		if (original.get(0).getBitstreams().size() > 1) {
			List<Bitstream> bitstreams = original.get(0).getBitstreams();

			for (int i = 0; (i < bitstreams.size()) && !html; i++) {
				if (bitstreams.get(i).getId() == original.get(0)
						.getPrimaryBitstream().getId()) {
					html = bitstreams.get(i).getFormat().getMIMEType().equals(
							"text/html");
				}
			}
		}

		// now actually pull out the thumbnail (ouch!)
		List<Bundle> thumbs = getBundles("THUMBNAIL");

		// if there are thumbs and we're not dealing with an HTML item
		// then show the thumbnail
		if ((thumbs.size() > 0) && !html) {
			Bitstream thumbnailBitstream;
			Bitstream originalBitstream;

			if ((original.get(0).getBitstreams().size() > 1)
					&& (original.get(0).getPrimaryBitstream().getId() > -1)) {
				/* FIXME: find da risolver */
				// originalBitstream = Bitstream.find(context,
				// original[0].getPrimaryBitstreamID());
				// thumbnailBitstream =
				// thumbs[0].getBitstreamByName(originalBitstream.getName() +
				// ".jpg");
				// copiati da sotto: rimuovere!
				originalBitstream = original.get(0).getBitstreams().get(0);
				thumbnailBitstream = thumbs.get(0).getBitstreams().get(0);
			} else {
				originalBitstream = original.get(0).getBitstreams().get(0);
				thumbnailBitstream = thumbs.get(0).getBitstreams().get(0);
			}

			if ((thumbnailBitstream != null)
					&& (AuthorizeManager.authorizeActionBoolean(context,
							thumbnailBitstream, Constants.READ))) {
				Thumbnail thumbnail = new Thumbnail(thumbnailBitstream,
						originalBitstream);
				return thumbnail;
			}
		}

		return null;
	}

	/* The collections that own this item */
	@ManyToMany(mappedBy = "items")
	public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	public void addCollection(Collection collection) {
		collections.add(collection);
	}

	@Transient
	public boolean isMetadataChanged() {
		return metadataChanged;
	}

	@Transient
	public boolean isModified() {
		return modified;
	}

}
