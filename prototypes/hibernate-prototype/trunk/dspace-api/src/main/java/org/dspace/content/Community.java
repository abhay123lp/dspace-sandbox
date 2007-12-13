/*
 * Community.java
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
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.content.dao.BitstreamDAO;         // Naughty!
import org.dspace.content.dao.BitstreamDAOFactory;  // Naughty!
import org.dspace.content.dao.CollectionDAO;        // Naughty!
import org.dspace.content.dao.CollectionDAOFactory; // Naughty!
import org.dspace.content.dao.CommunityDAO;         // Naughty!
import org.dspace.content.dao.CommunityDAOFactory;  // Naughty!
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.event.Event;

import org.dspace.content.factory.CollectionFactory;
import org.dspace.content.factory.CommunityFactory;
import org.dspace.content.factory.BitstreamFactory;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 *
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class Community extends DSpaceObject
{
    
	/*----------------- OLD FIELDS -----------------------------*/
    /** Flag set when data is modified, for events */
//    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;
    /*----------------------------------------------*/
    private static Logger log = Logger.getLogger(Community.class);
    
    private List<Community> parentCommunities; 
    private List<Community> subCommunities;
    private List<Collection> collections;
    private Map<String, String> metadata;
    private Bitstream logo;
    
    
    public Community(Context context)
    {
        this.context = context;
        this.metadata = new TreeMap<String, String>();
    }
    
    /* Creates a collection under this community */
    public Collection createCollection() {
    	/*FIXME: autorizzazione ~ archivemanager?*/
    	Collection collection = CollectionFactory.getInstance(context);
    	collections.add(collection);
    	return collection;
    }
    
    /* Creates a subcommunity of this community */
    public Community createSubCommunity() throws AuthorizeException{
    	AuthorizeManager.authorizeAction(context, this, Constants.ADD);
    	Community subcommunity = CommunityFactory.getInstance(context);
    	subcommunity.addParentCommunity(this);
    	subCommunities.add(subcommunity);
    	return subcommunity;
    }
    
    /* Adds a collection between the ones this community owns */
    public void addCollection(Collection collection) {
    	collections.add(collection);
    }
    
    /* Removes a collection from the ones this community owns */
    public void removeCollection(Collection collection) {
    	collections.remove(collection);
    }
    
    /* Adds a community as a subcommunity of this community */
    public void addSubCommunity(Community subcommunity) {
    	subCommunities.add(subcommunity);
    }
    
    /* Removes a community from the subcommunities of this community */
    public void removeSubCommunity(Community subcommunity) {
    	subCommunities.remove(subcommunity);
    }
    
    /* Returns all the parent communities of this community */
    public List<Community> getParents() {
    	return this.parentCommunities;
    }
    
    /* Add a community as a parent of this community */
    public void addParentCommunity(Community parent) {
    	parentCommunities.add(parent);
    }
    
    
    /* Returns all the sub-communities owned by this community */
    public List<Community> getSubCommunities() {
    	return this.subCommunities;
    }
    
    /* Returns all the collections owned by this community */
    public List<Collection> getCollections() {
    	return this.collections;
    }
    
    /* Add a logo to the community */
    public void setLogoBitstream(Bitstream logo) {
    	this.logo=logo;
    }
    
    public Bitstream setLogo(InputStream is) 
    throws AuthorizeException, IOException {
    	/*FIXME: parte autorizzazioni e inserimento del metodo canEdit(). controllare dao-pr.*/
    	
        // First, delete any existing logo
        if (logo != null)
        {
            log.info(LogManager.getHeader(context, "remove_logo",
                    "community_id=" + getID()));
           logo = null;
        }
        if (is != null)
        {
        	Bitstream newLogo = BitstreamFactory.getInstance(context);
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
            List policies = AuthorizeManager.getPoliciesActionFilter(context,
                    this, Constants.READ);
            AuthorizeManager.addPolicies(context, policies, newLogo);

            log.info(LogManager.getHeader(context, "set_logo",
                    "community_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

    	
    	return logo;
    }
    
    /* Gets the community logo */
    public Bitstream getLogo() {
    	return this.logo;
    }
    
    /* Returns a particular field of metadata */
    public String getMetadata(String field) {
        return metadata.get(field);
    }
    
    /* Returns the name of the community */
    public String getName() {
        return getMetadata("name");
    }
    
    /* Sets a field of metadata */
    public void setMetadata(String field, String value) {
        if ((field.trim()).equals("name") && (value.trim()).equals(""))
        {
            try
            {
                value = I18nUtil.getMessage(
                        "org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }
        metadata.put(field, value);
        modifiedMetadata = true;
        addDetails(field);
    }
    
    public int getType()
    {
        return Constants.COMMUNITY;
    }
    
}
