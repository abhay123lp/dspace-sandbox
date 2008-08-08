/*
 * ResearchContextServlet.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
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

import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAO;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.app.recsys.researchContext.dao.EssenceDAO;
import org.dspace.app.recsys.researchContext.dao.EssenceDAOFactory;
import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAOFactory;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAO;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.content.Item;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Servlet for managing interactions with Research Contexts by logged in users.
 *
 * TODO: Move retrieveUUID and setLastContextUUID to ResearchContextDAO
 */
public class ResearchContextServlet extends DSpaceServlet
{
    /** Logs data to <code>dspace.log</code> */
    private static Logger log = Logger.getLogger(ResearchContextServlet.class);

    /** A POST request to create a ResearchContext */
    public static final int CREATE_RESEARCH_CONTEXT = 0;

    /** A POST request configure create a ResearchContext */
    public static final int CONFIGURE_RESEARCH_CONTEXT = 1;

    /** A POST request to delete a ResearchContext */
    public static final int DELETE_RESEARCH_CONTEXT = 2;

    /** A POST request to configure an Essence */    
    public static final int MANAGE_ESSENCE = 3;

    /**
     * Provides data to help display either the Research Context splash screen,
     * or the detailed list of Bookmarked items or Recommendations.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String uri = request.getRequestURI();

        // Is there a more efficient way to determine the user action?

        if (uri.contains("recommendations"))
        {
            showRecommendedItems(context, request, response);
        }
        else if (uri.contains("bookmarks"))
        {
            showBookmarkedItems(context, request, response);
        }
        else if (uri.contains("configure"))
        {
            showConfigurePage(context, request, response);
        }
        else if (uri.contains("create"))
        {
            showCreatePage(context, request, response);   
        }
        else
        {
            showResearchContexts(context, request, response);
        }
    }

    /**
     * Delegates HTTP Post requests to the appropriate helper method cased on
     * the <code>step</code> parameter in the HTTP Request. If the
     * <code>step</code> does not match one of the defined static integer
     * variables defined in this class, an <code>Integrity Error</code> is shown
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        try
        {
            int step = UIUtil.getIntParameter(request, "step");

            switch (step)
            {
                case CONFIGURE_RESEARCH_CONTEXT:
                    configureResearchContext(context, request, response);
                    showResearchContexts(context, request, response);
                    break;
                case CREATE_RESEARCH_CONTEXT:
                    createResearchContext(context, request, response);
                    showResearchContexts(context, request, response);
                    break;
                case MANAGE_ESSENCE:
                    manageEssences(context, request, response);
                    showResearchContexts(context, request, response);                    
                    break;
                default:
                    log.warn(LogManager.getHeader(context, "integrity_error",
                             UIUtil.getRequestLogInfo(request)));
                    JSPManager.showIntegrityError(request, response);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets data to help display the ResearchContext main page and forwards
     * the request onto the JSPManager to display the page. 
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void showResearchContexts(Context context,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws
                                     ServletException, IOException,
                                     SQLException, AuthorizeException
    {
        response.addHeader("Pragma", "no-cache");

        // Try to get the request research context uuid from the request object
        UUID uuid = retrieveUUID(context, request);
        request.setAttribute("uuid", uuid);

        ResearchContextDAO dao = ResearchContextDAOFactory.getInstance(context);

        ResearchContext rC = dao.retrieve(uuid);

        addRecommendations(context, request, response, rC);

        addBookmarks(request, response, rC);

        request.setAttribute("researchContexts",
          dao.getResearchContexts(context.getCurrentUser()));

        request.setAttribute("rC", dao.retrieve(uuid));

        setLastContextUUID(context, uuid);
        JSPManager.showJSP(request, response, "/research-contexts/index.jsp");
    }

    /**
     * Gets data to help display the ResearchContext configuration page and
     * forwards the request onto the JSPManager to display the page. 
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void showConfigurePage(Context context, HttpServletRequest request,
                               HttpServletResponse response)
                               throws ServletException, IOException,
                                      SQLException, AuthorizeException
    {
        UUID uuid = UUID.fromString(request.getParameter("uuid"));
        EPerson user = context.getCurrentUser();
        request.setAttribute("user", user);

        ResearchContextDAO dao = ResearchContextDAOFactory.getInstance(context);
        ResearchContext rC = dao.retrieve(uuid);

        request.setAttribute("researchContexts", dao.getResearchContexts(user));
        request.setAttribute("rC", rC);
        request.setAttribute("uuid", uuid);
        request.setAttribute("essences", rC.getEssences());

        JSPManager.showJSP(request,
                           response,
                           "/research-contexts/configure.jsp");
    }

    /**
     * Gets data to help display the ResearchContext create page and forwards
     * the request onto the JSPManager to display the page.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void showCreatePage(Context context, HttpServletRequest request,
                               HttpServletResponse response)
                               throws ServletException, IOException,
                                      SQLException, AuthorizeException
    {
        ResearchContextDAO researchDAO =
                ResearchContextDAOFactory.getInstance(context);
        request.setAttribute("researchContexts",
                researchDAO.getResearchContexts(context.getCurrentUser()));

        JSPManager.showJSP(request, response, "/research-contexts/create.jsp");

    }

    /**
     * Gets data to help display the full list of recommendations and forwards
     * the request onto the JSPManager to display the page.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void showRecommendedItems(Context context,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
                                    throws ServletException, IOException,
                                           SQLException, AuthorizeException
    {
        UUID uuid = retrieveUUID(context, request);

        ResearchContextDAO dao = ResearchContextDAOFactory.getInstance(context);

        ResearchContext r = dao.retrieve(uuid);

        addRecommendations(context, request, response, r);

        request.setAttribute("researchContexts",
          dao.getResearchContexts(context.getCurrentUser()));

        request.setAttribute("researchContext", r);

        request.setAttribute("uuid", uuid);

        JSPManager.showJSP(request, response,
                           "/research-contexts/recommendations.jsp");
    }

    /**
     * Gets data to help display the full list of bookmarks and forwards
     * the request onto the JSPManager to display the page.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void showBookmarkedItems(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
                                                 SQLException,
                                                 AuthorizeException
    {
        UUID uuid = retrieveUUID(context, request);

        ResearchContextDAO dao = ResearchContextDAOFactory.getInstance(context);

        ResearchContext r = dao.retrieve(uuid);

        addBookmarks(request, response, r);

        request.setAttribute("researchContexts",
          dao.getResearchContexts(context.getCurrentUser()));

        request.setAttribute("researchContext", r);

        request.setAttribute("uuid", uuid);

        JSPManager.showJSP(request, response,
                           "/research-contexts/bookmarks.jsp");
    }

    /**
     * Performs actions as desired by the user to configure their
     * ResearchContext.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void configureResearchContext(Context context,
                                         HttpServletRequest request,
                                         HttpServletResponse response)
                                        throws ServletException, IOException,
                                               SQLException, AuthorizeException
    {
        // The user pressed the cancel button
        if (!UIUtil.getSubmitButton(request, "submit_confirm")
                  .equals("submit_cancel"))
        {
            ResearchContextDAO dao =
                    ResearchContextDAOFactory.getInstance(context);
    
            String name = request.getParameter("name");
            String delete = request.getParameter("delete");
            int id = new Integer(request.getParameter("id"));

            // The user request is to delete this ResearchContext
            if (delete != null)
            {
                TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_eperson", "eperson_id",
                    context.getCurrentUser().getID());

                String uuid =
                        row.getStringColumn("initial_research_context_uuid");
                setLastContextUUID(context, UUID.fromString(uuid));

                dao.delete(id);
                context.commit();

                return;
            }

            // The user request is to rename the ResearchContext
            if (name != null)
            {
                ResearchContext r = dao.retrieve(id);
                r.setName(name);
                dao.update(r);
            }
        }
    }

    /**
     * Performs actions as desired by the user to create a new
     * ResearchContext.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void createResearchContext(Context context,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
                                     throws ServletException, IOException,
                                            SQLException, AuthorizeException

    {
        if (!UIUtil.getSubmitButton(request, "submit_create")
                  .equals("submit_cancel"))
        {
            ResearchContextDAO dao =
                    ResearchContextDAOFactory.getInstance(context);

            String localURI = ConfigurationManager.getProperty("dspace.url")
                              + "/atom/essence/";

            ResearchContext newContext = dao.create(localURI);

            newContext.setName(request.getParameter("context_name"));

            if (!request.getParameter("essenceURL").equals(""))
            {
                AtomServlet a = new AtomServlet();
                a.parseEssenceFeed(context, request.getParameter("essenceURL"),
                                   newContext);
            }

            // Finish configuring the Group for this new Research Context
            newContext.setEperson(context.getCurrentUser());
            dao.update(newContext);

            context.commit();

            // Setting this attribute takes the user to their newly created
            // research context
            request.setAttribute("research_context_id", newContext.getID());
        }
    }

    /**
     * Performs actions as desired by the user to create a manage their existing
     * Essences or create a new Essence.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an input-output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void manageEssences(Context context, HttpServletRequest request,
                               HttpServletResponse response)
            throws ServletException, IOException,
                   SQLException, AuthorizeException
    {
        if (UIUtil.getSubmitButton(request, "submit_create")
                  .equals("submit_add_essence"))
        {
            ResearchContextDAO dao =
                    ResearchContextDAOFactory.getInstance(context);
            int id = Integer.parseInt(request.getParameter("id"));
            ResearchContext r = dao.retrieve(id);
            String uri = request.getParameter("essence_uri");
            AtomServlet a = new AtomServlet();
            a.parseEssenceFeed(context, uri, r);
            dao.update(r);
            return;
        }
        if (UIUtil.getSubmitButton(request, "submit_null")
                  .equals("submit_delete_essence"))
        {
            EssenceDAO eDAO = EssenceDAOFactory.getInstance(context);
            int id = Integer.parseInt(request.getParameter("delete_essence"));
            eDAO.delete(id);
        }
    }

    /**
     * Performs actions as desired by the user to create a manage their existing
     * Essences or create a new Essence.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @param r <code>ResearchContext</code> to add recommendations to      
     */
    private void addRecommendations(Context context, HttpServletRequest request,
                                    HttpServletResponse response,
                                    ResearchContext r)
    {
        Item2ResearchContextRecommendationDAO dao =
                Item2ResearchContextRecommendationDAOFactory.
                        getInstance(context);

        String processingStyle =
                ConfigurationManager.getProperty("quambo.processing-style")
                                    .trim();

        if (processingStyle.equals("real-time"))
        {
            dao.calculateRecommendations(r);
        }

        Set<Item2ResearchContextRecommendation> s = r.getRecommendations();

        List<Item2ResearchContextRecommendation> recommendations =
                new ArrayList<Item2ResearchContextRecommendation>(s);

        Collections.sort(recommendations);

        request.setAttribute("recommendations", recommendations);
    }

    /**
     * Performs actions as desired by the user to create a manage their existing
     * Essences or create a new Essence.
     *
     * @param request <code>HTTP Request</code>
     * @param response <code>HTTP Response</code>
     * @param r <code>ResearchContext</code> to add Bookmarks to
     */
    private void addBookmarks(HttpServletRequest request,
                              HttpServletResponse response,
                              ResearchContext r)
    {
        Set<Bookmark> bookmarks = r.getBookmarks();
        List<Bookmark> bookmarksList = new ArrayList<Bookmark>(bookmarks);

        Collections.sort(bookmarksList);
        Collections.reverse(bookmarksList);

        List<Item> recommendedItems = new ArrayList<Item>();
        for (Bookmark b: bookmarksList)
        {
            recommendedItems.add(b.getItem());
        }
        request.setAttribute("bookmarkedItems", recommendedItems);
        request.setAttribute("bookmarks", bookmarksList);
    }

    /**
     * Gets the <code>ResearchContext UUID</code> from either the
     * <code>HTTP Request parameter</code>, <code>HTTP Request attribute</code>,
     * or the database.
     *
     * @param context <code>Context</code> which is used for database access
     * @param request <code>HTTP Request</code>
     * @return UUID identifying which Research Context to show
     */
    private UUID retrieveUUID(Context context, HttpServletRequest request)
    {
        UUID uuid;

        if (request.getParameter("uuid") == null)
        {
            if (request.getAttribute("uuid") == null)
            {
                // The request object did not contain a research context uuid
                // so get the uuid of the previously viewed research context

                try
                {
                    TableRowIterator tri = DatabaseManager.query(context,
                      "SELECT last_research_context_uuid FROM " +
                            "quambo_eperson WHERE eperson_id = ?",
                            context.getCurrentUser().getID());

                    TableRow row = tri.toList().get(0);

                    uuid = UUID.fromString(
                      row.getStringColumn("last_research_context_uuid"));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }

            }
            else
            {
                uuid = UUID.fromString(
                  request.getAttribute("uuid").toString());
            }
        }
        else
        {
            uuid = UUID.fromString(request.getParameter("uuid"));
        }

        return uuid;
    }

    /**
     * Based on the current operation, set the last Research Context UUID in the
     * quambo_eperson table.
     *
     * @param context Context
     * @param uuid UUID
     * @throws SQLException The SQL doesn't execute correctly.
     */
    private void setLastContextUUID(Context context, UUID uuid)
            throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context,
            "quambo_eperson", "eperson_id", context.getCurrentUser().getID());

        if (row != null)
        {
            row.setColumn("last_research_context_uuid", uuid.toString());
            DatabaseManager.update(context, row);
            context.commit();
        }
    }    
}