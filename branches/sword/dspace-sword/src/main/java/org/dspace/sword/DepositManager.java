package org.dspace.sword;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.content.Item;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDEntry;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DepositManager
{
	public static Logger log = Logger.getLogger(DepositManager.class);
	
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
		throws DSpaceSWORDException
	{
		// first we want to verify that the deposit is safe
		// check the checksums and all that stuff
		// This throws an exception if it can't verify the deposit
		this.verify();

		// Obtain the relevant ingester from the factory
		SWORDIngester si = SWORDIngesterFactory.getInstance(context, deposit);
		
		// do the deposit
		DepositResult result = si.ingest(context, deposit);

		// now construct the deposit response
		DepositResponse response = new DepositResponse(Deposit.CREATED);
		DSpaceATOMEntry dsatom = new DSpaceATOMEntry();
		SWORDEntry entry = dsatom.getSWORDEntry(result.getItem());
		entry.setNoOp(deposit.isNoOp());
		entry.setVerboseDescription(result.getVerboseDescription());
		response.setEntry(entry);
		
		// if this was a no-op, we need to remove the files we just
		// deposited, and remove abort the transaction
		if (deposit.isNoOp())
		{
			this.undoDeposit(result);
		}
		
		return response;
	}
	
	private void verify()
		throws DSpaceSWORDException
	{
		// FIXME: please implement
		
		// is this going to be done elsewhere (in the front end implementation)?
	}
	
	private void undoDeposit(DepositResult result)
	{
		// FIXME: what do we need to do to know how to do this?
	}
	
	
}
