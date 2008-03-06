package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class ItemDAOHibernate extends ItemDAO {
	public ItemDAOHibernate(Context context) {
		super(context);
	}
	 
	/* returns all in-archive, not-withdrawn items */
	public List<Item> getItems(EntityManager em) {
	   	Query q = em.createQuery("SELECT OBJECT(i) " +
	   			                 "FROM Item i " +
	   			                 "WHERE i.withdrawn = false AND i.inArchive = true");	   	
		List<Item> items = q.getResultList();
		return items;
	}
	
	public List<Item> getWithdrawnItems(Collection collection, EntityManager em) {
        Query q = em.createQuery("SELECT i " +
        		                 "FROM Collection collection, IN (collection.items) AS i " +
        		                 "WHERE i.withdrawn = true AND collection = :collection");
        q.setParameter("collection", collection);
        List<Item> items = q.getResultList();
        return items;	    
	}
	
	public List<Item> getItemsByMetadataValue(MetadataValue value, EntityManager em) {
        Query q = em.createQuery("SELECT i FROM Item i, IN (i.metadata) AS m WHERE m = :metadatavalue");
        q.setParameter("metadatavalue", value);
        List<Item> items = q.getResultList();
        return items;   	    
	}
	
	//FIXME ricontrollare il formato delle date su itemdaopostgres, dato che viene cambiato
	public List<Item> findItemForHarvest(DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn, EntityManager em) {
	    String query = "SELECT DISTINCT i ";
	    
	    if (scope != null)
        {
            if (scope.getType() == Constants.COLLECTION)
            {
                query += "FROM Collection collection. IN (collection.items) AS i ";
            }
            else if (scope.getType() == Constants.COMMUNITY)
            {
                query += "FROM CommunityMapping cm, IN (cm.item) AS i ";
            }
        } else {
            query += "FROM Item i ";
        }
	    
	    query += "WHERE ";
	    boolean whereStart = false;
	    
	    if (scope != null)
        {
            if (scope.getType() == Constants.COLLECTION)
            {
                query += "collection.id = :scopeID ";
                whereStart = true;
            }
            else if (scope.getType() == Constants.COMMUNITY)
            {
                query += "community.id = :scopeID ";
                whereStart = true;
            }
        }
	    
	    if (startDate != null)
        {
            if (!whereStart)
            {
                query = query + "AND ";
            }
            query += "i.last_modified >= :startDate ";
            whereStart = true;
        }

	    if (endDate != null)
        {
            if (!whereStart)
            {
                query += "AND ";
            }
            query += "i.last_modified <= :endDate ";
            whereStart = true;
        }
	    
	    if (!withdrawn)
        {
	        if (!whereStart)
            {
                query += "AND ";
            }
            query += "i.withdrawn = false ";
            whereStart = true;
        }
	    
	    if (!whereStart)
        {
            query = query + "AND ";
        }
        query = query + "in_archive = true ";
        query += "ORDER BY item_id";
	    
	    Query q = em.createQuery(query);
	    if(scope != null) q.setParameter("scopeID", scope.getId());
	    if(startDate!=null) q.setParameter("startDate", startDate);
	    if(endDate!=null) q.setParameter("endDate", endDate);
	    
	    List<Item> result = q.getResultList();
	    return result;
	}

	
}
