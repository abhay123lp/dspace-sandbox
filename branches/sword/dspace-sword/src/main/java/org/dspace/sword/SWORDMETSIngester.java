package org.dspace.sword;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.dspace.content.DCDate;

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
import java.util.StringTokenizer;

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
			
			// update the item metadata to inclue the current time as
			// the updated date
			this.setUpdatedDate(wsi.getItem());
			
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
	
	private void setUpdatedDate(Item item)
		throws DSpaceSWORDException
	{
		String field = ConfigurationManager.getProperty("sword.updated.field");
		if (field == null || "".equals(field))
		{
			throw new DSpaceSWORDException("No configuration, or configuration is invalid for: sword.updated.field");
		}
		
		DCValue dc = this.configToDC(field, null);
		item.clearMetadata(dc.schema, dc.element, dc.qualifier, Item.ANY);
		DCDate date = new DCDate(new Date());
		item.addMetadata(dc.schema, dc.element, dc.qualifier, null, date.toString());
	}
	
	private DCValue configToDC(String config, String def)
	{
		DCValue dcv = new DCValue();
		dcv.schema = def;
		dcv.element= def;
		dcv.qualifier = def;
		
		StringTokenizer stz = new StringTokenizer(config, ".");
		dcv.schema = stz.nextToken();
		dcv.element = stz.nextToken();
		if (stz.hasMoreTokens())
		{
			dcv.qualifier = stz.nextToken();
		}
		
		return dcv;
	}
	
}
