package org.dspace.core;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
public class ArchiveManagerTest
{
    //private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
	private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
	
    protected static Context context;    
    private static ApplicationService applicationService;
    private static ArchiveManager archiveManager;
    
    public ArchiveManagerTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try {
        ConfigurationManager.loadConfig(CONFIG);
        context = new Context();
        applicationService = new ApplicationService();  
        archiveManager = new ArchiveManager();
        archiveManager.setApplicationService(applicationService);
        } catch (Throwable t) { t.printStackTrace(); }          
    }
    
//    @Test
//    public void testPopulateDBwithCommunityCollectionItem() {
//    	Community community;
//    	Collection collection;
//    	for(int i=0; i<5; i++) {
//    		community=archiveManager.createCommunity(null, context);
//    		for(int j=0; j<5; j++) {
//    			collection=archiveManager.createCollection(community, context);
//    			for(int k=0; k<3; k++)
//    				archiveManager.createItem(collection, context);
//    		}
//    		applicationService.save(context, Community.class, community);    		
//    	}
//    	
//    	applicationService.complete();
//    }
    
//    @Test
//    public void testCreateCommunity() {
//        Community community = archiveManager.createCommunity(null, context);
//        Assert.assertNotNull(community); 
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testCreateSubCommunity() {
//        Community parent = applicationService.get(context, Community.class, 2);
//        Community child = archiveManager.createCommunity(parent, context);
//        Assert.assertNotNull(child);
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testCreateCollection() {
//        Community parent = applicationService.get(context, Community.class, 3);
//        Collection collection = archiveManager.createCollection(parent, context); 
//        Assert.assertNotNull(collection);
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testCreateItem() {
//        Collection parent = applicationService.get(context, Collection.class, 1);
//        Item item = archiveManager.createItem(parent, context);
//        Assert.assertNotNull(item);
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testAddCommunity() {
//        Community community = applicationService.get(context, Community.class, 5);
//        Community futureParent = applicationService.get(context, Community.class, 2);
//        archiveManager.addCommunity(futureParent, community);
//        Assert.assertTrue(futureParent.getSubCommunities().contains(community));
//        Assert.assertTrue(community.getParentCommunities().contains(futureParent));
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testAddCollection() {
//        Community community = applicationService.get(context, Community.class, 5);
//        Collection collection = applicationService.get(context, Collection.class, 1);
//        archiveManager.addCollection(community, collection);
//        Assert.assertTrue(community.getCollections().contains(collection));
//        Assert.assertTrue(collection.getCommunities().contains(community));
//        applicationService.abort();
//    }
//    
//    @Test
//    public void testAddItem() {
//        Collection collection = applicationService.get(context, Collection.class, 3);
//        Item item = applicationService.get(context, Item.class, 2);
//        archiveManager.addItem(collection, item);
//        Assert.assertTrue(collection.getItems().contains(item));
//        Assert.assertTrue(item.getCollections().contains(collection));
//        applicationService.abort();
//    }
//    
//     @Test
//     public void testRemoveAndDeleteSubCommunity() {
//         Community parent = applicationService.get(context, Community.class, 2);
//         Community child = applicationService.get(context, Community.class, 7);
//         int childrenbefore = parent.getSubCommunities().size();
//         archiveManager.removeCommunity(parent, child, context);
//         int childrenafter = parent.getSubCommunities().size();
//         
//         Assert.assertFalse(childrenbefore==childrenafter);
//         
//         applicationService.abort();
//         
//         //prima del complete non e' null, viene cancellato al complete
//         //Assert.assertNull(child);
//     }
//     
//     @Test
//     public void testRemoveAndDeleteTopCommunity() {
//         Community community = applicationService.get(context, Community.class, 3);
//         archiveManager.removeCommunity(null, community, context);
//         applicationService.abort();
//     }
     
     @Test
     public void testRemoveAndDeleteCollection() {
    	 Collection collection = applicationService.get(context, Collection.class, 5);
    	 archiveManager.removeCollection(collection.getCommunities().get(0), collection, context);
    	 applicationService.complete();
     }
}
