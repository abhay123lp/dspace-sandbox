package org.dspace.checker;

import java.sql.Timestamp;
import java.util.List;

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
    
    public List<Integer> findAllCommunityBitstreamsId(int communityId, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bits.id " +
                "FROM Community comm, IN (comm.collections) AS coll, IN (coll.items) AS i, IN (i.bundles) AS b, IN (b.bitstreams) AS bits " +
                "WHERE comm.id = :community_id");
        q.setParameter("community_id", communityId);
        List<Integer> bids = q.getResultList();
        return bids;
    }
    
    public List<Integer> findAllCollectionBitstreamsId(int collectionId, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bits.id " +
        		"FROM Collection coll, IN (coll.items) AS i, IN (i.bundles) AS b, IN (b.bitstreams) AS bits " +
        		"WHERE coll.id = :collection_id");
        q.setParameter("collection_id", collectionId);
        List<Integer> bids = q.getResultList();
        return bids;
        
    }
    
    public List<Integer> findAllItemBitstreamsId(int itemId, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT bits.id " +
        		"FROM Item i, IN (i.bundles) AS b, IN (b.bitstreams) AS bits " +
        		"WHERE i.id = :item_id");
        q.setParameter("item_id", itemId);
        List<Integer> bids = q.getResultList();
        return bids;
        
    }

    // "select bitstream_id "
    // + "from most_recent_checksum " + "where to_be_processed = true "
    // + "order by date_trunc('milliseconds', last_process_end_date), "
    // + "bitstream_id " + "ASC LIMIT 1";
    public int findOldestBitstream(Context context)
    {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mrc.bitstream.id "
                + "FROM MostRecentChecksum mrc "
                + "WHERE mrc.toBeProcessed=TRUE "
                + "ORDER BY mrc.processEndDate");
        List<Integer> ids = q.getResultList();
        if(ids==null) {
            return -1;
        } else {
            return ids.get(0);
        }
    }
//    
//    GET_OLDEST_BITSTREAM_DATE = "select bitstream_id  "
//        + "from most_recent_checksum "
//        + "where to_be_processed = true "
//        + "and last_process_start_date < ? "
//        + "order by date_trunc('milliseconds', last_process_end_date), "
//        + "bitstream_id " + "ASC LIMIT 1";
    public int findOldestBitstream(Timestamp lessThanDate, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT mrc.bitstream.id "
                + "FROM MostRecentChecksum mrc "
                + "WHERE mrc.toBeProcessed=TRUE AND mrc.processStartDate = :lessThanDate"
                + "ORDER BY mrc.processEndDate");
        q.setParameter("lessThanDate", lessThanDate);
        List<Integer> ids = q.getResultList();
        if(ids==null) {
            return -1;
        } else {
            return ids.get(0);
        }
 
    }
    
    public MostRecentChecksum findMostRecentChecksumByBitstreamId(int bitstreamId, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT OBJECT(mrc) " +
        		"FROM MostRecentChecksum mrc " +
        		"WHERE mrc.bitstreamId = :bitstreamId");
        q.setParameter("bitstreamId", bitstreamId);
        MostRecentChecksum mrc = (MostRecentChecksum)q.getSingleResult();
        return mrc;
    }
    
    //TODO implementare
    public void insertMissingChecksumBitstreams(Context context) {
        
    }
    
    //TODO
    public void insertMissingHistoryBitstream(Context context) {
        
    }
    
    
}
