/*
 * Collection.java
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
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
import org.dspace.content.uri.ExternalIdentifier;
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
 * FIXME: Should we store a List of child Items in memory?
 * 
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class Collection extends DSpaceObject {
	/*---------------- OLD FIELDS ------------------------*/
	/** Flag set when data is modified, for events */
	// private boolean modified;
	/** Flag set when metadata is modified, for events */
	private boolean modifiedMetadata;

	// FIXME: Maybe we should be smart about this and only store the IDs. OTOH,
	// Groups aren't that heavyweight, and cacheing them may be a good thing
	// for performance. A proxy implementation that retrieved them on demand
	// and *then* cached them could be a good idea though.
	/*-------------------------------------------------*/
	private static Logger log = Logger.getLogger(Collection.class);
	private String license;

	private Group[] workflowGroups;
	private Group submitters;
	private Group admins;

	private List<Item> items;
	private Item templateItem;
	private Bitstream logo;

	private Map<String, String> metadata;

	public Collection(Context context) {
		this.context = context;
	}

	public Item createItem() {
		Item item = ItemFactory.getInstance(context);
		items.add(item);
		return item;
	}

	/* Creates an administrators group for this Collection */
	/*FIXME confrontare con l'originale */
	public Group createAdministrators() {
		if (admins == null) {
			admins = GroupFactory.getInstance(context);
		}
		return admins;
	}

	/* Creates a submitters group for this collection */
	public Group createSubmitters() throws AuthorizeException {
		AuthorizeManager.authorizeAction(context, this, Constants.WRITE);
		if (submitters == null) {
			submitters = GroupFactory.getInstance(context);
			submitters.setName("COLLECTION_" + getID() + "_SUBMIT");
		}
		setSubmitters(submitters);
		AuthorizeManager.addPolicy(context, this, Constants.ADD, submitters);
		return submitters;
	}

	public Group getSubmitters() {
		return submitters;
	}

	public void setSubmitters(Group submitters) {
		this.submitters = submitters;

		modifiedMetadata = false;
		clearDetails();
	}

	/* Returns the list of items of this collection */
	public List<Item> getItems() {
		return this.items;
	}

	public String getMetadata(String field) {
		if ("license".equals(field)) {
			return getLicense();
		}
		return metadata.get(field);
	}

	public void setMetadata(String field, String value) {
		// FIXME: This is a little naughty, but in technically, the license is
		// actually metadata.
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
		metadata.put(field, value);
		modifiedMetadata = true;
		addDetails(field);

	}

	public String getName() {
		return getMetadata("name");
	}

	public String getLicense() {
		if ((license == null) || license.equals("")) {
			// Fallback to site-wide default
			license = ConfigurationManager.getDefaultSubmissionLicense();
		}

		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	/* Adds an Item to this Collection */
	public void addItem(Item item) {
		items.add(item);
	}

	/* Removes an Item from this Collection */
	public void removeItem(Item item) {
		items.remove(item);
	}

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
	public Item getTemplateItem() {
		return templateItem;
	}

	/* Set the templateItem for this collection */
	public void setTemplateItem(Item templateItem) {
		this.templateItem = templateItem;
	}

	/* Sets a logo for this collection */
	public void setLogoBitstream(Bitstream logo) {
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
					"collection_id=" + getID()));
			logo = null;
		}

		if (is == null) {
			log.info(LogManager.getHeader(context, "remove_logo",
					"collection_id=" + getID()));
			logo = null;
		} else {
			/*FIXME ricontrollare questa parte, input stream -> bitstream */
        	Bitstream newLogo = BitstreamFactory.getInstance(context);
            logo = newLogo;
			// now create policy for logo bitstream
			// to match our READ policy
			List policies = AuthorizeManager.getPoliciesActionFilter(context,
					this, Constants.READ);
			AuthorizeManager.addPolicies(context, policies, logo);

			log.info(LogManager.getHeader(context, "set_logo", "collection_id="
					+ getID() + ",logo_bitstream_id=" + logo.getID()));
		}
		
		return logo;
	}

	/* Returns the collection logo */
	public Bitstream getLogo() {
		return this.logo;
	}
	
    public Group getWorkflowGroup(int step)
    {
        return workflowGroups[step - 1];
    }

    public Group[] getWorkflowGroups()
    {
        return workflowGroups;
    }
    
    public void setWorkflowGroup(int step, Group g)
    {
        workflowGroups[step - 1] = g;
    }
    
    /* FIXME controllare groupDAO */
    public Group createWorkflowGroup(int step) throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);
/*
        if (workflowGroups[step - 1] == null)
        {
            Group g = null;
            g = groupDAO.create();
            g.setName("COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
            groupDAO.update(g);
            setWorkflowGroup(step, g);

            AuthorizeManager.addPolicy(context, this, Constants.ADD, g);
        }
*/        return workflowGroups[step - 1];
    }
    
    public Group getAdministrators()
    {
        return admins;
    }
    
    public void setAdministrators(Group admins)
    {
        this.admins = admins;
    }
    
    public int getType()
    {
        return Constants.COLLECTION;
    }

}
