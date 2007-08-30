package org.dspace.sword;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;

public class CollectionLocation
{
	public static Logger log = Logger.getLogger(CollectionLocation.class);
	
	public String getLocation(Collection collection)
		throws DSpaceSWORDException
	{
		return this.getBaseUrl() + "/" + collection.getHandle();
	}
	
	public Collection getCollection(Context context, String location)
		throws DSpaceSWORDException
	{
		try
		{
			String baseUrl = this.getBaseUrl();
			if (baseUrl.length() == location.length())
			{
				throw new DSpaceSWORDException("The deposit URL is incomplete");
			}
			String handle = location.substring(baseUrl.length());
			if (handle.startsWith("/"))
			{
				handle = handle.substring(1);
			}
			if ("".equals(handle))
			{
				throw new DSpaceSWORDException("The deposit URL is incomplete");
			}
			
			DSpaceObject dso = HandleManager.resolveToObject(context, handle);
			
			if (!(dso instanceof Collection))
			{
				throw new DSpaceSWORDException("The deposit URL does not resolve to a valid collection");
			}
			
			return (Collection) dso;
		}
		catch (SQLException e)
		{
			log.error("Caught exception:", e);
			throw new DSpaceSWORDException("There was a problem resolving the collection", e);
		}
	}
	
	private String getBaseUrl()
		throws DSpaceSWORDException
	{
		String depositUrl = ConfigurationManager.getProperty("sword.deposit.url");
		if (depositUrl == null || "".equals(depositUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSWORDException("Unable to construct deposit urls, due to missing/invalid config in sword.deposit.url and/or dspace.url");
			}
			depositUrl = dspaceUrl + "/dspace-sword/deposit";
		}
		return depositUrl;
	}
}
