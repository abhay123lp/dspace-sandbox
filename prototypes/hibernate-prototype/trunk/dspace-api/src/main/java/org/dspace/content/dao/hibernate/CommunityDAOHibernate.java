package org.dspace.content.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.core.Context;
import org.dspace.persistence.HibernateUtil;

public class CommunityDAOHibernate extends CommunityDAO {
	public CommunityDAOHibernate(Context context) {
		super(context);
	}
	
	//FIXME implementare?
    public boolean linked(DSpaceObject parent, DSpaceObject child) {
    	return true;
    }

	/*
	 * Returns all communities
	 */
	public List<Community> getCommunities(EntityManager em) {
		List<Community> communities = null;
		Query q = em.createQuery("SELECT c FROM Community c");
		communities = q.getResultList();
		return communities;
	}
	
	@Override
	public Community retrieve(UUID uuid) {
		Community community = null;
		return community;
	}
}
