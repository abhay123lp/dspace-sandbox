/*
 * ItemCounter.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.ApplicationService;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;

import java.sql.SQLException;
import java.util.List;

/**
 * This class provides a standard interface to all item counting
 * operations for communities and collections.  It can be run from the
 * command line to prepare the cached data if desired, simply by
 * running:
 * 
 * java org.dspace.browse.ItemCounter
 * 
 * It can also be invoked via its standard API.  In the event that
 * the data cache is not being used, this class will return direct
 * real time counts of content.
 * 
 * @author Richard Jones
 *
 */
public class ItemCounter
{
    /** Log4j logger */
//    private static Logger log = Logger.getLogger(ItemCounter.class);
    
    /** DAO to use to store and retrieve data */
//    private ItemCountDAO dao;
    
    /** DSpace Context */
    private Context context;
    
    /**
     * method invoked by CLI which will result in the number of items
     * in each community and collection being cached.  These counts will
     * not update themselves until this is run again.
     * 
     * @param args
     */
    public static void main(String[] args)
        throws ItemCountException, SQLException
    {
        Context context = new Context();
        ItemCounter ic = new ItemCounter(context);
        ic.buildItemCounts();
        context.complete();
    }
    
    /**
     * Construct a new item counter which will use the give DSpace Context
     * 
     * @param context
     * @throws ItemCountException
     */
    public ItemCounter(Context context)
        throws ItemCountException
    {
        this.context = context;
//        this.dao = ItemCountDAOFactory.getInstance(this.context);
    }
    
    /**
     * This method does the grunt work of drilling through and iterating
     * over all of the communities and collections in the system and 
     * obtaining and caching the item counts for each one.
     * 
     * @throws ItemCountException
     */
    public void buildItemCounts()
        throws ItemCountException
    {
        List<Community> tlc = ApplicationService.findAllTopCommunities(context);
        for (Community community : tlc)
        {
            count(community);
        }
    }
    
    /**
     * Get the count of the items in the given container.  If the configuration
     * value webui.strengths.cache is equal to 'true' this will return the
     * cached value if it exists.  If it is equal to 'false' it will count
     * the number of items in the container in real time
     * 
     * @param dso
     * @return
     * @throws ItemCountException
     * @throws SQLException 
     */
    public int getCount(DSpaceObject dso)
        throws ItemCountException
    {
        /* this cache is not used anymore, entitymanager's one is automatically checked */
//        boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
//        
//        if (useCache)
//        {
//            return dao.getCount(dso);
//        }
        
        // if we make it this far, we need to manually count
        if (dso instanceof Collection)
        {
//            return ((Collection) dso).countItems();
            return ApplicationService.getCountItems((Collection)dso, context);
        }
        
        if (dso instanceof Community)
        {
//            return ((Community) dso).countItems();
            return ApplicationService.getCountItems((Community)dso, context);
        }
        
        return 0;
    }
    
    
    
    /**
     * Remove any cached data for the given container
     * 
     * @param dso
     * @throws ItemCountException
     */
    public void remove(DSpaceObject dso)
        throws ItemCountException
    {
//        dao.remove(dso);
        if (dso instanceof Collection)
        {
            Collection collection = (Collection)dso;
            collection.setItemCount(-1);
        }
        else if (dso instanceof Community)
        {
            Community community = (Community)dso;
            community.setItemCount(-1);
        }
        else
        {
            throw new ItemCountException("We can only delete count of items from Communities or Collections");
        }
    }
    
    /**
     * count and cache the number of items in the community.  This
     * will include all sub-communities and collections in the
     * community.  It will also recurse into sub-communities and
     * collections and call count() on them also.
     * 
     * Therefore, the count the contents of the entire system, it is
     * necessary just to call this method on each top level community
     * 
     * @param community
     * @throws ItemCountException
     */
    private void count(Community community)
        throws ItemCountException
    {
        // first count the community we are in
//        int count = community.countItems();//the count must be calculated
        int count = ApplicationService.countItems(community, context);
      //Set the given count as the number of items in the given community
//        dao.communityCount(community, count);
        community.setItemCount(count);
        
        // now get the sub-communities
        List<Community> scs = community.getSubCommunities();
        for (Community c : scs)
        {
            count(c);
        }
        
        // now get the collections
        List<Collection> cols = community.getCollections();
        for (Collection collection : cols)
        {
            count(collection);
        }
    }
    
    /**
     * count and cache the number of items in the given collection
     * 
     * @param collection
     * @throws ItemCountException
     */
    private void count(Collection collection)
        throws ItemCountException
    {
//        int ccount = collection.countItems(); //the count must be calculated
        int ccount = ApplicationService.countItems(collection, context);
      //Set the given count as the number of items in the given collection
//        dao.collectionCount(collection, ccount); 
        collection.setItemCount(ccount);
    }
}
