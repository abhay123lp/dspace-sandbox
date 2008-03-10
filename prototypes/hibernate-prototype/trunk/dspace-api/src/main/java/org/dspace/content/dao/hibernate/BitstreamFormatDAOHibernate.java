package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.BitstreamFormat;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.core.Context;

public class BitstreamFormatDAOHibernate extends BitstreamFormatDAO { 
        
    public BitstreamFormatDAOHibernate(Context context) {
        super(context);
    } 
    
    public List<BitstreamFormat> findAllBitstreamFormat(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bf FROM BitstreamFormat bf");
        List<BitstreamFormat> bfs = q.getResultList();
        return bfs;
    }
    
    public List<BitstreamFormat> getBitstreamFormatByInternal(boolean internal, EntityManager em) {
        Query q = em.createQuery("SELECT b FROM BitstreamFormat b WHERE b.internal := internal");
        q.setParameter("internal", internal);
        List<BitstreamFormat> bfs = q.getResultList();
        return bfs;
    }

    public BitstreamFormat getBitstreamFormatByShortDescription(String description, EntityManager em) {
        Query q = em.createQuery("SELECT b FROM BitstreamFormat b WHERE b.short_description := short_description");
        q.setParameter("short_description", description);
        BitstreamFormat bf = (BitstreamFormat)q.getSingleResult();
        return bf;
        
    }
    
    public BitstreamFormat findBitstreamFormatByMimeType(String mimeType, EntityManager em) {
        Query q = em.createQuery("SELECT bf FROM BitstreamFormat bf " +
        		"WHERE bf.mimeType = :mimeType AND bf.internale = false");
        q.setParameter("mimeType", mimeType);
        BitstreamFormat bf = (BitstreamFormat)q.getSingleResult();
        return bf;
    }
    
    public void deleteBitstreamFormat(BitstreamFormat bf, BitstreamFormat unknown, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("UPDATE Bitstream b " +
        		"SET b.bitstreamFormat = :unknown " +
        		"WHERE b.bistreamFormat = :format");
        q.setParameter("unknown", unknown);
        q.setParameter("format", bf);
        q.executeUpdate();
        em.remove(bf);
    }
    
    public List<BitstreamFormat> getBitstreamFormats(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createNamedQuery("SELECT bf FROM BitstreamFormat bf ORDER BY bf.id");
        List<BitstreamFormat> bfs = q.getResultList();
        return bfs;
    }
    public List<BitstreamFormat> getBitstreamFormats(String extension, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bf FROM BitstreamFormat bf, " +
        		"IN (bf.fileExtensions) AS ext " +
        		"WHERE ext.extension LIKE :extension");
        q.setParameter("extension", extension);
        List<BitstreamFormat> bfs = q.getResultList();
        return bfs;
    }
    public List<BitstreamFormat> getBitstreamFormats(boolean internal, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bf FROM BitstreamFormat bf " +
                "WHERE bf.internal = :internal " +
                "AND bf.shortDescription NOT LIKE 'Unknown' " +
                "ORDER BY bf.supportLevel DESC, bf.shortDescription");
        q.setParameter("internal", internal);
        List<BitstreamFormat> bfs = q.getResultList();
        return bfs;
    }

}
