package org.dspace.content.factory;

import java.util.UUID;
import org.dspace.content.Collection;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.Context;

public class CollectionFactory {
	public static Collection getInstance(Context context) {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Collection c = new Collection(context);
		c.setIdentifier(oid);
		
		return c;
	}
}
