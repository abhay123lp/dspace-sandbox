package org.dspace.sword;

import org.dspace.core.Context;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;

public interface SWORDIngester
{
	DepositResponse ingest(Context context, Deposit deposit) throws DSpaceSWORDException;
}
