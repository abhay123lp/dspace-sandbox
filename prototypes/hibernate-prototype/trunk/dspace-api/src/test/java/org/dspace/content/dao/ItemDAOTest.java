package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

public class ItemDAOTest extends TestCase 
{
    
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    Context context;
    ItemDAO idao;    
    
    public ItemDAOTest() {
        super();
    }

    @Before
    public void setUp()
    {
        try
        {
            ConfigurationManager.loadConfig(CONFIG);
            context = new Context();
            idao = ItemDAOFactory.getInstance(context);            
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
    
    @Test
    public void testGetItems() {
        List<Item> items = idao.getItems(context.getEntityManager());
        Assert.assertNotNull(items);
        Assert.assertTrue(items.get(0).isInArchive());
        Assert.assertFalse(items.get(0).isWithdrawn());  
        context.abort();
    }
    
    @Test
    public void testGetWithdrawnItems() {
        Collection collection = ApplicationService.get(context, Collection.class, 1);
        List<Item> items = idao.getWithdrawnItems(collection, context.getEntityManager());
        Assert.assertNotNull(items);
        Assert.assertTrue(items.get(0).isWithdrawn());
        context.abort();
    }
    
}
