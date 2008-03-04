/**
 * RemoteRepository.java
 *
 * Version: $Id$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.federate;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.dspace.content.Community;
import org.dspace.content.uri.ObjectIdentifier;

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * <P>
 * Class representing a remote repository that this DSpace instance is
 * interested in harvesting and replicating content from.
 * </P>
 *
 * This is a modified version of the RemoteRepository class used for the China
 * Digital Museum Project. The key difference is that we no longer require the
 * presence of a Registry to keep track of nodes in the federation, instead
 * replying on a distributed `friends list'. From this global list, each node
 * in the federation can decide which other nodes to harvest content from.
 *
 * @author James Rutherford
 */
public class RemoteRepository
{
	private int id;
    private ObjectIdentifier oi;
    private int distance;
	private URL url;
	private String name;
	private String adminEmail;
	private boolean isActive;
	private boolean isPublic;
	private boolean isAlive;
	private Date dateAdded;
	private Date dateLastHarvested;
	private Date dateLastSeen;
	private Community community;
	private List<MetadataFormat> metadataFormats;
	private List<String> failedImports;

	public RemoteRepository()
	{
    }

    public int getID()
	{
		return id;
	}

	public void setID(int id)
	{
		this.id = id;
	}

    public ObjectIdentifier getIdentifier()
    {
        return oi;
    }

    public void setIdentifier(ObjectIdentifier oi)
    {
        this.oi = oi;
    }

    public Community getCommunity()
	{
		return community;
	}

	public void setCommunity(Community community)
	{
		this.community = community;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public URL getBaseURL()
	{
		return url;
	}

	public void setBaseURL(URL url)
	{
		this.url = url;
	}

	public String getAdminEmail()
	{
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail)
	{
		this.adminEmail = adminEmail;
	}

	public boolean isActive()
	{
		return isActive;
	}

	public void setActive(boolean isActive)
	{
		this.isActive = isActive;
	}

	public boolean isPublic()
	{
		return isPublic;
	}

	public void setPublic(boolean isPublic)
	{
		this.isPublic = isPublic;
	}

	public boolean isAlive()
	{
		return isAlive;
	}

	public void setAlive(boolean isAlive)
	{
		this.isAlive = isAlive;
	}

	public int getDistance()
	{
		return distance;
	}

	public void setDistance(int distance)
	{
		this.distance = distance;
	}

	public Date getDateAdded()
	{
		return dateAdded;
	}

	public void setDateAdded(Date dateAdded)
	{
		this.dateAdded = dateAdded;
	}

	public Date getDateLastHarvested()
	{
		return dateLastHarvested;
	}

	public void setDateLastHarvested(Date dateLastHarvested)
	{
		this.dateLastHarvested = dateLastHarvested;
	}

	public Date getDateLastSeen()
	{
		return dateLastSeen;
	}

	public void setDateLastSeen(Date dateLastSeen)
	{
		this.dateLastSeen = dateLastSeen;
	}

	public List getMetadataFormats()
	{
		return metadataFormats;
	}

	public void addMetadataFormat(MetadataFormat mf)
	{
		this.metadataFormats.add(mf);
	}

	public void setMetadataFormats(List metadataFormats)
	{
		this.metadataFormats = metadataFormats;
	}

	public List<String> getFailedImports()
	{
		return failedImports;
	}

	public void setFailedImports(List<String> failedImports)
	{
		this.failedImports = failedImports;
	}

	////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////

	/**
	 * Update from the information in an OAIRepository object.
	 *
	 * @param oair
	 */
	public void update(OAIRepository oair)
	{
		setName(oair.getName());
		setAdminEmail(oair.getAdminEmail());
		setBaseURL(oair.getBaseURL());
		setMetadataFormats(oair.getMetadataFormats());
		setDateLastSeen(new Date());
		setAlive(true);
	}

	/**
	 * This method allows the user to check if the repository described by the
	 * OAIRepository object supports a specified metadata format, identified by
	 * its associated metadata prefix. It simply looks for a match between the
	 * metadataprefixes in the list and that specified.
	 *
	 * @param prefix
	 * @return a boolean, true if a match with the specified metadata prefix is
	 * found.
	 */
	public boolean supportsMetadataPrefix(String prefix)
	{
		if (this.metadataFormats == null)
		{
			return false;
		}

		for (MetadataFormat mf : this.metadataFormats)
		{
			if (mf.getMetadataPrefix().equals(prefix))
			{
				return true;
			}
		}

		return false;
	}

	public String toString()
	{
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

	public boolean equals(Object o)
	{
		return EqualsBuilder.reflectionEquals(this, o);
	}

	public int hashCode()
	{
		return HashCodeBuilder.reflectionHashCode(this);
	}
}
