package org.dspace.content.factory;

import java.util.UUID;
import org.dspace.content.Site;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.Context;

public class SiteFactory {
	public static Site getInstance(Context context) {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Site s = new Site(context);
		s.setIdentifier(oid);
		
		return s;
	}
}
