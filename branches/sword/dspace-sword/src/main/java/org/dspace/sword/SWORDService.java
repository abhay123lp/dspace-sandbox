package org.dspace.sword;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceLevel;
import org.purl.sword.base.Service;
import org.purl.sword.base.Workspace;

import org.dspace.content.Collection;
import java.sql.SQLException;
import org.dspace.handle.HandleManager;

import org.dspace.core.ConfigurationManager;

public class SWORDService
{
	public static Logger log = Logger.getLogger(SWORDService.class);
	
	private Context context;
	
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	public ServiceDocument getServiceDocument()
		throws DSpaceSWORDException
	{
		try
		{
			// DSpace will support the top level service option
			ServiceLevel sl = ServiceLevel.ONE;
			
			// can we dry-run requests
			boolean noOp = true;
			
			// can we be verbose in our actions
			boolean verbose = true;

			// construct a new service document
			Service service = new Service(sl, noOp, verbose);

			// set the title of the workspace as per the name of the DSpace installation
			String ws = ConfigurationManager.getProperty("dspace.name");
			Workspace workspace = new Workspace();
			workspace.setTitle(ws);

			// locate the collections to which the authenticated user has ADD rights
			Collection[] cols = Collection.findAuthorized(context, null, Constants.ADD);
			
			// add the permissable collections to the workspace
			for (int i = 0; i < cols.length; i++)
			{
				org.purl.sword.base.Collection scol = this.buildSwordCollection(cols[i]);
				workspace.addCollection(scol);
			}
			
			service.addWorkspace(workspace);
			
			ServiceDocument sd = new ServiceDocument(service);
			return sd;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException("There was a problem obtaining the list of authorized collections", e);
		}
	}
	
	private org.purl.sword.base.Collection buildSwordCollection(Collection col)
		throws DSpaceSWORDException
	{
		org.purl.sword.base.Collection scol = new org.purl.sword.base.Collection();
		
		// prepare the parameters to be put in the sword collection
		CollectionLocation cl = new CollectionLocation();
		String location = cl.getLocation(col);
		
		// collection title is just its name
		String title = col.getMetadata("name");
		
		// the collection policy is the licence to which the collection adheres
		String collectionPolicy = col.getLicense();
		
		// FIXME: what is the treatment?
		// String treatment = " ";
		
		// FIXME: this might be internal to SWORD - difficult to tell.  What is it?
		// String namespace = "";
		
		// abstract is the short description of the collection
		String dcAbstract = col.getMetadata("short_description");
		
		// FIXME: what does it mean to support mediation?
		boolean mediation = true;
		
		// the list of mime types that we accept
		// for the time being, we just take a zip, and we have to trust what's in it
		String zip = "application/zip";
		
		// load up the sword collection
		scol.setLocation(location);
		
		if (title != null && !"".equals(title))
		{
			scol.setTitle(title);
		}
		
		if (collectionPolicy != null && !"".equals(collectionPolicy))
		{
			scol.setCollectionPolicy(collectionPolicy);
		}
		
		// FIXME: leave the treatment out for the time being
		// scol.setTreatment(treatment);
		
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}
		
		scol.setMediation(mediation);
		scol.addAccepts(zip);
		
		return scol;
	}
}
