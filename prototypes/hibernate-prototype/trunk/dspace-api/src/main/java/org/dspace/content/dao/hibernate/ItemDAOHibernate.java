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
		Query q = em.createQuery("SELECT i FROM Item i WHERE i.inArchive = :inArchive AND i.withdrawn = :withdrawn");
		q.setParameter("inArchive", true);
		q.setParameter("withdrawn", false);
		List<Item> items = q.getResultList();
		return items;
	}
	
	public List<Item> getWithdrawnItems(Collection collection, EntityManager em) {
        Query q = em.createQuery("SELECT i FROM Collection collection, IN collection.items AS i WHERE i.withdrawn = :withdrawn");
        q.setParameter("withdrawn", true);
        List<Item> items = q.getResultList();
        return items;	    
	}
	
	public List<Item> getItemsByMetadataValue(MetadataValue value, EntityManager em) {
        Query q = em.createQuery("SELECT i FROM Item i, IN i.metadata AS m WHERE m = :metadatavalue");
        q.setParameter("metadatavalue", value);
        List<Item> items = q.getResultList();
        return items;   	    
	}
	//non serve più? cancellare?
//	public void removeFromCollections(EntityManager em, Item item) {
//		String query = "DELETE i FROM Item i AS j WHERE j.item_id = " + item.getId();
//		Query q = em.createQuery(query);
//		q.executeUpdate();
//	}

	
}
