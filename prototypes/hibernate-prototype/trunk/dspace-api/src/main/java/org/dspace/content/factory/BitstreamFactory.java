package org.dspace.content.factory;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.content.uri.ObjectIdentifier;
import java.util.UUID;

public class BitstreamFactory {
	public static Bitstream getInstance(Context context) {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);
		Bitstream b = new Bitstream(context);
		b.setIdentifier(oid);
		
		return b;
	}
}
