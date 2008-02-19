package org.dspace.content.factory;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.uri.ObjectIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BitstreamFactory {
	public static Bitstream getInstance(Context context, InputStream is) throws AuthorizeException, IOException {
		UUID uuid = UUID.randomUUID();		
		ObjectIdentifier oid = new ObjectIdentifier(uuid);		
		Bitstream b = BitstreamStorageManager.store(context, is);
		b.setIdentifier(oid);
		
		return b;
	}
}
