package org.dspace.content.dao.hibernate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

public class CollectionDAOHibernate extends CollectionDAO { 
    
    private Context context;
    
	public CollectionDAOHibernate(Context context) {
	    //this.context=context;
		super(context);
	}
	
	/*
	 * Returns all collections
	 */
	
	public List<Collection> getCollections(EntityManager em) {
		Query q = em.createQuery("SELECT c FROM Collection c");
		List<Collection> collections = q.getResultList();
		return collections;
	}
	
	//TODO testare
	public Integer getCount(Collection collection, EntityManager em) {
	    if(collection==null) throw new IllegalArgumentException("Collection in itemCount may not be null");
	    Query q = em.createQuery("SELECT itemCount FROM CollectionItemCount collectionItemCount WHERE collectionItemCount.collection= :collection");
        q.setParameter("collection", collection);
        Integer itemCount = (Integer)q.getSingleResult();
        return itemCount;	    
	}
	
	//TODO testare
	public Integer count(Collection collection, EntityManager em) {
	    if(collection==null) throw new IllegalArgumentException("Collection in itemCount may not be null");
	    Query q = em.createQuery("SELECT count(i) " +
	    		                 "FROM Collection c, IN (c.items) as i " +
	    		                 "WHERE c = :collection AND i.in_archive = true AND i.withdrawn = false");
	    q.setParameter("collection", collection);
	    Integer itemCount = (Integer)q.getSingleResult();
	    return itemCount;
	}
	
    public Collection getCollectionByUUID(UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT c FROM Collection c WHERE c.uuid = :uuid");
        q.setParameter("uuid", uuid);
        Collection collection = (Collection)q.getSingleResult();
        return collection;
    }

	
	
	
    public List<Collection> findCollectionsByAuthority(Community parent, int actionID, Context context)    {
        EntityManager em = context.getEntityManager();
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
    
    public List<Collection> findNotLinkedCollection(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT c FROM Collection c, WHERE c NOT IN " +
        		"(SELECT coll FROM Item i, IN (i.collections) AS coll WHERE " +
        		"i = :item)");
        q.setParameter("item", item);
        List<Collection> cols = q.getResultList();
        return cols;
    }
	
}
