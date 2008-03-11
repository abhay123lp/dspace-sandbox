package org.dspace.checker;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

public class ReporterDAOJPA extends ReporterDAO
{

    public List<MostRecentChecksum> findMRCBitstreamResultTypeReport(Date startDate, Date endDate, String resultCode, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mrc FROM MostRecentChecksum mrc WHERE mrc.lastProcessStartDate >= :startDate AND mrc.lastProcessStartDate < :endDate AND mrc.result = :resultCode ORDER BY mrc.bitstream.id");
        q.setParameter("startDate", startDate);
        q.setParameter("endDate", endDate);
        q.setParameter("resultCode", resultCode);
        List<MostRecentChecksum> mrcs = q.getResultList();
        return mrcs;
        
    }
    
    public List<MostRecentChecksum> findNotProcessedBitstreamsReport(Date startDate, Date endDate, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mrc FROM MostRecentChecksum mrc WHERE mrc.lastProcessStartDate >= :startDate AND mrc.lastProcessStartDate < :endDate AND mrc.toBeProcessed = FALSE ORDER BY mrc.bitstream.id");
        q.setParameter("startDate", startDate);
        q.setParameter("endDate", endDate);        
        List<MostRecentChecksum> mrcs = q.getResultList();
        return mrcs;
    }
    
    public List<Bitstream> findUnknownBitstreams(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT b FROM Bitstream b " +
        		"WHERE NOT EXISTS (SELECT mrc FROM MostRecentChecksum WHERE mrc.bitstream = b)");
        List<Bitstream> bitstreams = q.getResultList();
        return bitstreams;
    }
}
