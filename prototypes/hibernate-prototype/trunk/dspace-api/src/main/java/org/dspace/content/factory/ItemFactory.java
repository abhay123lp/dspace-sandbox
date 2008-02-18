package org.dspace.content.factory;

import java.util.UUID;
import org.dspace.content.Item;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.Context;
import java.util.Date;

public class ItemFactory {
	public static Item getInstance(Context context) {
		Date date = new Date();
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Item i = new Item(context);
		i.setIdentifier(oid);
		i.setLastModified(date);
		return i;
	}
}
