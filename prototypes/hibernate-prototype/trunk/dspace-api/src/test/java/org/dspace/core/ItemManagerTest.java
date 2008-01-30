package org.dspace.core;

import org.junit.Before;

public class ItemManagerTest
{
	//private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
    
    protected static Context context;    
    private static ApplicationService applicationService;
    
    public ItemManagerTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try {
        ConfigurationManager.loadConfig(CONFIG);
        context = new Context();
        applicationService = new ApplicationService();  
        
        } catch (Throwable t) { t.printStackTrace(); }          
    }
    
}
