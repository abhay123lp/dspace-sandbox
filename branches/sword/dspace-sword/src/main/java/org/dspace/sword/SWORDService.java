package org.dspace.sword;

import org.dspace.core.Context;

import org.purl.sword.base.ServiceDocument;

public class SWORDService
{
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
	{
		return null;
	}
}
