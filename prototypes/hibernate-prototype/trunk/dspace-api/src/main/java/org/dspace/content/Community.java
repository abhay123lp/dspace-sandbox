/*
 * Community.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.CascadeType;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
//import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.CollectionFactory;
import org.dspace.content.factory.CommunityFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 *
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision: 1772 $
 */
@Entity
public class Community extends DSpaceObject
{
    
	
    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;
    
    private static Logger log = Logger.getLogger(Community.class);
    
    private List<Community> parentCommunities; 
    private List<Community> subCommunities;
    private List<Collection> collections;
    private Bitstream logo;
    
    /** Metadata of this community **/
    private Map<String, CommunityMetadata> communityMetadata;
    
    
    
    public Community(Context context)
    {
        this.context = context;
        this.communityMetadata = new TreeMap<String, CommunityMetadata>();
        
        modifiedMetadata=modified=false;
        
        this.parentCommunities = new ArrayList<Community>();
        this.subCommunities = new ArrayList<Community>();
        this.collections = new ArrayList<Collection>();
    }
    
    protected Community() {}
    
    /* Creates a collection under this community */
/*    public Collection createCollection() {// throws AuthorizeException{
//    	AuthorizeManager.authorizeAction(context, this, Constants.ADD);
    	Collection collection = CollectionFactory.getInstance(context);
    	collection.addCommunity(this);
    	collections.add(collection);
    	return collection;
    }
*/    
    /* Creates a subcommunity of this community */
/*    public Community createSubCommunity() {//throws AuthorizeException{
//    	AuthorizeManager.authorizeAction(context, this, Constants.ADD);
    	Community subcommunity = CommunityFactory.getInstance(context);
    	subcommunity.addParentCommunity(this);
    	subCommunities.add(subcommunity);
    	return subcommunity;
    }
*/    
    /* Adds a collection between the ones this community owns */
/*    public void addCollection(Collection collection) {
    	collection.addCommunity(this);
    	collections.add(collection);
    }
*/    
    /* Removes a collection from the ones this community owns */
/*    public void removeCollection(Collection collection) {
    	collection.removeCommunity(this);
    	collections.remove(collection);
    }
*/    
    /* Adds a community as a subcommunity of this community */
/*    public void addSubCommunity(Community subcommunity) {
    	subcommunity.addParentCommunity(this);
    	subCommunities.add(subcommunity);
    }
*/    
    /* Removes a community from the subcommunities of this community */
/*    public void removeSubCommunity(Community subcommunity) {
    	subcommunity.removeParentCommunity(this);
    	subCommunities.remove(subcommunity);
    }
*/    
    /* Removes a community from the parent communities of this community 
     * but DOES NOT remove this community from subcommunities of the ex-parent */
/*    protected void removeParentCommunity(Community parentCommunity) {
    	parentCommunities.remove(parentCommunity);
    }
*/    
    /* Returns all the parent communities of this community */
    @ManyToMany(mappedBy="subCommunities")
    public List<Community> getParentCommunities() {
    	return this.parentCommunities;
    }
    
    /* Add a community as a parent of this community 
     * but DOES NOT add this community between the subcommunities of the new parent*/
/*    protected void addParentCommunity(Community parent) {
    	parentCommunities.add(parent);
    }
*/    
    
    /* Returns all the sub-communities owned by this community */
    @ManyToMany(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
//    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(name="community2community")
    public List<Community> getSubCommunities() {
    	return this.subCommunities;
    }
    
    /* Returns all the collections owned by this community */
    @ManyToMany(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
//    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(name="collection2communities")
    public List<Collection> getCollections() {
    	return this.collections;
    }
    
    /* Sets the logo of this community */
    protected void setLogoBitstream(Bitstream logo) {
    	this.logo=logo;
    }
    
    /* Add a logo to this community */
    public Bitstream setLogo(InputStream is) throws AuthorizeException, IOException {
    	// Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                context, this, Constants.DELETE)))
        {
            canEdit();
        }
    	
        // First, delete any existing logo
        if (logo != null)
        {
            log.info(LogManager.getHeader(context, "remove_logo",
                    "community_id=" + getId()));
            logo.setDeleted(true);
        }
        if (is != null)
        {
        	Bitstream newLogo = BitstreamFactory.getInstance(context, is);
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
//            List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(context,
//                    this, Constants.READ);
//            AuthorizeManager.addPolicies(context, policies, newLogo);
//
//            log.info(LogManager.getHeader(context, "set_logo",
//                    "community_id=" + getId() + "logo_bitstream_id="
//                            + newLogo.getId()));
        } else {
        	logo = null;
        }

    	
    	return logo;
    }
    
    /* Gets the community logo */
    @OneToOne
    public Bitstream getLogo() {
    	return this.logo;
    }
    
    /* Returns a particular field of metadata */
    @Transient
    public String getMetadata(String field) {
        return communityMetadata.get(field).getValue();
    }
    
    /* Returns the name of the community */
    @Transient
    public String getName() {
        return getMetadata("name");
    }
    
    /* Sets the name of this community */
    public void setName(String newname) {
    	setMetadata("name", newname);
    }
    
    /* Sets a field of metadata */
    public void setMetadata(String field, String value) {
    	if(field==null) {
    		throw new IllegalArgumentException(
            "Cannot save metadata with field null");
    	}
    	if (value != null) {
			if ((field.trim()).equals("name") && (value.trim()).equals("")) {
				try {
					value = I18nUtil
							.getMessage("org.dspace.workflow.WorkflowManager.untitled");
				} catch (MissingResourceException e) {
					value = "Untitled";
				}
			}
		}
        // metadata.put(field, value);
        CommunityMetadata oldmetadata = communityMetadata.get(field);
        if (oldmetadata==null) {
        	communityMetadata.put(field, new CommunityMetadata(this, field, value));        				
		} else {
			oldmetadata.setValue(value);			
		}
        
        modifiedMetadata = true;
        addDetails(field);
    }
    @Transient
    public int getType()
    {
        return Constants.COMMUNITY;
    }
    @Transient
	public boolean isModified() {
		return modified;
	}
    @Transient
	public boolean isModifiedMetadata() {
		return modifiedMetadata;
	}
    
	public void canEdit() throws AuthorizeException
    {
        //List<Community> parents = dao.getParentCommunities(this);

        for (Community parent : parentCommunities)
        {
            if (AuthorizeManager.authorizeActionBoolean(context, parent,
                    Constants.WRITE))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(context, parent,
                    Constants.ADD))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);
    }

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	public void setParentCommunities(List<Community> parentCommunities) {
		this.parentCommunities = parentCommunities;
	}

	public void setSubCommunities(List<Community> subCommunities) {
		this.subCommunities = subCommunities;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	protected void setLogo(Bitstream logo) {
		this.logo = logo;
	}
	
	/* Returns all the ancestors of this community */
	@Transient
    public List<Community> getAllParentCommunities()
    {
        List<Community> superParents = new ArrayList<Community>(parentCommunities);

        for (Community parent : parentCommunities)
        {
            superParents.addAll(parent.getAllParentCommunities());
        }

        return superParents;
    }
    
    @OneToMany(mappedBy="community",cascade = CascadeType.ALL)
//    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@MapKey(name = "field")
	public Map<String, CommunityMetadata> getCommunityMetadata() {
		return communityMetadata;
	}

	public void setCommunityMetadata(
			Map<String, CommunityMetadata> communityMetadata) {
		this.communityMetadata = communityMetadata;
	}

}
