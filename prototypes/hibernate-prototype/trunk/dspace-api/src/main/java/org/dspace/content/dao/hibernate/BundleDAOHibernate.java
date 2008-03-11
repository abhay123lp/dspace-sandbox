package org.dspace.content.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.dao.BundleDAO;
import org.dspace.core.Context;

public class BundleDAOHibernate extends BundleDAO{ 

    public BundleDAOHibernate(Context context) {
        super(context);
	}
	public Bundle findBundleByName(Item item, String name, EntityManager em) {
		Query q = em.createQuery("SELECT OBJECT(b) FROM Bundle b WHERE b.name = :name AND b.item = :item");
		q.setParameter("name", name);
		q.setParameter("item", item);
		List<Bundle> bundles = q.getResultList();
		Bundle bundle = bundles.get(0); 		
		return bundle;		
	}
	
	public Bundle getBundleByUUID(UUID uuid, Context context) {
	    EntityManager em = context.getEntityManager();
	    Query q = em.createQuery("SELECT b FROM Bundle b WHERE b.uuid = :uuid");
	    q.setParameter("uuid", uuid);
	    Bundle bundle = (Bundle)q.getSingleResult();
	    return bundle;
	}
}
