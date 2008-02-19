package org.dspace.core;

import java.sql.SQLException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.CollectionFactory;
import org.junit.Before;
import org.junit.Test;

public class ApplicationServiceTest extends TestCase {
    
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    //private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
    
    protected static Context context;   
    
    public ApplicationServiceTest() {
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
    public void testfake()
    {
        Assert.assertTrue(true);
    }

//    @Test
//    public void testGet() {
//        Community community = ApplicationService.get(context, Community.class, 1);
//        Assert.assertNotNull(community);        
//        context.abort();
//    }
//
//    @Test
//    public void testUpdate() throws SQLException {
//        Community community = ApplicationService.get(context, Community.class, 2);
//        community.setName("testUpdate");
//        ApplicationService.update(context, Community.class, community);
//        Assert.assertEquals(community.getName(), "testUpdate");
//        context.abort();
//    }
//    
//    @Test
//    public void testDelete() throws SQLException {
//        Collection collection = ApplicationService.get(context, Collection.class, 2);
//        ApplicationService.delete(context, Collection.class, collection);
//        //Assert.assertNull(collection);
//        context.abort();
//    }
//    
//    @Test
//    public void testSave() throws  SQLException {
//        Collection collection = CollectionFactory.getInstance(context);
//        ApplicationService.save(context, Collection.class, collection);
//        Assert.assertNotNull(collection.getId());
//        context.abort();
//    }
//  @Test 
//  public void testCascadeSiteCommunity() {
//      Site site = new Site(context);
//      site.setName("Site for CascadeSiteCommunity");
//      Community community = site.createTopCommunity();
//      community.setName("Community for CascadeSiteCommunity");
//      
//      applicationService.save(context, Site.class, site);
//      
//      //Community community2 = applicationService.get(context, Community.class, community.getId());
//      applicationService.complete();
//      
//      //assertNotNull(community2);        
//  }
    
//  @Test //ok
//  public void testUpdateCommunity() {
//      
//      Community community = applicationService.get(context, Community.class, 1);
//      applicationService.commit();
//      
//      //assertNotNull(community);
//      community.setName("!Community");
//      
//      
//      applicationService.update(context, Community.class, community);
//      applicationService.complete();
//      
//  }
    
//  @Test 
//  public void testCascadeCommunityCollection() {
//      
//      Community community = applicationService.get(context, Community.class, 1);
//      //applicationService.commit();
//      
//      assertNotNull(community);
//  
//      Collection collection = community.createCollection();
//      collection.setName("123");
//              
//      applicationService.update(context, Community.class, community);
//      //applicationService.save(context, Collection.class, collection);
//      
//      applicationService.complete();
//      
//  }
//  
//  @Test 
//  public void testCascadeSiteCommunityCollection() {
//      Site site = new Site(context);
//      site.setName("AAA");
//      Community community = site.createTopCommunity();
//      community.setName("AAA");
////        Collection collection = community.createCollection();
////        collection.setName("AAA");
//      
//      applicationService.save(context, Site.class, site);
//
////        Collection retrieved = applicationService.get(context, Collection.class, collection.getId());
////        System.out.println("Update: prima del complete");
//      applicationService.complete();
////        System.out.println("Update: dopo il complete");
////        assertNotNull(retrieved);
//      
//  }
    
//  @Test //ok
//  public void testCascadeCollectionItem() {
//      Collection collection = applicationService.get(context, Collection.class, 13);
//      
//      collection.setName("Cascade collection -> item");
//                  
//      Item item = collection.createItem();
//      
//      applicationService.update(context, Collection.class, collection);
//      applicationService.complete();
//  }
    
//  @Test //ok
//  public void testCascadeSiteCommunityCollectionItem() {
//      Site site = new Site(context);
//      site.setName("Da site a item");
//      Community community = site.createTopCommunity();
//      community.setName("Cascade Site->Item");
//      Collection collection = community.createCollection();
//      collection.setName("Cascade Site->Item");
//      Item item = collection.createItem();
//      
//      applicationService.save(context, Site.class, site);
//      applicationService.complete();
//  }
    
//  @Test //ok
//  public void testCascadeSiteCommunityCollectionItemBundle() {
//      Site site = new Site(context);
//      site.setName("Da site a item");
//      Community community = site.createTopCommunity();
//      community.setName("Cascade Site->Item");
//      Collection collection = community.createCollection();
//      collection.setName("Cascade Site->Item");
//      Item item = collection.createItem();
//      Bundle bundle = item.createBundle();
//      bundle.setName("Cascade Item->Bundle");
//      applicationService.save(context, Site.class, site);
//      applicationService.complete();
//  }
    
//  @Test 
//  public void testCascadeItemBundle() {
//      //List<Bundle> listabundles;
//      Item item = applicationService.get(context, Item.class, 1);
//      //applicationService.commit();
//
//      //assertNotNull(item);
//
//      Bundle bundle = item.createBundle();
//      System.out.println("id -> " + bundle.getId());
//      bundle.setName("BBB");
////        for (Bundle bnd: item.getBundles())
////        {
////            bnd.setName("aaaa");
////        }
////        applicationService.save(context, Bundle.class, bundle); 
////        System.out.println("--Andrea");
//      applicationService.update(context, Item.class, item);
//      System.out.println("id -> " + bundle.getId());
//      applicationService.complete();
//      System.out.println("id -> " + bundle.getId());
//  }
    
//  @Test
//  public void testCascadeFromSiteToBundle() {
//      Site site = new Site(context);
//      site.setName("Cascade Site->Bundle");
//      Community community = site.createTopCommunity();
//      community.setName("Cascade Site->Bundle");
//      Collection collection = community.createCollection();
//      collection.setName("Cascade Site->Bundle");
//      Item item = collection.createItem();
//      Bundle bundle = item.createBundle();
//      bundle.setName("Cascade Site->Bundle");
//      
//      applicationService.save(context, Site.class, site);
//      applicationService.complete();
//      
//  }
    
//  @Test
//  public void testCascadeBundleBitstream() throws FileNotFoundException, AuthorizeException,  IOException{
//      Bundle bundle = applicationService.get(context, Bundle.class, 1);
//      BufferedInputStream bis = new BufferedInputStream(new FileInputStream("/home/daniele/Immagini/eroe.jpg"));
//      
//      Bitstream bitstream = bundle.createBitstream(bis);
//      bitstream.setName("A");
//      applicationService.commit();
//      
//      bitstream.setName("B");
//      //applicationService.update(context, Bundle.class, bundle);
//      applicationService.complete();
//      
//  }
    
//  @Test
//  public void testCommunitySetLogo() throws FileNotFoundException, AuthorizeException,  IOException {
//      BufferedInputStream bis = new BufferedInputStream(new FileInputStream("/home/daniele/Immagini/eroe.jpg"));
//      Community community = applicationService.get(context, Community.class, 1);
//              
//      community.setLogo(bis);
//      applicationService.commit();
//      
//      BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream("/home/daniele/Immagini/car.png"));
//      community.setLogo(bis2);
//      //applicationService.update(context, Community.class, community);
//      applicationService.complete();
//  }
//  @Test
//  public void testEperson() {
//      EPerson ep = new EPerson(context);
//      ep.setEmail("bbbb");
//      applicationService.save(context, EPerson.class, ep);
//      applicationService.complete();
//  }
//  @Test
//  public void testgetEPerson() {
//      EPerson ep = applicationService.get(context, EPerson.class, 1);
//      ep.setEmail("ciao!");
//      applicationService.update(context, EPerson.class, ep);
//      applicationService.complete();
//  }
    
//  @Test
//  public void testMetadataCommunity() {
//      Community community = applicationService.get(context, Community.class, 2);
//      community.setMetadata("name", null);
//      applicationService.update(context, Community.class, community);
//      applicationService.complete();
//  }

}
