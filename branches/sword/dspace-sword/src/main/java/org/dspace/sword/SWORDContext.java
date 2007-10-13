package org.dspace.sword;

import org.dspace.eperson.EPerson;

public class SWORDContext 
{
	private EPerson authenticated = null;
	
	private EPerson onBehalfOf = null;

	public EPerson getAuthenticated() 
	{
		return authenticated;
	}

	public void setAuthenticated(EPerson authenticated) 
	{
		this.authenticated = authenticated;
	}

	public EPerson getOnBehalfOf() 
	{
		return onBehalfOf;
	}

	public void setOnBehalfOf(EPerson onBehalfOf) 
	{
		this.onBehalfOf = onBehalfOf;
	}
	
	
	
}
