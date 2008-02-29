package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.core.Context;

public class MetadataValueDAOJPA extends MetadataValueDAO
{
    public MetadataValueDAOJPA(Context context) {
        super(context);
    }
    
    public List<MetadataValue> getMetadataValues(MetadataField field, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT value " +
        		"FROM MetadataValue value " +
        		"WHERE value.metadataField = :field");
        q.setParameter("field", field);
        List<MetadataValue> values = q.getResultList();
        return values;        
    }
    
    public List<MetadataValue> getMetadataValues(MetadataField field, String value, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdv " +
        		"FROM MetadataValue mdv " +
        		"WHERE mdv.metadataField = :field AND mdv.value LIKE :value");
        q.setParameter("field", field);
        q.setParameter("value", value);
        List<MetadataValue> values = q.getResultList();
        return values;        
    }
    
    public List<MetadataValue> getMetadataValues(MetadataField field, String value, String language, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdv " +
                "FROM MetadataValue mdv " +
                "WHERE mdv.metadataField = :field " +
                "AND mdv.value LIKE :value AND mdv.language LIKE :language");
        q.setParameter("field", field);
        q.setParameter("value", value);
        List<MetadataValue> values = q.getResultList();
        return values;   
    }
    
    public List<MetadataValue> getMetadataValues(Item item, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT value " +
                "FROM MetadataValue value " +
                "WHERE value.item = :item");
        q.setParameter("item", item);
        List<MetadataValue> values = q.getResultList();
        return values;        
    }
}
