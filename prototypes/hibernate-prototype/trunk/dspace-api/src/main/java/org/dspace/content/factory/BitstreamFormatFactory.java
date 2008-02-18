package org.dspace.content.factory;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BitstreamFormatFactory {
    public static BitstreamFormat getInstance(Context context) {
        UUID uuid = UUID.randomUUID();      
        ObjectIdentifier oid = new ObjectIdentifier(uuid);
        BitstreamFormat bf = new BitstreamFormat(context);
        bf.setIdentifier(oid);
        
        return bf;
    }
}
