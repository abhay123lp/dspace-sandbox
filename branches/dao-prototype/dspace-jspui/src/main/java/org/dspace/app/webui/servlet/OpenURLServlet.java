/*
 * OpenURLServlet.java
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
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.content.uri.dao.PersistentIdentifierDAO;
import org.dspace.content.uri.dao.PersistentIdentifierDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Simple servlet for open URL support. Presently, simply extracts terms from
 * open URL and redirects to search.
 * 
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class OpenURLServlet extends URIServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(OpenURLServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String uri = request.getParameter("uri");

        if (uri != null)
        {
            processURI(context, request, response, uri);
        }
        else
        {
            // Previous behaviour
            String query = "";

            // Extract open URL terms. Note: assume no repetition
            String title = request.getParameter("title");
            String authorFirst = request.getParameter("aufirst");
            String authorLast = request.getParameter("aulast");

            String logInfo = "";

            if (title != null)
            {
                query = query + " " + title;
                logInfo = logInfo + "title=\"" + title + "\",";
            }

            if (authorFirst != null)
            {
                query = query + " " + authorFirst;
                logInfo = logInfo + "aufirst=\"" + authorFirst + "\",";
            }

            if (authorLast != null)
            {
                query = query + " " + authorLast;
                logInfo = logInfo + "aulast=\"" + authorLast + "\",";
            }

            log.info(LogManager.getHeader(context, "openURL", logInfo
                    + "dspacequery=" + query));

            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/simple-search?query=" + query));
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Same as a GET
        doDSGet(context, request, response);
    }

    private void processURI(Context context, HttpServletRequest request,
            HttpServletResponse response, String uri)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        PersistentIdentifier identifier = null;
        ObjectIdentifier oi = null;
        DSpaceObject dso = null;

        // The value of URI will be the persistent identifier in canonical
        // form, eg: xyz:1234/56
        PersistentIdentifierDAO identifierDAO =
            PersistentIdentifierDAOFactory.getInstance(context);
        identifier = identifierDAO.retrieve(uri);

        oi = identifier.getObjectIdentifier();

        dso = oi.getObject(context);

        if (dso == null)
        {
            log.info(LogManager
                    .getHeader(context, "invalid_id", "uri=" + uri));
            JSPManager.showInvalidIDError(request, response, uri, -1);

            return;
        }
        else
        {
            processDSpaceObject(context, request, response, dso, null);
        }
    }
}
