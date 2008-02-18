/*
 * Collection.java
 *
 * Version: $Revision: 1772 $
 *
 * Date: $Date: 2008-01-29 22:46:01 +0100 (mar, 29 gen 2008) $
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.CascadeType;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ArchiveManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.content.dao.BitstreamDAO; // Naughty!
import org.dspace.content.dao.BitstreamDAOFactory; // Naughty!
import org.dspace.content.dao.CollectionDAO; // Naughty!
import org.dspace.content.dao.CollectionDAOFactory; // Naughty!
import org.dspace.content.dao.CommunityDAO; // Naughty!
import org.dspace.content.dao.CommunityDAOFactory; // Naughty!
import org.dspace.content.dao.ItemDAO; // Naughty!
import org.dspace.content.dao.ItemDAOFactory; // Naughty!
//import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO; // Naughty!
import org.dspace.eperson.dao.GroupDAOFactory; // Naughty!
import org.dspace.event.Event;

import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.ItemFactory;
import org.dspace.eperson.factory.GroupFactory;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 * 
 * 
 * 
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision: 1772 $
 */
@Entity
public class Collection extends DSpaceObject {
	
	/** Flag set when data is modified, for events */
	private boolean modified;
	/** Flag set when metadata is modified, for events */
	private boolean modifiedMetadata;

	private static Logger log = Logger.getLogger(Collection.class);
	private String collectionLicense;
	
	private List<Group> workflowGroups;
	private Group submitters;
	private Group administrators;
	
	
	private List<Community> communities;
	private List<Item> items;
	private Item templateItem;
	private Bitstream logo;

	//private Map<String, String> metadata;
	/* String is the field, CollectionMetadata is an object with field as key attribute
	 * and the value as attribute */
	private Map<String, CollectionMetadata> collectionMetadata;
	
	public Collection(Context context) {
		this.context = context;
		this.collectionMetadata = new TreeMap<String, CollectionMetadata>();
		modified=modifiedMetadata=false;
		
		this.communities = new ArrayList<Community>();
		this.items = new ArrayList<Item>();
		this.workflowGroups = new ArrayList<Group>();
	}

	protected Collection() {}
	
	@OneToMany(mappedBy="collection",cascade = CascadeType.ALL)
//	@org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@MapKey(name = "field")
	public Map<String, CollectionMetadata> getCollectionMetadata() {
		return collectionMetadata;
	}

	public void setCollectionMetadata(Map<String, CollectionMetadata> collectionMetadata) {
		this.collectionMetadata = collectionMetadata;
	}
	
/*	public Item createItem() {
		Item item = ItemFactory.getInstance(context);
		item.setOwningCollection(this);
		item.addCollection(this);
		items.add(item);
		return item;
	}
*/
	/* Creates an administrators group for this Collection */	
	public Group createAdministrators() throws AuthorizeException {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        if (administrators == null)
        {
        	//administrators = Group.create(ourContext);
        	administrators = GroupFactory.getInstance(context);
        	administrators.setName("COLLECTION_" + getId() + "_ADMIN");
        	//administrators.update();
        }

        AuthorizeManager.addPolicy(context, this,
                Constants.COLLECTION_ADMIN, administrators);

        // register this as the admin group
        //collectionRow.setColumn("admin", administrators.getId());
        //FIXME si può eliminare?
        
        // administrators also get ADD on the submitter group
        if (submitters != null)
        {
            AuthorizeManager.addPolicy(context, submitters, Constants.ADD,
            		administrators);
        }

        modified = true;
        return administrators;
	}

	/* Creates a submitters group for this collection */
	public Group createSubmitters() throws AuthorizeException {
		AuthorizeManager.authorizeAction(context, this, Constants.WRITE);
		if (submitters == null) {
			submitters = GroupFactory.getInstance(context);
			submitters.setName("COLLECTION_" + getId() + "_SUBMIT");
		}
		setSubmitters(submitters);
		AuthorizeManager.addPolicy(context, this, Constants.ADD, submitters);
		return submitters;
	}
	
	@OneToOne	
	public Group getSubmitters() {
		return submitters;
	}

	public void setSubmitters(Group submitters) {
		this.submitters = submitters;

		modifiedMetadata = false;
		clearDetails();
	}

	/* Returns the list of items of this collection */	
	@ManyToMany(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
//    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(name="item2collections")
	public List<Item> getItems() {
		return this.items;
	}
	@Transient 
	public String getMetadata(String field) {
		if ("license".equals(field)) {
			return getLicense();
		}
		return collectionMetadata.get(field).getValue();
	}

	public void setMetadata(String field, String value) {
		if ("license".equals(field)) {
			setLicense(value);
			return;
		}

		if ((field.trim()).equals("name") && (value.trim()).equals("")) {
			try {
				value = I18nUtil
						.getMessage("org.dspace.workflow.WorkflowManager.untitled");
			} catch (MissingResourceException e) {
				value = "Untitled";
			}
		}
		CollectionMetadata oldmetadata = collectionMetadata.get(field);
		if (oldmetadata==null) {
			collectionMetadata.put(field, new CollectionMetadata(this, field, value));
		} else {
			oldmetadata.setValue(value);
		}
		modifiedMetadata = true;
		addDetails(field);

	}
	@Transient
	public String getName() {
		return getMetadata("name");
	}
	
	public void setName(String name) {
		setMetadata("name", name);
	}
	@Transient
	public String getLicense() {
		if ((collectionLicense == null) || collectionLicense.equals("")) {
			// Fallback to site-wide default
			collectionLicense = ConfigurationManager.getDefaultSubmissionLicense();
		}

		return collectionLicense;
	}

	public void setLicense(String license) {
		this.collectionLicense = license;
	}

	/* Adds an Item to this Collection */
/*	public void addItem(Item item) {
		item.addCollection(this);
		items.add(item);
	}
*/
	/* Removes an Item from this Collection */
/*	public void removeItem(Item item) {
		item.removeCollection(this);
		items.remove(item);
	}
*/
	/* Creates an empty template item for this collection */
	public void createTemplateItem() {
		if (templateItem == null) {
			templateItem = ItemFactory.getInstance(context);
		}
	}

	/* Removes the template item of this collection */
	public void removeTemplateItem() {
		if (templateItem != null) {
			templateItem = null;
		}
	}

	/* Returns the templateItem of this collection */
	@OneToOne
	public Item getTemplateItem() {
		return templateItem;
	}

	/* Set the templateItem for this collection */
	public void setTemplateItem(Item templateItem) {
		this.templateItem = templateItem;
	}

	/* Sets a logo for this collection */
	protected void setLogoBitstream(Bitstream logo) {
		this.logo = logo;
	}

	public Bitstream setLogo(InputStream is) throws AuthorizeException,
			IOException {
		// Check authorisation
		// authorized to remove the logo when DELETE rights
		// authorized when canEdit
		/* FIXME: autorizzazioni con il canedit() */

		// First, delete any existing logo
		if (logo != null) {
			log.info(LogManager.getHeader(context, "remove_logo",
					"collection_id=" + getId()));
			logo.setDeleted(true);
		}

		if (is == null) {
			log.info(LogManager.getHeader(context, "remove_logo",
					"collection_id=" + getId()));
			logo = null;
		} else {			
        	Bitstream newLogo = BitstreamFactory.getInstance(context, is);
            logo = newLogo;
			// now create policy for logo bitstream
			// to match our READ policy
			List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(context,
					this, Constants.READ);
			AuthorizeManager.addPolicies(context, policies, logo);

			log.info(LogManager.getHeader(context, "set_logo", "collection_id="
					+ getId() + ",logo_bitstream_id=" + logo.getId()));
		}
		
		return logo;
	}

	/* Returns the collection logo */
	@OneToOne
	public Bitstream getLogo() {
		return this.logo;
	}
	@Transient
    public Group getWorkflowGroup(int step)
    {
        return workflowGroups.get(step-1);
    }
	
	@OneToMany
	@JoinTable(name="collection2epersongroup")
    public List<Group> getWorkflowGroups()
    {
        return workflowGroups;
    }
    
    public void setWorkflowGroup(int step, Group g)
    {
        workflowGroups.set(step - 1, g);
    }
    
    
    public Group createWorkflowGroup(int step) throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        if (workflowGroups.get(step-1) == null)
        {
        	/* FIXME va bene così? */
            Group g = GroupFactory.getInstance(context);
            //g = groupDAO.create();
            g.setName("COLLECTION_" + getId() + "_WORKFLOW_STEP_" + step);
            //groupDAO.update(g);
            setWorkflowGroup(step, g);

            AuthorizeManager.addPolicy(context, this, Constants.ADD, g);
        }
        return workflowGroups.get(step - 1);
    }
    @OneToOne    
    public Group getAdministrators()
    {
        return administrators;
    }
    
    public void setAdministrators(Group administrators)
    {
        this.administrators = administrators;
    }
    @Transient
    public int getType()
    {
        return Constants.COLLECTION;
    }
    @ManyToMany(mappedBy="collections")
    public List<Community> getCommunities() {
		return communities;
	}

	public void setCommunities(List<Community> communities) {
		this.communities = communities;
	}
	
/*	protected void addCommunity(Community community) {
		communities.add(community);
	}
*/	
/*	protected void removeCommunity(Community community) {
		communities.remove(community);
	}
*/	@Transient
	public boolean isModified() {
		return modified;
	}
	@Transient
	public boolean isModifiedMetadata() {
		return modifiedMetadata;
	}


	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public void setWorkflowGroups(List<Group> workflowGroups) {
		this.workflowGroups = workflowGroups;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	protected void setLogo(Bitstream logo) {
		this.logo = logo;
	}
	
	@Transient
    public List<Community> getAllParentCommunities()
    {
        List<Community> superParents = new ArrayList<Community>(communities);

        for (Community parent : communities)
        {
            superParents.addAll(parent.getAllParentCommunities());
        }

        return superParents;
    }
	
	@Transient
	public Map<String, String> getMetadata() {
		Map<String, String> metadata = new TreeMap<String, String>();
		java.util.Collection<CollectionMetadata> cma = collectionMetadata.values();
		for(CollectionMetadata cm : cma) {
			metadata.put(cm.getField(), cm.getValue());
		}
		return metadata;
	}

	public void setModifiedMetadata(boolean modifiedMetadata) {
		this.modifiedMetadata = modifiedMetadata;
	}
	
	@Column(name="license")
	public String getCollectionLicense() {
		return collectionLicense;
	}

	public void setCollectionLicense(String collectionLicense) {
		this.collectionLicense = collectionLicense;
	}

}
