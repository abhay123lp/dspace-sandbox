package org.dspace.sword;

import org.dspace.core.Context;
import org.purl.sword.base.Deposit;

public class SWORDIngesterFactory
{
	public static SWORDIngester getInstance(Context context, Deposit deposit) 
		throws DSpaceSWORDException
	{
		// so there is only one implementation at the moment, and this is it
		return new SWORDMETSIngester();
	}
}
