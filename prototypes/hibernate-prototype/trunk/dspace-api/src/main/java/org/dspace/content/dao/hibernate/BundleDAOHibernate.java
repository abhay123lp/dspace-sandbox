package org.dspace.content.dao.hibernate;

import javax.persistence.Query;
import javax.persistence.EntityManager;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.dao.BundleDAO;
import org.dspace.core.Context;

public class BundleDAOHibernate extends BundleDAO{

	public BundleDAOHibernate(Context context) {
		super(context);
	}
	public Bundle findBundleByName(Item item, String name, EntityManager em) {
		Query q = em.createQuery("SELECT OBJECT(b) FROM Bundle b WHERE b.name='"+name+"' AND b.item="+item);
		Bundle bundle = (Bundle) q.getSingleResult();
		return bundle;		
	}
}
