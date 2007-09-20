/**
 * OAIRepository.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2005/11/23 13:24:55 $
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
package org.dspace.app.federate;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import org.apache.log4j.Logger;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.dspace.app.federate.MetadataFormat;
import org.dspace.core.Context;

/**
 * Class representing the OAI-PMH interface of a remote repository that this
 * DSpace instance is interested in harvesting and replicating content from.
 *
 * TODO: Add support for verbs other than 'Identify'. It might be worth
 * supporting verbs such as 'ListMetadataFormats', which would return a List
 * containing the supported formats, etc. It would be particularly useful for
 * 'ListRecords'. This functionality already exists (to a certain extent) in
 * org.dspace.app.federate.harvester.
 *
 * @author James Rutherford
 */
public class OAIRepository
{
	/** log4j category */
	private static Logger log = Logger.getLogger(OAIRepository.class);

	/** The base url of the repository. */
	private URL baseURL;

	/** The name of the repository. */
	private String name;

	/** The repository administrator email address. FIXME: In the OAI-PMH
	 * protocol definition, there can be more than one adminEmail element in
	 * the Indentify response, but here we only allow for one. */
	private String adminEmail;

	/** The time granularity supported by the repository.*/
	private String timeGranularity;

	/** A list of the friends listed by the repository. */
	private List<URL> friends;

	/** A list of the metadata formats the repository supports. */
	private List<MetadataFormat> metadataFormats;

	/**
	 * Create a new instance of OAIRepository. If there is a problem creating
	 * the object from the provided URL, we propogate the exception upwards so
	 * it can be dealt with in the calling code.
	 */
	public OAIRepository(URL baseURL) throws IOException, JDOMException
	{
		Context c = null;
		try
		{
			c = new Context();
		}
		catch (SQLException sqle)
		{
			log.error("SQLException", sqle);
		}

		this.baseURL = baseURL;
		this.metadataFormats = new ArrayList<MetadataFormat>();
		this.friends = new ArrayList<URL>();

		// Populate the object with some useful information
		identify();
		listMetadataFormats();
	}

	////////////////////////////////////////////////////////////////////
	// OAI-PMH "Verb" methods
	////////////////////////////////////////////////////////////////////

	/**
	 * Identify the repository.
     */
	public void identify() throws IOException, JDOMException
	{
		String url = baseURL.toString() + "?verb=Identify";
		SAXBuilder builder;
        Document doc;
		
		builder = new SAXBuilder();
		doc = builder.build(new URL(url));

        // Parse the xml for the right details.
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Namespace fns = Namespace.getNamespace(ns.getURI() + "friends/");
        Element ident = root.getChild("Identify", ns);
        
        // And load them into the object
        setName(ident.getChildText("repositoryName", ns));
        setBaseURL(new URL(ident.getChildText("baseURL", ns)));
        setAdminEmail(ident.getChildText("adminEmail", ns));
        setTimeGranularity(ident.getChildText("granularity", ns));

		// Retrieving urls from the <friends> list is a little more complicated
		List<Element> descriptions = ident.getChildren("description", ns);
		for (Element desc : descriptions)
		{
			Element child = desc.getChild("friends", fns);
			if (child != null)
			{
				List urls = child.getChildren("baseURL", fns);
				Iterator j = urls.iterator();
				while (j.hasNext())
				{
					Element elt = (Element) j.next();
					friends.add(new URL(elt.getText()));
				}
			}
		}
	}

	/**
	 * Get the list of metadata formats supported by the repository.
	 */
	private void listMetadataFormats() throws IOException, JDOMException
	{
		String url = baseURL.toString() + "?verb=ListMetadataFormats";
		SAXBuilder builder;
        Document doc;
		
		builder = new SAXBuilder();
		doc = builder.build(new URL(url));

        // Parse the xml for the right details.
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element ident = root.getChild("ListMetadataFormats", ns);
        
        // And load them into the object
		List formats = ident.getChildren("metadataFormat", ns);
		Iterator i = formats.iterator();
		while (i.hasNext())
		{
			Element format = (Element) i.next();
			if (format != null)
			{
				String prefix = format.getChild("metadataPrefix", ns).getText();
				String schema = format.getChild("schema", ns).getText();
				String namespace = format.getChild("metadataNamespace", ns).getText();

				MetadataFormat mf = new MetadataFormat(prefix, schema, namespace);

				addMetadataFormat(mf);
			}
		}
	}

	////////////////////////////////////////////////////////////////////
	// Standard getters & setters
	////////////////////////////////////////////////////////////////////

	/**
	 * Return the name of the repository.
	 *
	 * @return name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name for the OAIRepository object.
	 *
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Get the contact details (admin email address) of the repository.
	 *
	 * @return contact
	 */
	public String getAdminEmail()
	{
		return adminEmail;
	}

	/**
	 * Set the contact details (admin email address) of the repository.
	 *
	 * @param adminEmail
	 */
	public void setAdminEmail(String adminEmail)
	{
		this.adminEmail = adminEmail;
	}

	/**
	 * Get the OAI base URL of the repository.
	 *
	 * @return baseURL
	 */
	public URL getBaseURL()
	{
		return baseURL;
	}

	/**
	 * Set the OAI base url of the repository.
	 *
	 * @param baseURL
	 */
	public void setBaseURL(URL baseURL)
	{
		this.baseURL = baseURL;
	}

	/**
	 * Get the time granularity of the repository.
	 *
	 * @return timeGranularity.
	 */
	public String getTimeGranularity()
	{
		return timeGranularity;
	}

	/**
	 * Set the time granularity of the repository. This should be String
	 * describing the UTC timestamp. Either YYYY-MM-DD (if the repository only
	 * supports day level granularity) or YYYY-MM-DDThh:mm:ssZ if it supports
	 * second level granularity.
	 *
	 * @param timeGranularity
	 */
	public void setTimeGranularity(String timeGranularity)
	{
		this.timeGranularity = timeGranularity;
	}

	/**
	 * Return the list of metadata prefixes, or null if there is none.
	 *
	 * @return metadataPrefixes if present, null if not.
	 */
	public List<MetadataFormat> getMetadataFormats()
	{
		if (metadataFormats.size() > 0)
		{
			return metadataFormats;
		}
		else
		{
			return new ArrayList<MetadataFormat>();
		}
	}

	/**
	 * Add a metadata prefix to the list.
	 *
	 * @param mf
	 */
	public void addMetadataFormat(MetadataFormat mf)
	{
		metadataFormats.add(mf);
	}

	/**
	 * Set the list of supported metadata prefixes. The parameter list should
	 * be an ArrayList of Strings.
	 *
	 * @param formats
	 */
	public void setMetadataFormats(List<MetadataFormat> formats)
	{
		metadataFormats = formats;
	}

	/**
	 * Return the list of friends.
	 *
	 * @return friends if present, null if not.
	 */
	public List<URL> getFriends()
	{
		return friends;
	}

	/**
	 * Add a friend to the list.
	 *
	 * @param baseURL
	 */
	public void addFriend(URL baseURL)
	{
		friends.add(baseURL);
	}

	/**
	 * Set the list of supported metadata prefixes. The parameter list should
	 * be an ArrayList of Strings.
	 *
	 * @param friends
	 */
	public void setFriends(ArrayList friends)
	{
		this.friends = friends;
	}

	////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////

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

