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
    
    //TODO implementare o cancellare dal dao
    public List<BitstreamFormat> getBitstreamFormats() {return null;}
    public List<BitstreamFormat> getBitstreamFormats(String extension) {return null;}
    public List<BitstreamFormat> getBitstreamFormats(boolean internal) {return null;}

}
