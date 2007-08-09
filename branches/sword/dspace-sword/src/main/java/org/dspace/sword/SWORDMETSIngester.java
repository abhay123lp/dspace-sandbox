package org.dspace.sword;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;


public class SWORDMETSIngester implements SWORDIngester
{
	public static Logger log = Logger.getLogger(SWORDMETSIngester.class);
	
	/* (non-Javadoc)
	 * @see org.dspace.sword.SWORDIngester#ingest(org.dspace.core.Context, org.purl.sword.base.Deposit)
	 */
	public DepositResponse ingest(Context context, Deposit deposit) 
		throws DSpaceSWORDException
	{
		try
		{
			// the DSpaceMETSIngester requires an input stream
			File zip = deposit.getFile();
			InputStream is = new FileInputStream(zip);
			
			// FIXME: and now finish coding me ...
			
			return null;
		}
		catch (FileNotFoundException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException("Unable top locate the file to deposit", e);
		}
		
	}
	
}
