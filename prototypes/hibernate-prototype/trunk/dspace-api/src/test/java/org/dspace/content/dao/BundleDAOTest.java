package org.dspace.content.dao;

import java.sql.SQLException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

public class BundleDAOTest extends TestCase 
{
    private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    private BundleDAO bdao;
    private Context context;
    
    public BundleDAOTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try
        {
            ConfigurationManager.loadConfig(CONFIG);
            context = new Context();
            bdao = BundleDAOFactory.getInstance(context);
        }
        catch (SQLException e){e.printStackTrace();}
        
    }
    
    @Test
    public void testFindBundleByName() {
        Item item = ApplicationService.get(context, Item.class, 1);
        Bundle bundle = bdao.findBundleByName(item, "", context.getEntityManager());
        Assert.assertNotNull(bundle);
        context.abort();
    }
}
