package org.dspace.checker;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;

public class BitstreamInfoDAOJPA
{
    public void deleteBitstreamInfo(int bitstream_id, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("Delete from MostRecentChecksum m where m.bitstreamId = :bitstream_id");
        q.setParameter("bitstream_id", bitstream_id);
        int numDeleted = q.executeUpdate();
        if (numDeleted > 1)
        {            
            throw new IllegalStateException(
                    "Too many rows deleted! Number of rows deleted: "
                            + numDeleted
                            + " only one row should be deleted for bitstream id "
                            + bitstream_id);
        }        
    }
}
