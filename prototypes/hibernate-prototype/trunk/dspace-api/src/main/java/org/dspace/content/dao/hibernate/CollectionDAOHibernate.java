package org.dspace.content.dao.hibernate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

public class CollectionDAOHibernate extends CollectionDAO {
	public CollectionDAOHibernate(Context context) {
		super(context);
	}
	
	/*
	 * Returns all collections
	 */
	@Override
	public List<Collection> getCollections(EntityManager em) {
		Query q = em.createQuery("SELECT c FROM Community c");
		List<Collection> collections = q.getResultList();
		return collections;
	}
	
/*	public void removeFromParentCommunities(EntityManager em, Collection collection) {
		String querycommunity = "DELETE c FROM Collection c AS j WHERE j.collections_id = " + collection.getId();
		String queryitem = "metti a null tutti gli owning_collection degli item che hanno questa coll";
		Query qc = em.createQuery(querycommunity);
		Query qi = em.createQuery(queryitem);
		qc.executeUpdate();
		qi.executeUpdate();
	}
*/	
	public Collection retrieve(UUID uuid)
    {
        return null;
    }
	@Override
    public List<Collection> getCollectionsByAuthority(Community parent,
            int actionID, EntityManager em)
    {
        List<Collection> results = new ArrayList<Collection>();
        List<Collection> collections = null;

        if (parent != null)
        {
            collections = parent.getCollections();
        }
        else
        {
            collections = getCollections(em);
        }

        for (Collection collection : collections)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    collection, actionID))
            {
                results.add(collection);
            }
        }

        return results;
    }
    //FIXME implementare?
    public boolean linked(Collection collection, Item item)
    {
    	return true;
    }

	
}
