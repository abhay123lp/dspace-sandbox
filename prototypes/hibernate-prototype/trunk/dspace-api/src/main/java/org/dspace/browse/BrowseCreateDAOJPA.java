package org.dspace.browse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class BrowseCreateDAOJPA //implements BrowseCreateDAO?
{
    public void pruneItemIndex(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE iie FROM ItemIndexEntry iie WHERE iie.item NOT IN " +
        		"(SELECT i FROM Item i)");
        q.executeUpdate();
    }
    
    public void pruneMetadataIndex(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE mie " +
        		"FROM MetadataIndexEntry mie, IN (MetadataIndexEntry.items) AS item " +
        		"WHERE item NOT IN " +
                "(SELECT i FROM Item i)");
        q.executeUpdate();
    }
    
    public void deleteMetadataIndexForItem(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE mie FROM MetadataIndexEntry mie, " +
        		"IN (MetadataIndexEntry.items) AS item " +
        		"WHERE item = :i");
        q.setParameter("i", item);
        q.executeUpdate();
    }
    
    public void deleteItemIndexForItem(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE mie " +
        		"FROM ItemIndexEntry iie " +
                "WHERE iie.item = :i");
        q.setParameter("i", item);
        q.executeUpdate();
    }
    
    public void deleteCommunityMappings(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE cm FROM CommunityMapping cm WHERE cm.item = :item");
        q.setParameter("item", item);
        q.executeUpdate();
    }
    
    public Set<Integer> findCommunitiesAndAncestorsId(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT parent FROM Community c, IN (c.parentCommunities) AS parent " +
        		"WHERE c IN " +
        		"(SELECT cm.community FROM CommunityMapping cm WHERE cm.item = :item)");
        q.setParameter("item", item);
        List<Community> result = q.getResultList();
        Set<Integer> resultSet = new HashSet<Integer>();
        for(Community c : result) {
            resultSet.add(c.getId());
        }
        return resultSet;
    }
    
    public List<CommunityMapping> findCommunityMappings(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT cm FROM CommunityMapping cm WHERE cm.item = :item)");
        q.setParameter("item", item);
        List<CommunityMapping> result = q.getResultList();
        return result;
    }
    
    public MetadataIndexEntry findMetadataIndexEntryByValue(String value, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mie FROM MetadataIndexEntry mie WHERE mie.value = :value");
        q.setParameter("value", value);
        MetadataIndexEntry mie = (MetadataIndexEntry)q.getSingleResult();
        return mie;
    }


}
