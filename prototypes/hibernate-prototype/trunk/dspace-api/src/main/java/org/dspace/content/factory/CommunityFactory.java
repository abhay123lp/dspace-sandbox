package org.dspace.content.factory;

import java.util.UUID;
import org.dspace.content.Community;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.Context;

public class CommunityFactory {
	public static Community getInstance(Context context) {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Community c = new Community(context);
		c.setIdentifier(oid);
		
		return c;
	}
}
