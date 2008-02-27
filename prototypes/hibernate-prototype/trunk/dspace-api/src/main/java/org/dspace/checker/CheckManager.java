package org.dspace.checker;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
    

    public static BitstreamInfo getBitstreamInfoByBitstreamId(int bitstreamId, Context context) {
        MostRecentChecksum mrc = ApplicationService.findMostRecentChecksumByBitstreamId(bitstreamId, context);
        Bitstream b = mrc.getBitstream();
            
        BitstreamInfo bInfo = new BitstreamInfo(b.isDeleted(), b.getStoreNumber(), b.getSize(), 
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
    
    public static List<ChecksumHistory> getBitstreamResultTypeReport(Date startDate, Date endDate, String resultCode, Context context) {
        List<ChecksumHistory> bitstreamHistory = new LinkedList<ChecksumHistory>();
        ChecksumHistory checksumHistory;
        String resultDescription;
        
        List<MostRecentChecksum> mrcs = ApplicationService.findMRCForBitstreamResultTypeReport(startDate, endDate, resultCode, context);
        
        for(MostRecentChecksum mrc : mrcs) {
            resultDescription = ApplicationService.findChecksumCheckStrByCode(mrc.getResult(), context);
            checksumHistory = new ChecksumHistory(mrc.getBitstream().getId(), 
                    mrc.getLastProcessStartDate(),
                    mrc.getLastProcessEndDate(),
                    mrc.getExpectedChecksum(),
                    mrc.getCurrentChecksum(),
                    resultDescription);
            bitstreamHistory.add(checksumHistory);
        }
        
        return bitstreamHistory;
    }
    
    public static List<ChecksumHistory> getNotProcessedBitstreamsReport(Date startDate, Date endDate, Context context) {
        List<ChecksumHistory> bitstreamHistory = new LinkedList<ChecksumHistory>();
        ChecksumHistory checksumHistory;
        String resultDescription;
        
        List<MostRecentChecksum> mrcs = ApplicationService.findMRCNotProcessedBitstreamsReport(startDate, endDate, context);
        
        for(MostRecentChecksum mrc : mrcs) {
            resultDescription = ApplicationService.findChecksumCheckStrByCode(mrc.getResult(), context);
            checksumHistory = new ChecksumHistory(mrc.getBitstream().getId(), 
                    mrc.getLastProcessStartDate(),
                    mrc.getLastProcessEndDate(),
                    mrc.getExpectedChecksum(),
                    mrc.getCurrentChecksum(),
                    resultDescription);
            bitstreamHistory.add(checksumHistory);
        }
        
        return bitstreamHistory;
    }
    
    public static List<DSpaceBitstreamInfo> getUnknownBitstreams(Context context) {
        List<DSpaceBitstreamInfo> unknownBitstreams = new LinkedList<DSpaceBitstreamInfo>();
        List<Bitstream> bitstreams = ApplicationService
                .findUnknownBitstreams(context);
        DSpaceBitstreamInfo info;
        
        for (Bitstream b : bitstreams)
        {          
            info = new DSpaceBitstreamInfo(b.isDeleted(), b.getStoreNumber(),
                    b.getSize(), b.getFormatDescription(), b.getId(), b
                            .getUserFormatDescription(), b.getInternalID(), b
                            .getSource(), b.getChecksumAlgorithm(), b
                            .getChecksum(), b.getName(), b.getDescription());
            unknownBitstreams.add(info);
        }

        return unknownBitstreams;
    }
    
}
