/* DSpaceATOMEntry.java
 * 
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */ 

package org.dspace.sword;

import java.sql.SQLException;
import java.text.ParseException;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;

import org.purl.sword.base.SWORDEntry;

import org.w3.atom.Author;
import org.w3.atom.Content;
import org.w3.atom.ContentType;
import org.w3.atom.Contributor;
import org.w3.atom.InvalidMediaTypeException;
import org.w3.atom.Rights;
import org.w3.atom.Summary;
import org.w3.atom.Title;

/**
 * Class to represent a DSpace Item as an ATOM Entry.  This
 * handles the objects in a default way, but the intention is
 * for you to be able to extend the class with your own
 * representation if necessary.
 * 
 * @author Richard Jones
 *
 */
public class DSpaceATOMEntry
{
	/** the SWORD ATOM entry which this class effectively decorates */
	protected SWORDEntry entry;
	
	/** the item this ATOM entry represents */
	protected Item item;
	
	/**
	 * Construct the SWORDEntry object which represents the given
	 * item with the given handle.  An argument as to whether this
	 * is a NoOp request is required because in that event the 
	 * assigned identifier for the item will not be added to the
	 * SWORDEntry as it will be invalid.
	 * 
	 * @param item	the item to represent
	 * @param handle	the handle for the item
	 * @param noOp		whether this is a noOp request
	 * @return		the SWORDEntry for the item
	 */
	public SWORDEntry getSWORDEntry(Item item, String handle, boolean noOp)
	{
		entry = new SWORDEntry();
		this.item = item;

		// add the authors to the sword entry
		this.addAuthors();
		
		// add the category information to the sword entry
		this.addCategories();
		
		// add a content element to the sword entry
		this.addContentElement();
		
		// add contributors (authors plus any other bits) to the sword entry
		this.addContributors();
		
		// add the identifier for the item, if the id is going
		// to be valid by the end of the request
		if (!noOp)
		{
			this.addIdentifier(handle);
		}
		
		// add any appropriate links
		this.addLinks();
		
		// add the publish date
		this.addPublishDate();
		
		// add the rights information
		this.addRights();
		
		// add the source infomation
		this.addSource();
		
		// add the summary of the item
		this.addSummary();
		
		// add the title of the item
		this.addTitle();
		
		// add the date on which the entry was last updated
		this.addLastUpdatedDate();
		
		return entry;
	}
	
	/**
	 * add the author names from the bibliographic metadata.  Does
	 * not supply email addresses or URIs, both for privacy, and
	 * because the data is not so readily available in DSpace.
	 *
	 */
	protected void addAuthors()
	{
		DCValue[] dcv = item.getMetadata("dc.contributor.author");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Author author = new Author();
				author.setName(dcv[i].value);
				entry.addAuthors(author);
			}
		}
	}
	
	/**
	 * Add all the subject classifications from the bibliographic
	 * metadata.
	 *
	 */
	protected void addCategories()
	{
		DCValue[] dcv = item.getMetadata("dc.subject.*");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				entry.addCategory(dcv[i].value);
			}
		}
	}
	
	/**
	 * Set the content type that DSpace received.  This is just
	 * "application/zip" in this default implementation.
	 *
	 */
	protected void addContentElement()
	{
		try
		{
			Content content = new Content();
			content.setType("application/zip");
			entry.setContent(content);
		}
		catch (InvalidMediaTypeException e)
		{
			// do nothing; we'll live without the content type declaration!
		}
	}
	
	/**
	 * Add the list of contributors to the item.  This will include
	 * the authors, and any other contributors that are supplied
	 * in the bibliographic metadata
	 *
	 */
	protected void addContributors()
	{
		DCValue[] dcv = item.getMetadata("dc.contributor.*");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Contributor cont = new Contributor();
				cont.setName(dcv[i].value);
				entry.addContributor(cont);
			}
		}
	}
	
	/**
	 * Add the identifier for the item.  If the item object has
	 * a handle already assigned, this is used, otherwise, the 
	 * passed handle is used.  It is set in the form that
	 * they can be used to access the resource over http (i.e.
	 * a real URL).
	 * 
	 * @param handle
	 */
	protected void addIdentifier(String handle)
	{
		// it's possible that the item hasn't been assigned a handle yet
		if (item.getHandle() != null)
		{
			handle = item.getHandle();
		}
		
		if (handle != null && !"".equals(handle))
		{
			entry.setId(HandleManager.getCanonicalForm(handle));
		}
	}
	
	/**
	 * Add links associated with this item.  The default implementation
	 * does not support item linking, so this method currently does
	 * nothing
	 *
	 */
	protected void addLinks()
	{
		// do nothing
	}
	
	/**
	 * Add the date of publication from the bibliographic metadata
	 *
	 */
	protected void addPublishDate()
	{
		try
		{
			DCValue[] dcv = item.getMetadata("dc.date.issued");
			if (dcv != null)
			{
				if (dcv.length == 1)
				{
					entry.setPublished(dcv[0].value);
				}
			}
		}
		catch (ParseException e)
		{
			// do nothing; we'll live without the publication date
		}
	}
	
	/**
	 * Add rights information.  This attaches an href to the URL
	 * of the item's licence file
	 *
	 */
	protected void addRights()
	{
		try
		{
			String base = ConfigurationManager.getProperty("dspace.url");
			
			// if there's no base URL, we are stuck
			if (base == null)
			{
				return;
			}
			
			StringBuilder rightsString = new StringBuilder();
			Bundle[] bundles = item.getBundles("LICENSE");
			for (int i = 0; i < bundles.length; i++)
			{
				Bitstream[] bss = bundles[i].getBitstreams();
				for (int j = 0; j < bss.length; j++)
				{
					String url = base + "/bitstream/" + item.getHandle() + "/" + bss[j].getSequenceID() + "/" + bss[j].getName();
					rightsString.append(url + " ");
				}
			}
			
			Rights rights = new Rights();
			rights.setContent(rightsString.toString());
			rights.setType(ContentType.TEXT);
			entry.setRights(rights);
		}
		catch (SQLException e)
		{
			// do nothing
		}
	}
	
	/**
	 * Add the source of the bibliographic metadata.  In the default
	 * implementation, this information is not available, so this
	 * method does nothing.
	 *
	 */
	protected void addSource()
	{
		// do nothing
	}
	
	/**
	 * Add the summary/abstract from the bibliographic metadata
	 *
	 */
	protected void addSummary()
	{
		DCValue[] dcv = item.getMetadata("dc.description.abstract");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Summary summary = new Summary();
				summary.setContent(dcv[i].value);
				summary.setType(ContentType.TEXT);
				entry.setSummary(summary);
			}
		}
	}
	
	/**
	 * Add the title from the bibliographic metadata
	 *
	 */
	protected void addTitle()
	{
		DCValue[] dcv = item.getMetadata("dc.title");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Title title = new Title();
				title.setContent(dcv[i].value);
				title.setType(ContentType.TEXT);
				entry.setTitle(title);
			}
		}
	}
	
	/**
	 * Add the date that this item was last updated
	 *
	 */
	protected void addLastUpdatedDate()
	{
		try
		{
			String config = ConfigurationManager.getProperty("sword.updated.field");
			DCValue[] dcv = item.getMetadata(config);
			if (dcv != null)
			{
				if (dcv.length == 1)
				{
					DCDate dcd = new DCDate(dcv[0].value);
					entry.setUpdated(dcd.toString());
				}
			}
		}
		catch (ParseException e)
		{
			// do nothing
		}
	}
}
