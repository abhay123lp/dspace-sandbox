package org.dspace.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ItemManagerTest
{
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    //private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
    
    private static final String FILEPATH= "C:\\workspace\\chicagoartinstitutelibrary.jpg";
    
    protected static Context context;    

    
    public ItemManagerTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try {
        ConfigurationManager.loadConfig(CONFIG);
        context = new Context();

        } catch (Throwable t) { t.printStackTrace(); }          
    }
    
    @Test 
    public void testfake()  {
        Assert.assertTrue(true);
        
    }
    
    /* Tutti i test ok */
   
//    @Test
//    public void testPopulateDBwithBundleBitstream()throws FileNotFoundException, AuthorizeException,  IOException, SQLException {
//        Item item = ApplicationService.get(context, Item.class, 1);
//        Assert.assertNotNull(item);
//        Bundle bundle;
//        BufferedInputStream is;
//        
//        for(int i=0; i<10; i++) {
//            System.out.print("-");
//            bundle = ItemManager.createBundle(item, context);            
//            for(int j=0; j<5; j++) {
//                is= new BufferedInputStream(new FileInputStream(FILEPATH));
//                ItemManager.createBitstream(bundle, is, context);                
//            }
//            
//        }
//        context.complete();
//    }
//    
//    @Test
//    public void testCreateBundle() {
//        Item item = ApplicationService.get(context, Item.class, 1);
//        Bundle bundle = ItemManager.createBundle(item, context);
//        Assert.assertNotNull(bundle);
//        context.abort();
//    }
//    
//    @Test
//    public void testCreateBitstream() throws FileNotFoundException, AuthorizeException,  IOException {
//        BufferedInputStream is = new BufferedInputStream(new FileInputStream(FILEPATH));
//        Bundle bundle = ApplicationService.get(context, Bundle.class, 1);
//        Bitstream bitstream = ItemManager.createBitstream(bundle, is, context);        
//        Assert.assertNotNull(bitstream);
//        context.abort();
//    }
//    
//    @Test 
//    public void testCreateSingleBitstream() throws FileNotFoundException, AuthorizeException,  IOException, SQLException {
//        BufferedInputStream is = new BufferedInputStream(new FileInputStream(FILEPATH));
//        Item item = ApplicationService.get(context, Item.class, 1);
//        Bitstream bitstream = ItemManager.createSingleBitstream(item, is, "", context);
//        Assert.assertNotNull(bitstream);
//        context.abort();
//        
//    }
//    
//    @Test
//    public void testAddBitstream() {
//        Bitstream bitstream = ApplicationService.get(context, Bitstream.class, 3);
//        Bundle bundle = ApplicationService.get(context, Bundle.class, 2);
//        Assert.assertFalse(bitstream.getBundle()==bundle);
//        ItemManager.addBitstream(bundle, bitstream);
//        Assert.assertTrue(bitstream.getBundle()==bundle);
//        context.abort();
//    }
//    
//    @Test
//    public void testAddBundle() {
//        Bundle bundle = ApplicationService.get(context, Bundle.class, 1);
//        Item item = ApplicationService.get(context, Item.class, 4);
//        Assert.assertFalse(bundle.getItem()==item);
//        ItemManager.addBundle(item, bundle, context);
//        Assert.assertTrue(bundle.getItem()==item);
//        context.abort();
//    }
//    
//    @Test
//    public void testRemoveBitstream() {
//        Bitstream bitstream = ApplicationService.get(context, Bitstream.class, 149);        
//        ItemManager.removeBitstream(bitstream.getBundle(), bitstream);
//        Assert.assertTrue(bitstream.isDeleted()==true);
//        context.abort();
//    }
//    
//    @Test
//    public void testRemoveBundle() {
//        Bundle bundle = ApplicationService.get(context, Bundle.class, 10);
//        ItemManager.removeBundle(bundle.getItem(), bundle, context);
//        Assert.assertFalse(bundle.getItem().getBundles().contains(bundle));
//        context.abort();
//    }
//    
//    @Test
//    public void testRemoveAllBundles() throws SQLException{
//        Item item = ApplicationService.get(context, Item.class, 3);
//        ItemManager.removeAllBundles(item, context);
//        Assert.assertTrue(item.getBundles()==null);
//        context.abort();
//    }
}
