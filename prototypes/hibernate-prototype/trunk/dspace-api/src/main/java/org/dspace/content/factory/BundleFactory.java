package org.dspace.content.factory;

import java.util.UUID;
import org.dspace.content.Bundle;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.Context;

public class BundleFactory {
	public static Bundle getInstance(Context context) {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Bundle b = new Bundle(context);
		b.setIdentifier(oid);
		return b;
	}
}
