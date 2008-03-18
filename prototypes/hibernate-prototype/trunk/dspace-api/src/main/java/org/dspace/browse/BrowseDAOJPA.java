package org.dspace.browse;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;

public class BrowseDAOJPA
{
    public String findMaxForMetadataIndex(int itemID, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT MAX(mie.sortValue) " +
        		"FROM MetadataIndexEntry mie, IN (mie.items) AS item " +
        		"WHERE item.id = :itemID");
        q.setParameter("itemID", itemID);
        String max = (String)q.getSingleResult();
        return max;
        
    }
    
    public String findMaxForItemIndex(int indexNumber, int itemID, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT MAX(iie.sortValue) " +
                "FROM ItemIndexEntry iie, IN (iie.items) AS item " +
                "WHERE item.id = :itemID AND iie.indexNumber = :indexNumber");
        q.setParameter("itemID", itemID);
        q.setParameter("indexNumber", indexNumber);
        String max = (String)q.getSingleResult();
        return max;
    }
    
    public int findMaxOffsetForMetadataIndex(String value, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT DISTINCT COUNT (mie.sortValue) " +
                "FROM MetadataIndexEntry mie     " +
                "WHERE mie.sortValue < :value"); 
        q.setParameter("value", value);
        Integer offset = (Integer)q.getSingleResult();
        return offset;
    }
    
    public int findMaxOffsetForItemIndex(int sortNumber, String value, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT COUNT (iie.sortValue) " +
        		"FROM ItemIndexEntry iie " +
        		"WHERE iie.indexNumber <> :sortNumber AND iie.sortValue < :value");
        q.setParameter("sortNumber", sortNumber);
        q.setParameter("value", value);
        Integer offset = (Integer)q.getSingleResult();
        return offset;
    }
}
