package org.dspace.core;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
public class ArchiveManagerTest
{
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
	//private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
	
    protected static Context context;    
    
    public ArchiveManagerTest() {
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
    public void testfake() {
        Assert.assertTrue(true);
        
    }
    
    /* Tutti i test ok */
    
//    @Test //populates the db, it's not a real test
//    public void testPopulateDBwithCommunityCollectionSubcommunityItem() throws SQLException{
//    	Community community;
//    	Collection collection;
//    	
//    	for(int i=0; i<5; i++) {
//    		community=ArchiveManager.createCommunity(null, context);
//    		for(int j=0; j<5; j++) {
//    		    ArchiveManager.createCommunity(community, context);
//    		    collection=ArchiveManager.createCollection(community, context);
//    			for(int k=0; k<3; k++)
//    				ArchiveManager.createItem(collection, context);
//    		}
//    		ApplicationService.save(context, Community.class, community);    		
//    	}
//    	
//    	context.complete();
//    }
    
//    @Test
//    public void testCreateCommunity() throws SQLException{
//        Community community = ArchiveManager.createCommunity(null, context);
//        Assert.assertNotNull(community); 
//        context.abort();
//    }
//    
//    @Test
//    public void testCreateSubCommunity() {
//        Community parent = ApplicationService.get(context, Community.class, 2);
//        Community child = ArchiveManager.createCommunity(parent, context);
//        Assert.assertNotNull(child);
//        context.abort();
//    }
//    
//    @Test
//    public void testCreateCollection() {
//        Community parent = ApplicationService.get(context, Community.class, 3);
//        Collection collection = ArchiveManager.createCollection(parent, context); 
//        Assert.assertNotNull(collection);
//        context.abort();
//    }
//    
//    @Test
//    public void testCreateItem() {
//        Collection parent = ApplicationService.get(context, Collection.class, 1);
//        Item item = ArchiveManager.createItem(parent, context);
//        Assert.assertNotNull(item);
//        context.abort();
//    }
//    
//    @Test
//    public void testAddCommunity() {
//        Community community = ApplicationService.get(context, Community.class, 5);
//        Community futureParent = ApplicationService.get(context, Community.class, 2);
//        ArchiveManager.addCommunity(futureParent, community);
//        Assert.assertTrue(futureParent.getSubCommunities().contains(community));
//        Assert.assertTrue(community.getParentCommunities().contains(futureParent));
//        context.abort();
//    }
//    
//    @Test
//    public void testAddCollection() {
//        Community community = ApplicationService.get(context, Community.class, 5);
//        Collection collection = ApplicationService.get(context, Collection.class, 1);
//        ArchiveManager.addCollection(community, collection);
//        Assert.assertTrue(community.getCollections().contains(collection));
//        Assert.assertTrue(collection.getCommunities().contains(community));
//        context.abort();
//    }
//    
//    @Test
//    public void testAddItem() {
//        Collection collection = ApplicationService.get(context, Collection.class, 3);
//        Item item = ApplicationService.get(context, Item.class, 2);
//        ArchiveManager.addItem(collection, item);
//        Assert.assertTrue(collection.getItems().contains(item));
//        Assert.assertTrue(item.getCollections().contains(collection));
//        context.abort();
//    }
//    
//     @Test
//     public void testRemoveAndDeleteSubCommunity() throws IOException, AuthorizeException {
//         Community parent = ApplicationService.get(context, Community.class, 15);
//         Community child = ApplicationService.get(context, Community.class, 11);
//         int childrenbefore = parent.getSubCommunities().size();
//         ArchiveManager.removeCommunity(parent, child, context);
//         int childrenafter = parent.getSubCommunities().size();
//         
//         Assert.assertFalse(childrenbefore==childrenafter);
//         
//         context.abort();
//         
//         //prima del complete non e' null, viene cancellato al complete
//         //Assert.assertNull(child);
//     }
//     
//     @Test
//     public void testRemoveAndDeleteTopCommunity() throws IOException, AuthorizeException {
//         Community community = ApplicationService.get(context, Community.class, 3);
//         ArchiveManager.removeCommunity(null, community, context);
//         context.abort();
//     }
//     
//     @Test
//     public void testRemoveAndDeleteCollection() throws IOException, AuthorizeException {
//    	 Collection collection = ApplicationService.get(context, Collection.class, 16);
//    	 ArchiveManager.removeCollection(collection.getCommunities().get(0), collection, context);    	
//    	 context.abort();
//     }    
}
