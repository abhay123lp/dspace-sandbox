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
	
	private String username;
	
	private String onBehalfOf;
	
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	public void setUsername(String un)
	{
		this.username = un;
	}
	
	public void setOnBehalfOf(String obo)
	{
		this.onBehalfOf = obo;
	}
	
	public ServiceDocument getServiceDocument()
		throws DSpaceSWORDException
	{
		try
		{
			ServiceLevel sl = ServiceLevel.ONE;
			boolean noOp = true;
			boolean verbose = true;

			Service service = new Service(sl, noOp, verbose);
			ServiceDocument sd = new ServiceDocument(service);

			String ws = ConfigurationManager.getProperty("dspace.name");
			Workspace workspace = new Workspace();
			workspace.setTitle(ws);

			Collection[] cols = Collection.findAuthorized(context, null, Constants.ADD);
			
			for (int i = 0; i < cols.length; i++)
			{
				org.purl.sword.base.Collection scol = this.buildSwordCollection(cols[i]);
				workspace.addCollection(scol);
			}
			
			return sd;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException("There was a problem obtaining the list of authorized collections", e);
		}
	}
	
	private org.purl.sword.base.Collection buildSwordCollection(Collection col)
	{
		org.purl.sword.base.Collection scol = new org.purl.sword.base.Collection();
		
		// prepare the parameters to be put in the sword collection
		String location = HandleManager.getCanonicalForm(col.getHandle()); // FIXME: is this the URL it wants?
		String title = col.getMetadata("name");
		String collectionPolicy = col.getLicense();
		String treatment = ""; // FIXME: what sort of info is this?
		// String namespace = "";  FIXME: this might be internal to SWORD - difficult to tell
		String dcAbstract = col.getMetadata("short_description");
		boolean mediation = true;
		
		// the list of mime types that we accept
		String zip = "application/zip";
		
		// load up the sword collection
		scol.setLocation(location);
		scol.setTitle(title);
		scol.setCollectionPolicy(collectionPolicy);
		scol.setTreatment(treatment);
		scol.setAbstract(dcAbstract);
		scol.setMediation(mediation);
		scol.addAccepts(zip);
		
		return scol;
	}
}
