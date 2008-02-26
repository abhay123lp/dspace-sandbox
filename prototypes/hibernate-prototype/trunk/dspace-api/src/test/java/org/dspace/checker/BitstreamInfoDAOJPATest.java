package org.dspace.checker;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

public class BitstreamInfoDAOJPATest extends TestCase
{
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    private Context context;
    private BitstreamInfoDAOJPA bdao;
    
    public BitstreamInfoDAOJPATest() {
        super();
    }
    
    @Before
    public void setUp() {
        try
        {
            ConfigurationManager.loadConfig(CONFIG);
            context = new Context();
            bdao = new BitstreamInfoDAOJPA();            
        }
        catch (SQLException e)
        {       
            e.printStackTrace();
        }
    }
    
    @Test
    public void testFake() {
        Assert.assertTrue(true);
    }
//    
//    @Test
//    public void testFindAllItemBitstreamsId() throws SQLException {
//        int itemId = 1;
//        List<Integer> bids = bdao.findAllItemBitstreamsId(itemId, context);
//        Assert.assertNotNull(bids);
//    }
//    
//    @Test
//    public void testFindAllCollectionBitstreamsId() throws SQLException {
//        int collectionId = 2;
//        List<Integer> bids = bdao.findAllCollectionBitstreamsId(collectionId, context);
//        Assert.assertNotNull(bids);
//    }
//    
//    @Test
//    public void testFindAllCommunityBitstreamsId() throws SQLException {
//        int communityId = 24;
//        List<Integer> bids = bdao.findAllCommunityBitstreamsId(communityId, context);
//        Assert.assertNotNull(bids);
//    }
//    
//    @Test
//    public void testFindBitstreamInfoByBitstreamId() {
//        int bitstreamId=1;
//        BitstreamInfo bi = bdao.findBitstreamInfoByBitstreamId(bitstreamId, context);
//    }
}
