/*
 * Site.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2005/04/20 14:22:34 $
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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.CascadeType;

import org.dspace.content.factory.CommunityFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
public class Site extends DSpaceObject
{
	/*------------OLD FIELDS----------*/
    /** "database" identifier of the site */
//    public static final int SITE_ID = 0;

    // cache for Handle that is persistent ID for entire site.
//    private static String handle = null;

//    private static Site theSite = null;
    /*--------------------------------*/
    
    private List<Community> topCommunities;
    private Bitstream logo;
    private String name;
    
    public Site(Context context) {
    	this.context = context;
    	topCommunities = new ArrayList<Community>();
    }
    
    protected Site() {}
    
    /* Creates a top-community */
    public Community createTopCommunity() {
    	Community community = CommunityFactory.getInstance(context);
    	topCommunities.add(community);
    	return community;
    }
    
    /* Marks a community as a top-community */
    public void addTopCommunity(Community community) {
    	topCommunities.add(community);
    }
    
    /* Removes a community from top-communities */
    public void deleteTopCommunity(Community community) {
    	topCommunities.remove(community);
    }
    
    /* Returns all the top-communities of this site */
    @OneToMany(cascade=CascadeType.ALL)
    @JoinTable(name="site2community")
    public List<Community> getTopCommunities() {
    	return this.topCommunities;
    }
    
    /* Add a logo to the Site */
    public void setLogo(Bitstream logo) {
    	this.logo=logo;
    }
    
    /* Gets the Site logo */
    @OneToOne
    public Bitstream getLogo() {
    	return logo;
    }
    
    public void setName(String name) {
    	this.name=name;
    }
    @Column(name="name")
    public String getName() {
    	return name;
    }
    @Transient
    public int getType()
    {
        return Constants.SITE;
    }

	public void setTopCommunities(List<Community> topCommunities) {
		this.topCommunities = topCommunities;
	}
}
