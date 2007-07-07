/*
 * Navigation.java
 *
 * Version: $Revision: 1.14 $
 *
 * Date: $Date: 2006/08/08 20:58:45 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * This transform applys the basic navigational links that should be available
 * on all pages generated by DSpace.
 * 
 * @author Scott Phillips
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_head_all_of_dspace =
        message("xmlui.ArtifactBrowser.Navigation.head_all_of_dspace");
    
    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.Navigation.head_browse");
    
    private static final Message T_communities_and_collections =
        message("xmlui.ArtifactBrowser.Navigation.communities_and_collections");
    
    private static final Message T_browse_titles =
        message("xmlui.ArtifactBrowser.Navigation.browse_titles");
    
    private static final Message T_browse_authors = 
        message("xmlui.ArtifactBrowser.Navigation.browse_authors");
    
    private static final Message T_browse_subjects = 
        message("xmlui.ArtifactBrowser.Navigation.browse_subjects");
    
    private static final Message T_browse_dates = 
        message("xmlui.ArtifactBrowser.Navigation.browse_dates");
    
    private static final Message T_head_this_collection =
        message("xmlui.ArtifactBrowser.Navigation.head_this_collection");
    
    private static final Message T_head_this_community =
        message("xmlui.ArtifactBrowser.Navigation.head_this_community");
     
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key = request.getScheme() + request.getServerName() + request.getServerPort() + request.getSitemapURI() + request.getQueryString();
            
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
                key += "-" + dso.getHandle();

            return HashUtil.hash(key);
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    /**
     * Add the basic navigational options:
     * 
     * Search - advanced search
     * 
     * browse - browse by Titles - browse by Authors - browse by Dates
     * 
     * language FIXME: add languages
     * 
     * context no context options are added.
     * 
     * action no action options are added.
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used 
    	 */
        List browse = options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");
        
        
        browse.setHead(T_head_browse);

        List browseGlobal = browse.addList("global");
        List browseContext = browse.addList("context");

        browseGlobal.setHead(T_head_all_of_dspace);
        browseGlobal.addItemXref(contextPath + "/community-list",T_communities_and_collections);
        browseGlobal.addItemXref(contextPath + "/browse-title",T_browse_titles);
        browseGlobal.addItemXref(contextPath + "/browse-author",T_browse_authors);
        browseGlobal.addItemXref(contextPath + "/browse-subject",T_browse_subjects);
        browseGlobal.addItemXref(contextPath + "/browse-date",T_browse_dates);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso != null)
        {
            if (dso instanceof Item)
            {
                // If we are an item change the browse scope to the parent
                // collection.
                dso = ((Item) dso).getOwningCollection();
            }

            if (dso instanceof Collection)
            {
                browseContext.setHead(T_head_this_collection);
            }
            if (dso instanceof Community)
            {
                browseContext.setHead(T_head_this_community);
            }

            String handle = dso.getHandle();
            browseContext.addItemXref(contextPath + "/handle/" + handle + "/browse-title",T_browse_titles);
            browseContext.addItemXref(contextPath + "/handle/" + handle + "/browse-author",T_browse_authors);
            browseContext.addItemXref(contextPath + "/handle/" + handle + "/browse-subject",T_browse_subjects);
            browseContext.addItemXref(contextPath + "/handle/" + handle + "/browse-date",T_browse_dates);
        }
    }

    /**
     * Insure that the context path is added to the page meta.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // FIXME: I don't think these should be set here, but there needed and I'm
        // not sure where else it could go. Perhaps the linkResolver?
    	Request request = ObjectModelHelper.getRequest(objectModel);
        pageMeta.addMetadata("contextPath").addContent(contextPath);
        pageMeta.addMetadata("request","queryString").addContent(request.getQueryString());
        pageMeta.addMetadata("request","scheme").addContent(request.getScheme());
        pageMeta.addMetadata("request","serverPort").addContent(request.getServerPort());
        pageMeta.addMetadata("request","serverName").addContent(request.getServerName());
        pageMeta.addMetadata("request","URI").addContent(request.getSitemapURI());
        
        // Add metadata for quick searches:
        pageMeta.addMetadata("search", "simpleURL").addContent(
                contextPath + "/search");
        pageMeta.addMetadata("search", "advancedURL").addContent(
                contextPath + "/advanced-search");
        pageMeta.addMetadata("search", "queryField").addContent("query");
        
        pageMeta.addMetadata("page","contactURL").addContent(contextPath + "/contact");
        pageMeta.addMetadata("page","feedbackURL").addContent(contextPath + "/feedback");
        
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso != null)
        {
            if (dso instanceof Item)
            {
                pageMeta.addMetadata("focus","object").addContent("hdl:"+dso.getHandle());
                this.getObjectManager().manageObject(dso);
                dso = ((Item) dso).getOwningCollection();
            }
            
            if (dso instanceof Collection || dso instanceof Community)
            {
                pageMeta.addMetadata("focus","container").addContent("hdl:"+dso.getHandle());
                this.getObjectManager().manageObject(dso);
            }
        }
    }
}
