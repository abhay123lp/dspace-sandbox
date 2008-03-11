package org.dspace.content.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.core.Context;

public class MetadataSchemaDAOJPA extends MetadataSchemaDAO
{
    public MetadataSchemaDAOJPA(Context context) {
        super(context);
    }
    
    public List<MetadataSchema> findAllMetadataSchema(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM MetadataSchema s");
        List<MetadataSchema> mds = q.getResultList();
        return mds;
    }
    
    public MetadataSchema findMetadataSchemaByName(String name, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM MetadataSchema s WHERE s.name = :name");
        q.setParameter("name", name);
        MetadataSchema mds = (MetadataSchema)q.getSingleResult();
        return mds;
    }
    
    public MetadataSchema findMetadataSchemaByNamespace(String namespace, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM MetadataSchema s WHERE s.namespace = :namespace");
        q.setParameter("namespace", namespace);
        MetadataSchema mds = (MetadataSchema)q.getSingleResult();
        return mds;
    }

    public MetadataSchema getMetadataSchemaByUUID(UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT ms FROM MetadataSchema ms WHERE ms.uuid = :uuid");
        q.setParameter("uuid", "uuid");
        MetadataSchema ms = (MetadataSchema)q.getSingleResult();
        return ms;
    }
    
}
