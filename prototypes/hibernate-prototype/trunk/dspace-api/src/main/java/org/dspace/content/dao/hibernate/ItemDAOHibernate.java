package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
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


	
}
