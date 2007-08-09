package org.dspace.sword;

import org.dspace.core.Context;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;

public class DepositManager
{
	private Context context;
	
	private Deposit deposit;
	
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
	public void setDeposit(Deposit deposit)
	{
		this.deposit = deposit;
	}

	public DepositResponse deposit()
	{
		return null;
	}
}
