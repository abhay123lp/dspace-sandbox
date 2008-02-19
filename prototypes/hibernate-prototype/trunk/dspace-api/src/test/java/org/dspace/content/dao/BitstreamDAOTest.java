package org.dspace.content.dao;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.dspace.content.Bitstream;
import org.dspace.core.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

public class BitstreamDAOTest extends TestCase 
{
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    private Context context;
    private BitstreamDAO bdao;
    
    public BitstreamDAOTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try
        {
            ConfigurationManager.loadConfig(CONFIG);
            context = new Context();
            bdao = BitstreamDAOFactory.getInstance(context);            
        }
        catch (SQLException e)
        {       
            e.printStackTrace();
        }
    }
    
    @Test
    public void testGetDeletedBitstreams() {
        List<Bitstream> bitstreams = bdao.getDeletedBitstreams(context.getEntityManager());
        Assert.assertNotNull("bitstreams null in testGetDeletedBitstreams", bitstreams);
        Assert.assertTrue("bitstream not deleted in testGetDeletedBitstreams",bitstreams.get(0).isDeleted());
    }
    
    @Test
    public void testRetrieveInputStream() {
        Bitstream bitstream = ApplicationService.get(context, Bitstream.class, 183);
        InputStream is = bdao.retrieveInputStream(bitstream, context.getEntityManager());
        Assert.assertNotNull(is);
    }
}
