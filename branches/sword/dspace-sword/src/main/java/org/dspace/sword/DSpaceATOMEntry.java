package org.dspace.sword;

import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.dspace.handle.HandleManager;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.DCDate;

import org.purl.sword.base.SWORDEntry;
import org.w3.atom.Author;
import org.w3.atom.Content;
import org.w3.atom.InvalidMediaTypeException;
import org.w3.atom.Contributor;
import org.w3.atom.Rights;
import org.w3.atom.ContentType;
import org.w3.atom.Summary;
import org.w3.atom.Title;

import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;
import java.sql.SQLException;

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
	protected SWORDEntry entry;
	
	protected Item item;
	
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
	 * not supply email addresses or URIs
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
			// do nothing
		}
	}
	
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
	
	protected void addLinks()
	{
		// do nothing
	}
	
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
			// do nothing
		}
	}
	
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
	
	protected void addSource()
	{
		// do nothing
	}
	
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
