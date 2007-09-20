/*
 * Harvester.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/30 03:57:56 $
 *
 * Copyright (c) 2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.federate.harvester;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Harvester
{
	/** log4j category */
	private static Logger log = Logger.getLogger(Harvester.class);

	/** Date formatter for formatting Date objects as ISO8601 dates */
	private static DateFormat dateFormat;

	/** SAX parser, for quick splitting of response into separate records */
	private static XMLReader xmlReader;

	/**
	 * Create a new METS harvester
	 */
	public Harvester() throws SAXException
	{
		super();

		// Create ISO8601 date formatter
		dateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");
		//dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateFormat.setTimeZone(TimeZone.getDefault());
		
		// Create non-validating SAX parser
		xmlReader = XMLReaderFactory
				.createXMLReader("org.apache.xerces.parsers.SAXParser");
		xmlReader.setFeature("http://xml.org/sax/features/validation", false);
		xmlReader.setFeature(
				"http://apache.org/xml/features/validation/dynamic", true);
	}

	/**
	 * Harvests metadata documents from a remote repository in a particular date
	 * range. Either date range may be <code>null</code>, indicating the
	 * beginning or end of time. If the remote repository does not wish to
	 * return all metadata documents in one response, the results will include a
	 * resumption token to carry the harvest on later on.
	 * 
	 * @param baseURL
	 *			OAI-PMH base URL of remote repository
	 * @param metadataPrefix
	 *			metadata prefix to harvest
	 * @param startDate
	 *			start of required date range, or <code>null</code>
	 * @param endDate
	 *			end of required date range, or <code>null</code>
	 * @return results of the harvest
	 * @throws HarvestException
	 *			 if some problem occurs during the harvest, for example the
	 *			 network connection or remote repository is down
	 */
	public HarvestResults harvestDateRange(String baseURL,
			String metadataPrefix, Date startDate, Date endDate)
			throws HarvestException
	{
		StringBuffer harvestURL = new StringBuffer(baseURL).append(
				"?verb=ListRecords&metadataPrefix=").append(metadataPrefix);

		if (startDate != null)
		{
			harvestURL.append("&from=").append(dateFormat.format(startDate));
		}

		if (endDate != null)
		{
			harvestURL.append("&until=").append(dateFormat.format(endDate));
		}

		log.info("harvestURL = " + harvestURL.toString());
		return doHarvest(harvestURL.toString());
	}

	public HarvestResults harvestDateRange(URL baseURL,
			String metadataPrefix, Date startDate, Date endDate)
			throws HarvestException
	{
		return harvestDateRange(baseURL.toString(), metadataPrefix, startDate, endDate);
	}

	/**
	 * Harvests all METS documents from a remote repository. If the remote
	 * repository does not wish to return all METS documents in one response,
	 * the results will include a resumption token to carry the harvest on later
	 * on.
	 * 
	 * @param baseURL
	 *			OAI-PMH base URL of remote repository
	 * @param metadataPrefix
	 *			metadata prefix to harvest
	 * @return results of the harvest
	 * @throws HarvestException
	 *			 if some problem occurs during the harvest, for example the
	 *			 network connection or remote repository is down
	 */
	public HarvestResults harvestAll(String baseURL, String metadataPrefix)
			throws HarvestException
	{
		StringBuffer harvestURL = new StringBuffer(baseURL).append(
				"?verb=ListRecords&metadataPrefix=").append(metadataPrefix);

		return doHarvest(harvestURL.toString());
	}

	public HarvestResults harvestAll(URL baseURL, String metadataPrefix)
			throws HarvestException
	{
		return harvestAll(baseURL.toString(), metadataPrefix);
	}

	/**
	 * Resume a harvest previously started.
	 * 
	 * @param baseURL
	 *			OAI-PMH base URL of remote repository
	 * @param resumptionToken
	 *			the resumption token given by the remote repository
	 * @return results of the harvest
	 * @throws HarvestException
	 *			 if some problem occurs during the harvest, for example the
	 *			 network connection or remote repository is down, or if the
	 *			 remote repository does not want us to resume the harvest yet
	 */
	public HarvestResults resumeHarvest(String baseURL, String resumptionToken)
			throws HarvestException
	{
		StringBuffer harvestURL = new StringBuffer(baseURL).append(
				"?verb=ListRecords&resumptionToken=").append(resumptionToken);

		return doHarvest(harvestURL.toString());
	}

	public HarvestResults resumeHarvest(URL baseURL, String resumptionToken)
			throws HarvestException
	{
		return resumeHarvest(baseURL.toString(), resumptionToken);
	}

	/**
	 * Attempt to retrieve a single METS document from a remote repository. Note
	 * that this is not a Handle, since the OAI identifier is not the same as a
	 * Handle, and if there was previously a problem reading the METS for this
	 * item, we may not have been able to find out what the Handle really is.
	 * 
	 * @param baseURL
	 *			OAI-PMH base URL of remote repository
	 * @param metadataPrefix
	 *			metadata prefix to harvest
	 * @param oaiIdentifier
	 *			the OAI identifier of the object to retrieve.
	 * @return results of the harvest. This should have either 0 or 1 results in
	 *		 it, and no resumption token.
	 * @throws HarvestException
	 *			 if some problem occurs during the harvest, for example the
	 *			 network connection or remote repository is down
	 */
	public HarvestResults harvestSingle(String baseURL, String metadataPrefix,
			String oaiIdentifier) throws HarvestException
	{
		StringBuffer harvestURL = new StringBuffer(baseURL).append(
				"?verb=GetRecord&metadataPrefix=").append(metadataPrefix)
				.append("&identifier=").append(oaiIdentifier);

		return doHarvest(harvestURL.toString());
	}

	public HarvestResults harvestSingle(URL baseURL, String metadataPrefix,
			String oaiIdentifier) throws HarvestException
	{
		return harvestSingle(baseURL.toString(), metadataPrefix, oaiIdentifier);
	}

	/**
	 * Internal method to perform the actual harvest
	 * 
	 * @param harvestURL
	 *			complete harvest URL including verb and all arguments
	 * @return results of the harvest
	 * @throws HarvestException
	 *			 if some problem occurred during the harvest -- network
	 *			 connection, server error etc.
	 */
	private HarvestResults doHarvest(String harvestURL) throws HarvestException
	{
		try
		{
			if (log.isDebugEnabled())
			{
				log.debug("Harvesting from URL: " + harvestURL.toString());
			}

			HarvestResults results = new HarvestResults();

			xmlReader.setContentHandler(new HarvesterContentHandler(results,
					ConfigurationManager.getProperty("upload.temp.dir")));
			xmlReader.parse(harvestURL);

			if (log.isDebugEnabled())
			{
				log.debug("Returned " + results.getResultCount() + " results");
				if (results.hasResumptionToken())
				{
					log.debug("Resumption token: "
							+ results.getResumptionToken());
				}
				else
				{
					log.debug("No resumption token");
				}
			}

			return results;
		}
		catch (Exception e)
		{
			throw new HarvestException(e);
		}
	}
}
