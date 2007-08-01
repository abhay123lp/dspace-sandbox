package org.dspace.sword;

import org.dspace.core.Context;
import org.dspace.eperson.AuthenticationManager;
import org.dspace.eperson.AuthenticationMethod;

public class SWORDAuthentication
{
	public boolean authenticates(Context context, String un, String pw)
	{
		int auth = AuthenticationManager.authenticate(context, un, pw, null, null);
		if (auth == AuthenticationMethod.SUCCESS)
		{
			return true;
		}
		return false;
	}
}
