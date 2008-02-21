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

import java.sql.SQLException;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

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
	private static Logger log = Logger.getLogger(ItemCounter.class);
	
	/** DAO to use to store and retrieve data */
	private ItemCountDAO dao;
	
	/** DSpace Context */
	private Context context;
	
	@Entity
	public class CommunityItemCount {
	    private Community community;
	    private int count;
	    
	    public CommunityItemCount(Community community) {
	        this.community = community;
	    }
	    
	    protected CommunityItemCount() {}
	    @Id
	    @OneToOne
        public Community getCommunity()
        {
            return community;
        }

        public void setCommunity(Community community)
        {
            this.community = community;
        }

        public int getCount()
        {
            return count;
        }

        public void setCount(int count)
        {
            this.count = count;
        }
	}
	
	@Entity
	public class CollectionItemCount {
	    private Collection collection;
        private Integer count;
        
        protected CollectionItemCount() {}
        
        @Id
        @OneToOne
        @JoinColumn(name="collection_id")
        public Collection getCollection()
        {
            return collection;
        }

        public void setCollection(Collection collection)
        {
            this.collection = collection;
        }

        public Integer getCount()
        {
            return count;
        }

        public void setCount(Integer count)
        {
            this.count = count;
        }
	}
	
	
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
		this.dao = ItemCountDAOFactory.getInstance(this.context);
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
		//Community[] tlc = Community.findAllTop(context);
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
		boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
	
		
		if (dso instanceof Collection)
        {
            if (useCache)
            {
                return ApplicationService.countItems((Collection) dso, context);
            }
            else //recalculate and save the value
            {
                // return ((Collection) dso).countItems();
                return ApplicationService.getCountItems((Collection) dso,
                        context);
            }
        }

        if (dso instanceof Community)
        {
            if (useCache)
            {
                return ApplicationService.countItems((Community) dso, context);
            }
            else
            {

                //return ((Collection) dso).countItems();
                return ApplicationService.getCountItems((Community) dso,
                        context);
            }
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
		dao.remove(dso);
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
        List<Collection> collections = community.getCollections();
        List<Community> subCommunities = community.getSubCommunities();
        
        for (Collection collection : collections)
        {
            count(collection, context);
        }

        for (Community child : subCommunities)
        {
            count(child);
        }

	}
	
	/**
	 * count and cache the number of items in the given collection
	 * 
	 * @param collection
	 * @throws ItemCountException
	 */
	private void count(Collection collection, Context context)
		throws ItemCountException
	{
		//int ccount = collection.countItems();
	    Integer ccount = ApplicationService.countItems(collection, context);
	    
	    //the collection is the primary key of the relation, so we can use merge
	    //if the row does not exist, it will be created
	    CollectionItemCount cic = new CollectionItemCount();
	    cic.setCollection(collection);	        
	    cic.setCount(ccount);
	        
	    ApplicationService.update(context, CollectionItemCount.class, cic);
		//dao.collectionCount(collection, ccount);
	}
}
