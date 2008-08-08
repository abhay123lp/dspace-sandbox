package org.dspace.app.webui.servlet;

import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAO;
import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAO;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAOFactory;
import org.dspace.app.recsys.recommendation.dao.Item2ItemRecommendationDAO;
import org.dspace.app.recsys.recommendation.dao.Item2ItemRecommendationDAOFactory;
import org.dspace.app.recsys.recommendation.Item2ItemRecommendation;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.eperson.EPerson;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * <code>QuamboItemServlet</code> registers that this item has been accessed,
 * returns any <code>Item2ItemRecommendation</code>s, and provides
 * Bookmarking and Unbookmarking tools is the user is logged in.
 *
 * @author Desmond Elliott
 */
public class QuamboItemServlet extends DSpaceServlet {

    /**
     * If the <code>DSpaceObject</code> is an <code>Item</code> then
     * recommended items are returned. If the user is logged in then they will
     * see Bookmarking tools.
     *
     * @param context used for database access
     * @param request the HTTP Request
     * @param response the HTTP Response
     * @param dso the DSpaceObject
     */
    public void processDSpaceObject(Context context, HttpServletRequest
            request, HttpServletResponse response, DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item i = (Item) dso;

            // Check whether a request has been made to change the bookmark
            // status of this i
            if (request.getParameter("bookmark_action") != null)
            {
                modifyBookmarkStatus(context, request, response, i);
            }

            EPerson e = context.getCurrentUser();

            if (e == null)
            {
                request.setAttribute("user", null);
            }
            else
            {
                ResearchContextDAO dao =
                        ResearchContextDAOFactory.getInstance(context);

                request.setAttribute("user", e);

                // ResearchContexts this item is not bookmarked in
                request.setAttribute("not_bookmarked_in",
                                 dao.getResearchContexts(e, false, i));

                // ResearchContexts this item is bookmarked in
                request.setAttribute("bookmarked_in",
                                  dao.getResearchContexts(e, true, i));
            }

            // Update recommendations and retrieve similar items
            Item2ItemRecommendationDAO i2irDAO =
                    Item2ItemRecommendationDAOFactory.getInstance(context);

            String processingStyle =
                    ConfigurationManager.getProperty("quambo.processing-style");
            
            if (processingStyle.equals("real-time"))
            {
                i2irDAO.calculateRecommendations(i);
            }
            
            List<Item2ItemRecommendation> recommendations =
                    i2irDAO.getRecommendations(i.getID());

            request.setAttribute("relatedItems", recommendations);
        }
    }

    /**
     * When a logged in user requests to either <code>Bookmark</code> or
     * <code>Unbookmark</code> an <code>Item</code>
     * from a <code>ResearchContext</code>, this method is called.
     *
     * @param context used for database access
     * @param request contains the parameters to parse the requested action
     * @param response not used
     * @param item the <code>Item</code> to be bookmarked
     */
    private void modifyBookmarkStatus(Context context,
                                      HttpServletRequest request,
                                      HttpServletResponse response, Item item)
    {
        try
        {
            EPerson e = context.getCurrentUser();
            String flag = request.getParameter("bookmark_action");

            if (e == null && !Authenticate.startAuthentication(context,
                                                               request,
                                                               response))
            {
                // Send the user to the login / register page
                return;
            }
            else
            {
                BookmarkDAO bDAO = BookmarkDAOFactory.getInstance(context);
                ResearchContextDAO rDAO =
                        ResearchContextDAOFactory.getInstance(context);

                int rID = Integer.parseInt(request.getParameter("context_id"));
                ResearchContext r = rDAO.retrieve(rID);

                if (flag.equals("bookmark"))
                {
                    bDAO.addBookmark(item, r);
                }
                else
                {
                    bDAO.removeBookmark(item, r);
                }
            }

            context.commit();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
