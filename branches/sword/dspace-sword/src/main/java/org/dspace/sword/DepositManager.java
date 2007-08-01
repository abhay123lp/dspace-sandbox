package org.dspace.sword;

import org.dspace.core.Context;

import org.purl.sword.base.SWORDDeposit;
import org.purl.sword.base.SWORDDepositResponse;

public class DepositManager
{
	private Context context;
	
	private String username;
	
	private SWORDDeposit deposit;
	
	/**
	 * @param context the context to set
	 */
	public void setContext(Context context)
	{
		this.context = context;
	}

	/**
	 * @param deposit the deposit to set
	 */
	public void setDeposit(SWORDDeposit deposit)
	{
		this.deposit = deposit;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	public SWORDDepositResponse deposit()
	{
		return null;
	}
}
