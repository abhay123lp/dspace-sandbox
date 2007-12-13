package org.dspace.eperson.factory;

import org.dspace.eperson.EPerson;
import org.dspace.core.Context;

public class EPersonFactory {
	public static EPerson getInstance(Context context) {
		return new EPerson(context);
	}
}
