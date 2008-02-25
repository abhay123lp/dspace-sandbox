package org.dspace.checker;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;

public class ChecksumHistoryDAOJPA
{
    public void deleteHistoryForBitstreamInfo(int bitstream_id, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("Delete from ChecksumHistory c where c.bitstreamId = :bitstream_id");
        q.setParameter("bitstream_id", bitstream_id);
        q.executeUpdate();
    }
    
    public int deleteHistoryByDateAndCode(Date retentionDate, String result, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE FROM ChecksumHistory c " +
        		"WHERE c.processEndDate<:redentionDate AND result=:result");
        q.setParameter("redentionDate", retentionDate);
        q.setParameter("result", result);
        int resultCount = q.executeUpdate();
        return resultCount;
    }
}
