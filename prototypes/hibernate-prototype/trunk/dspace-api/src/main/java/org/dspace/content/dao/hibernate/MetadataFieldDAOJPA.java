package org.dspace.content.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.core.Context;

public class MetadataFieldDAOJPA extends MetadataFieldDAO
{
    public MetadataFieldDAOJPA(Context context) {
        super(context);
    }
    
    public MetadataField findMetadataField(String schema, String element, String qualifier, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdf " +
        		"FROM MetadataField mdf " +
        		"WHERE mdf.schema.name = :schema AND mdf.element = :element AND mdf.qualifier = :qualifier");
        q.setParameter("schema", schema);
        q.setParameter("element", element);
        q.setParameter("qualifier", qualifier);
        MetadataField mdf = (MetadataField)q.getSingleResult();
        return mdf;
    }
    
    public MetadataField findMetadataField(int schemaId, String element, String qualifier, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdf " +
                "FROM MetadataField mdf " +
                "WHERE mdf.schema.id = :schemaId AND mdf.element = :element AND mdf.qualifier = :qualifier");
        q.setParameter("schemaId", schemaId);
        q.setParameter("element", element);
        q.setParameter("qualifier", qualifier);
        MetadataField mdf = (MetadataField)q.getSingleResult();
        return mdf;
    }
    
    public List<MetadataField> findMetadataFields(MetadataSchema schema, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdf " +
        		"FROM MetadataField mdf " +
        		"WHERE mdf.schema = :schema");
        q.setParameter("schema", schema);
        List<MetadataField> mdfs = q.getResultList();
        return mdfs;        
    }
    
    /* FIXME ordinare prima di tutto per short_id dello schema */
    public List<MetadataField> findAllMetadataFields(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mdf FROM MetadataField mdf " +
        		"ORDER BY mdf.elementi, mdf.qualifier");
        List<MetadataField> mdfs = q.getResultList();
        return mdfs;
    }
    
    public MetadataField getMetadataFieldByUUID (UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT c FROM Community c WHERE c.uuid = :uuid");
        q.setParameter("uuid", uuid);
        MetadataField mdf = (MetadataField)q.getSingleResult();
        return mdf;
    }
}
