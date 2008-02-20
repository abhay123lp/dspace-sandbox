package org.dspace.content.dao.hibernate;

import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Bitstream;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.core.Context;

public class BitstreamDAOHibernate extends BitstreamDAO { 
        
    public BitstreamDAOHibernate(Context context) {
        super(context);
    }
    
    public List<Bitstream> getDeletedBitstreams(EntityManager em) {
        Query q = em.createQuery("SELECT OBJECT(b) FROM Bitstream b WHERE b.deleted = true");        
        List<Bitstream> bitstreams = q.getResultList();        
        return bitstreams;
    }
}
