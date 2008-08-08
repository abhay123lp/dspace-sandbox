/*
 * AtomServlet.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2007, Hewlett-Packard Company and Massachusetts
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

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.*;
import java.io.IOException;
import java.sql.SQLException;
import java.net.URL;

import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAO;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.app.recsys.researchContext.dao.EssenceDAO;
import org.dspace.app.recsys.researchContext.dao.EssenceDAOFactory;
import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.researchContext.KeyValue;
import org.dspace.app.recsys.researchContext.Essence;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.app.recsys.atom.EssenceModuleImpl;
import org.dspace.app.recsys.atom.EssenceModule;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAOFactory;

/**
 * Generates an Atom Syndication Feed representations of the Recommended Items,
 * Boomarked Items, and Essence of a <code>ResearchContext</code>.
 *
 * @author Desmond Elliott
 */
public class AtomServlet extends DSpaceServlet
{
    /**
     * Creates and inserts a feed representing a Research Context's
     * recommended items, bookmarked items, or essence, depending on the request
     * path.
     *
     * @param context Used to create DAOs
     * @param request The feed type is read from the request object.
     * @param response The feed is written to the response object.
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
        SQLException, AuthorizeException
	{
        /* Deconstruct the request path to determine which feed is requested */
        String path = request.getPathInfo();
        String[] split = path.substring(1).split("/");

        /* Create a Research Context from the UUID in the path */
        ResearchContextDAO researchDAO =
                ResearchContextDAOFactory.getInstance(context);

        ResearchContext researchContext =
                researchDAO.retrieve(UUID.fromString(split[1]));

        SyndFeed feed;

        String uri = "http://" + request.getServerName() + ":" +
                     request.getServerPort() + request.getContextPath() +
                     "/atom" + request.getPathInfo();

        /* Call the appropriate helper method based on the request path */
        if (split[0].equals("recommended_items"))
        {
            feed = generateRecommendedItemsFeed(context, researchContext);
        }
        else if (split[0].equals("bookmarked_items"))
        {
            feed = generateBookmarkedItemsFeed(context, researchContext);
        }
        else if (split[0].equals("essence"))
        {
            feed = generateEssenceFeed(request, researchContext);
        }
        else
        {
            throw new RuntimeException();
        }

        SyndLink selfLink = new SyndLinkImpl();
        selfLink.setHref(uri);
        selfLink.setRel("self");
        feed.setLinks(Collections.singletonList(selfLink));

        feed.setFeedType("atom_1.0");
        feed.setPublishedDate(new Date());
        feed.setUri(uri);

        /* Write the created feed to the HttpServletResponse */
        writeFeed(feed, response);
    }

    /**
     * Write a feed to the HttpServletResponse object.
     *
     * @param feed The SyndFeed to write to the HttpServletResponse
     * @param response The HttpServletResponse to write to
     */
    private void writeFeed(SyndFeed feed, HttpServletResponse response)
    {
        SyndFeedOutput output = new SyndFeedOutput();
        try
        {
            response.setContentType("text/xml; charset=UTF-8");
            output.output(feed, response.getWriter());
        }
        catch(Exception e)
        {
           e.printStackTrace();
        }
    }

    /**
     * Create and return a SyndFeed representing the Recommended Items in
     * the Research Context passed as a parameter to the method.
     *
     * @param context Used to create an Item Data Access Object to
     *                retrieve items
     *
     * @param researchContext Recommended Items are extracted from here.
     *
     * @return A SyndFeed representing the recommended items for this
     *         research context.
     */
    public SyndFeed generateRecommendedItemsFeed(Context context,
                                                ResearchContext researchContext)
    {
        SyndFeed feed = new SyndFeedImpl();

        /* Set the title for the feed */
        feed.setTitle("Recommendations for " +
                      researchContext.getEperson().getFullName() +"'s " +
                      researchContext.getName());

        List<SyndEntry> entries = new ArrayList<SyndEntry>();

        Iterator<Item2ResearchContextRecommendation> i = researchContext.
                getRecommendations().
                                         iterator();

        /* Create the feed entries by iterating through the
           Item2ResearchContextRecommendation
           objects and extracting the Item objects from them. */
        while(i.hasNext())
        {
            Item2ResearchContextRecommendation recommendation = i.next();

            Item item = ItemDAOFactory.getInstance(context).
                                       retrieve(recommendation.getItemID());

            // Create a SyndEntry and set the Link to the item, the title of
            // the item, and the time this recommendation was created.
            SyndEntry e = new SyndEntryImpl();
            e.setLink(item.getIdentifier().getCanonicalForm());
            e.setTitle(item.getName());
            e.setUpdatedDate(recommendation.getLastUpdated());
          
            List<String> authors = new ArrayList<String>();
            DCValue[] a = item.getMetadata("dc.contributor.author");
            for (DCValue d: a)
            {
              authors.add(d.value);
            }
            e.setAuthors(authors);

            // Add the abstract of the item as the content of the feed entry
            List<SyndContent> contents = extractAbstract(item);

            e.setContents(contents);

            // Add this SyndEntry to the List of SyndEntries that make up this
            // feed.
            entries.add(e);
        }

        feed.setEntries(entries);

        return feed;
   }

    /**
     * Create and return a SyndFeed representing the Bookmarks in
     * the Research Context passed as a parameter to the method.
     *
     * @param context Used to create an Item Data Access Object to retrieve
     *                items
     * @param researchContext Bookmarked Items are extracted from here.
     *
     * @return A SyndFeed representing the bookmarks for this research context.
     */
    public SyndFeed generateBookmarkedItemsFeed(Context context,
                                                ResearchContext researchContext)
    {
        SyndFeed feed = new SyndFeedImpl();

        /* Set the title for the feed */
        feed.setTitle("Bookmarks for " +
                      researchContext.getEperson().getFullName() +"'s " +
                      researchContext.getName());

        List<SyndEntry> entries = new ArrayList<SyndEntry>();

        Iterator<Bookmark> i = researchContext.getBookmarks().iterator();

        /* Create the feed entries by iterating through the Bookmark
           objects and extracting the Item objects from them. */
        while(i.hasNext())
        {
            Bookmark bookmark = i.next();
            
            Item item = ItemDAOFactory.getInstance(context).
                                       retrieve(bookmark.getItemID());

            SyndEntry e = new SyndEntryImpl();
            e.setLink(item.getIdentifier().getCanonicalForm());
            e.setTitle(item.getName());
            e.setUpdatedDate(bookmark.getCreated());

            List<SyndPerson> authors = new ArrayList<SyndPerson>();
            DCValue[] a = item.getMetadata("dc.contributor.author");
            for (DCValue d: a)
            {
              SyndPerson s = new SyndPersonImpl();
              s.setName(d.value);
              authors.add(s);
            }
            e.setAuthors(authors);          

            List<SyndContent> contents = extractAbstract(item);
            e.setContents(contents);

            entries.add(e);
        }

        feed.setEntries(entries);
        return feed;
    }

    /**
     * Create and return a SyndFeed representing the keyword-value pairs
     * representing the essence of a research context.
     *
     * @param request Used to construct the URL to the keywords
     * @param researchContext Used to extract the essence
     * @return A SyndFeed representing the essence of this research context.
     */
    public SyndFeed generateEssenceFeed(HttpServletRequest request,
                                        ResearchContext researchContext)
    {
        SyndFeed feed = new SyndFeedImpl();

        /* Set the title for the feed */
        feed.setTitle("Essence for " +
                      researchContext.getEperson().getFullName() + "'s " +
                      researchContext.getName());

        feed.getModules().add(new EssenceModuleImpl());

        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        Set<KeyValue> keyValues = researchContext.getKeyValueObjects();
        Set<Essence> essences = researchContext.getEssences();

        /* Create the feed entries by iterating through the Keyword-Value pairs
            in the Research Context essence. */
        for (KeyValue kv: keyValues)
        {
            // Set title and link so visual representation of feed links to a
            // browse by subject of the repository
            SyndEntry e = new SyndEntryImpl();
            e.setTitle(kv.getKey());
            e.setLink(request.getContextPath() +
                      "/browse?type=subject&order=ASC&rpp=20&value=" +
                      kv.getKey().replace(" ", "+"));

            // Create an add a module to the entry representing the custom
            // Essence module.
            EssenceModule s = new EssenceModuleImpl();
            s.setKey(kv.getKey());
            s.setValue(kv.getValue());
            s.setType(kv.getType());

            for (Essence es: essences)
            {
                if (es.getID() == kv.getEssenceID())
                {
                    s.setURI(es.getUri());
                    break;
                }
            }
            
            e.getModules().add(s);

            entries.add(e);
        }

        feed.setEntries(entries);
        return feed;
    }

    /**
     * The feed of the essence of a Research Context is parsed to seed the
     * creation of a new Research Context.
     *
     * @param url The URL of the feed used to seed
     * @param r The Research Context to seed
     */
    public void parseEssenceFeed(Context context, String url, ResearchContext r)
    {
        SyndFeedInput input = new SyndFeedInput();
        
        try
        {
            SyndFeed feed = input.build(new XmlReader(new URL(url)));

            EssenceDAO eDAO = EssenceDAOFactory.getInstance(context);
            Essence e = eDAO.create();
            e.setResearchContextID(r.getID());
            e.setUri(url);
            e.setWeight(1);
            r.addEssence(e);

            Iterator<SyndEntry> entries = feed.getEntries().iterator();
            while (entries.hasNext())
            {
                SyndEntry entry = entries.next();
                EssenceModule module =
                        (EssenceModule)entry.getModule(EssenceModule.URI);
                
                if (module != null)
                {
                    r.addKeyValue(module.getKey(), module.getValue(),
                                  module.getType(), e.getID());
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract metadata from the item and insert into the Content of an Atom
     * entry to make the feed more useful. Include author and abstract.
     *
     * @param i
     * @return
     */
    private List<SyndContent> extractAbstract(Item i)
    {
        // Add the abstract of the item as the content of the feed entry
        List<SyndContent> contents = new ArrayList<SyndContent>();
        SyndContentImpl itemAbstract = new SyndContentImpl();

        String itemAbstractString;
        DCValue[] itemAbstractArray = i.getMetadata("dc.description.abstract");
        if (itemAbstractArray.length != 0)
        {
            itemAbstractString = itemAbstractArray[0].value;
        }
        else
        {
            itemAbstractString = "No abstract available.";
        }

        itemAbstract.setValue("Abstract: " + itemAbstractString);

        contents.add(itemAbstract);

        return contents;
    }
}
