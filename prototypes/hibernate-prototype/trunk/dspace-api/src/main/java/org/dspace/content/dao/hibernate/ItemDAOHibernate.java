package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Context;

public class ItemDAOHibernate extends ItemDAO {
	public ItemDAOHibernate(Context context) {
		super(context);
	}
	
	public List<Item> getItems(EntityManager em) {
		Query q = em.createQuery("SELECT i FROM Item i");
		List<Item> items = q.getResultList();
		return items;
	}
	public void removeFromCollections(EntityManager em, Item item) {
		String query = "DELETE i FROM Item i AS j WHERE j.item_id = " + item.getId();
		Query q = em.createQuery(query);
		q.executeUpdate();
	}

	
}
