package org.dspace.eperson.factory;

import org.dspace.eperson.Group;
import org.dspace.core.Context;

public class GroupFactory {
	public static Group getInstance(Context context) {
		return new Group(context);
	}
}
