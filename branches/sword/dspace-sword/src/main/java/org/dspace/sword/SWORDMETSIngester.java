package org.dspace.sword;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;

import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.core.PluginInstantiationException;
import org.dspace.core.PluginManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;

import java.util.Date;

public class SWORDMETSIngester implements SWORDIngester
{
	public static Logger log = Logger.getLogger(SWORDMETSIngester.class);
	
	private StringBuilder verboseDesc = new StringBuilder();
	
	private boolean verbose = false;
	
	/* (non-Javadoc)
	 * @see org.dspace.sword.SWORDIngester#ingest(org.dspace.core.Context, org.purl.sword.base.Deposit)
	 */
	public DepositResult ingest(Context context, Deposit deposit) 
		throws DSpaceSWORDException
	{
		try
		{
			// set the verbosity of the response
			this.verbose = deposit.isVerbose();
			message((new Date()).toString());
			message("Initialising Verbose Deposit");
			
			// the DSpaceMETSIngester requires an input stream
			InputStream is = deposit.getFile();
			
			// get the target collection
			String loc = deposit.getLocation();
			CollectionLocation cl = new CollectionLocation();
			Collection collection = cl.getCollection(context, loc);
			message("Performing deposit using location: " + loc);
			message("Location resolves to collection with handle: " + collection.getHandle());
			
			// load the plugin manager for the required configuration
			String cfg = ConfigurationManager.getProperty("sword.mets-ingester.package-ingester");
			if (cfg == null || "".equals(cfg))
			{
				cfg = "METS";  // default to METS
			}
			message("Using package manifest format: " + cfg);
			
			PackageIngester pi = (PackageIngester) PluginManager.getNamedPlugin(PackageIngester.class, cfg);
			
			// the licence is either in the zip or the mets manifest.  Either way
			// it's none of our business here
			String licence = null;
			
			// We don't need to include any parameters
			PackageParameters params = new PackageParameters();
			
			WorkspaceItem wsi = pi.ingest(context, collection, is, params, licence);
			if (wsi == null)
			{
				message("Failed to ingest the package; throwing exception");
				throw new DSpaceSWORDException("Package Ingest failed");
			}
			
			message("Ingest successful");
			message("Item created with internal identifier: " + wsi.getItem().getID());
			if (wsi.getItem().getHandle() != null)
			{
				message("Item created with external identifier: " + wsi.getItem().getHandle());
			}
			
			DepositResult dr = new DepositResult();
			dr.setItem(wsi.getItem());
			dr.setVerboseDescription(verboseDesc.toString());
			
			return dr;
		}
		catch (Exception e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}
	
	private void message(String msg)
	{
		if (this.verbose)
		{
			verboseDesc.append("\n\n");
			verboseDesc.append(msg);
		}
	}
}
