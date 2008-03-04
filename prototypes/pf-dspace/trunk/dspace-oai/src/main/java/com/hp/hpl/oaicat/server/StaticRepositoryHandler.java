/**
 * StaticRepositoryHandler.java
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

package com.hp.hpl.oaicat.server;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.verb.BadArgumentException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import ORG.oclc.oai.server.verb.ServerVerb;
import org.apache.log4j.Logger;
import org.dspace.app.federate.OAIRepository;
import org.dspace.app.federate.RemoteRepository;
import org.dspace.app.federate.dao.RemoteRepositoryDAO;
import org.dspace.app.federate.dao.RemoteRepositoryDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.jdom.JDOMException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author James Rutherford
 */
public class StaticRepositoryHandler extends ServerVerb
{
	private static Logger log = Logger.getLogger(StaticRepositoryHandler.class);

	private static ArrayList validParamNames = new ArrayList();
	static
	{
		validParamNames.add("initiate");
	}

	private static ArrayList requiredParamNames = new ArrayList();

	/**
	 * Server-side construction of the xml response
	 *
	 * @param context the servlet context
	 * @param request the servlet request
	 * @exception OAIBadRequestException an http 400 status code problem
	 * @exception OAINotFoundException an http 404 status code problem
	 * @exception OAIInternalServerError an http 500 status code problem
	 */
    public static String construct(HashMap context, HttpServletRequest request,
			HttpServletResponse response, Transformer serverTransformer)
		throws OAIInternalServerError, TransformerException
	{
		String remoteHost = request.getRemoteAddr();
		RemoteRepositoryDAO dao = null;
        Context c = null;

        Properties properties = (Properties)context.get("OAIHandler.properties");
		AbstractCatalog abstractCatalog =
			(AbstractCatalog)context.get("OAIHandler.catalog");
		String baseURL = properties.getProperty("OAIHandler.baseURL");
		if (baseURL == null)
		{
			try
			{
				baseURL = request.getRequestURL().toString();
			}
			catch (java.lang.NoSuchMethodError nsme)
			{
				baseURL = HttpUtils.getRequestURL(request).toString();
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		String styleSheet = properties.getProperty("OAIHandler.styleSheet");
		if (styleSheet != null)
		{
			sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"");
			sb.append(styleSheet);
			sb.append("\"?>");
		}
		sb.append("<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"");
		sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		sb.append(" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/");
		sb.append(" http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">");
		sb.append("<responseDate>");
		sb.append(createResponseDate(new Date()));
		sb.append("</responseDate>");
		sb.append(getRequestElement(request, validParamNames, baseURL));

		if (hasBadArguments(request, requiredParamNames.iterator(),
					validParamNames))
		{
			sb.append(new BadArgumentException().getMessage());
		}
		else
		{
			URL url = null;
			try
			{
				url = new URL(request.getParameter("initiate"));
			}
			catch (MalformedURLException mue)
			{
				sb.append("<message type=\"error\">");
				sb.append(request.getParameter("initiate") + " not a valid URL.");
				sb.append("</message>");
			}

			if (url != null && url.toString().length() != 0)
			{
				try
				{
                    c = new Context();
                    dao = RemoteRepositoryDAOFactory.getInstance(c);

                    RemoteRepository rr = dao.retrieve(url);
					URL ourURL = new URL(ConfigurationManager.getProperty("dspace.url.oai"));
					if (rr == null && !url.equals(ourURL))
					{
						OAIRepository oair = new OAIRepository(url);
						rr = dao.create(oair);

						sb.append("<action name=\"initiate\">");
						sb.append("<initiateURL>" + url.toString() + "</initiateURL>");
						sb.append("<remoteRepository>");
						sb.append("<name>" + rr.getName() + "</name>");
						sb.append("<adminEmail>" + rr.getAdminEmail() + "</adminEmail>");
						sb.append("<baseURL>" + rr.getBaseURL() + "</baseURL>");
						sb.append("</remoteRepository>");
						sb.append("</action>");

						// FIXME: It might be a good idea to think more carefully
						// about the minimum criteria.
						if (rr.getName() != null &&
							rr.getAdminEmail() != null &&
							rr.getBaseURL() != null)
						{
							rr.setDistance(0);
							dao.update(rr);
							sendNotificationEmail(rr, remoteHost);
							sb.append("<message type=\"success\">");
							sb.append("Repository added to candidate nodes list.");
							sb.append("</message>");
						}
					}
					else
					{
                        sb.append("<message type=\"warning\">");
                        sb.append(request.getParameter("initiate") +
                                " is already a known node.");
                        sb.append("</message>");
					}
                    c.complete();
				}
				catch (IOException ioe)
				{
					sb.append("<message type=\"error\">");
					sb.append("Invalid Identify response received from " +
							request.getParameter("initiate"));
					sb.append("</message>");
				}
				catch (JDOMException jdome)
				{
					sb.append("<message type=\"error\">");
					sb.append("Invalid Identify response received from " +
							request.getParameter("initiate"));
					sb.append("</message>");
				}
                catch (SQLException sqle)
                {
                    c.abort();
                    throw new RuntimeException(sqle);
                }
			}
		}
		sb.append("<message type=\"information\">");
		sb.append("Requesting IP address: " + remoteHost);
		sb.append("</message>");
		sb.append("</OAI-PMH>");
		return render(response, "text/xml; charset=UTF-8", sb.toString(), serverTransformer);
	}

	private static String[] split(String s)
	{
		StringTokenizer tokenizer = new StringTokenizer(s);
		String[] tokens = new String[tokenizer.countTokens()];
		for (int i=0; i<tokens.length; ++i)
		{
			tokens[i] = tokenizer.nextToken();
		}
		return tokens;
	}

	private static void sendNotificationEmail(RemoteRepository rr, String ip)
	{
		try
		{
			Email mail = ConfigurationManager.getEmail("federation-initiate");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

			mail.addArgument(ConfigurationManager.getProperty("dspace.url"));
			mail.addArgument(rr.getName());
			mail.addArgument(rr.getBaseURL());
			mail.addArgument(rr.getAdminEmail());
			mail.addArgument(dateFormat.format(rr.getDateAdded()));
			mail.addArgument(ip);

			// Send to remote admin as well as local admin
			mail.addRecipient(ConfigurationManager.getProperty("mail.admin"));
			mail.addRecipient(rr.getAdminEmail());
			mail.send();
		}
		catch (IOException ioe)
		{
			log.error("Could not find 'federation-initiate' email file.");
		}
		catch (MessagingException me)
		{
			log.error("Failed to send initiation confirmation email.");
		}
	}
}
