package org.dspace.checker;

import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ApplicationService;
import org.dspace.core.Context;

public class CheckManager
{
    private static final Logger LOG = Logger.getLogger(CheckManager.class);
    
    public static void deleteBitstreamInfoWithHistory(int id, Context context) {
        ApplicationService.deleteBitstreamInfo(id, context);
        ApplicationService.deleteHistoryForBitstreamInfo(id, context);
    }
    
    public static void updateMissingBitstreams(Context context) {
        LOG.debug("updating missing bitstreams");
        ApplicationService.insertMissingChecksumBitstreams(context);
        ApplicationService.insertMissingHistoryBitstream(context);
    }
    

    public static BitstreamInfo findBitstreamInfoByBitstreamId(int bitstreamId, Context context) {
        MostRecentChecksum mrc = ApplicationService.findMostRecentChecksumByBitstreamId(bitstreamId, context);
        Bitstream b = mrc.getBitstream();
                     
        long bitstream_size = b.getSize(); //FIXME ma questa discrepanza tra long e int nel size?
        int bitstream_size_int = (int)bitstream_size;
        BitstreamInfo bInfo = new BitstreamInfo(b.isDeleted(), b.getStoreNumber(), bitstream_size_int, 
                b.getBitstreamFormat().getShortDescription(), b.getId(), b.getUserFormatDescription(),                b.getInternalID(),
                b.getSource(), b.getChecksumAlgorithm(), b.getChecksum(), b.getName(), 
                mrc.getLastProcessEndDate(), mrc.isToBeProcessed(), new Date());
        return bInfo;
    }
    
    public static void insertHistory(BitstreamInfo info, Context context) {
        if (info == null)
        {
            throw new IllegalArgumentException(
                    "BitstreamInfo parameter may not be null");
        }
        ChecksumHistory ch = new ChecksumHistory(info.getBitstreamId(),
                new java.sql.Timestamp(info.getProcessStartDate().getTime()),
                new java.sql.Timestamp(info.getProcessEndDate().getTime()),
                info.getStoredChecksum(), info.getCalculatedChecksum(), info
                        .getChecksumCheckResult());
        ApplicationService.save(context, ChecksumHistory.class, ch);

    }
    
}
