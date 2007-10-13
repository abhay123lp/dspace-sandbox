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

import org.dspace.content.Collection;
import java.io.IOException;
import org.dspace.authorize.AuthorizeException;
import java.sql.SQLException;

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
		String handle = result.getHandle();
		int state = Deposit.CREATED;
		if (handle == null || "".equals(handle))
		{
			state = Deposit.ACCEPTED;
		}
		DepositResponse response = new DepositResponse(state);
		DSpaceATOMEntry dsatom = new DSpaceATOMEntry();
		SWORDEntry entry = dsatom.getSWORDEntry(result.getItem(), handle, deposit.isNoOp());
		
		// if this was a no-op, we need to remove the files we just
		// deposited, and abort the transaction
		String nooplog = "";
		if (deposit.isNoOp())
		{
			this.undoDeposit(result);
			nooplog = "NoOp Requested: Removed all traces of submission; \n\n";
		}
		
		entry.setNoOp(deposit.isNoOp());
		
		if (deposit.isVerbose())
		{
			String verboseness = result.getVerboseDescription();
			if (verboseness != null && !"".equals(verboseness))
			{
				entry.setVerboseDescription(result.getVerboseDescription() + nooplog);
			}
		}
		
		response.setEntry(entry);
		
		return response;
	}
	
	private void verify()
		throws DSpaceSWORDException
	{
		// FIXME: please implement
		// in reality, all this is done higher up the stack, so we don't
		// need to worry!
	}
	
	private void undoDeposit(DepositResult result)
		throws DSpaceSWORDException
	{
		try
		{
			// obtain the item's owning collection (there can be only one)
			// and ask it to remove the item.  Although we're going to abort
			// the context, so that this nevers gets written to the db,
			// it will get rid of the files on the disk
			Item item = result.getItem();
			Collection collection = item.getOwningCollection();
			collection.removeItem(item);

			// abort the context, so no database changes are written
			if (context != null && context.isValid())
			{
				context.abort();
			}
		}
		catch (IOException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			log.error("authentication problem; caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}
	
	
}
