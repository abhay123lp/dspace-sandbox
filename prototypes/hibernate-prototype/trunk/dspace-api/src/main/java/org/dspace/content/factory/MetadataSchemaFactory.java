package org.dspace.content.factory;

import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

public class MetadataSchemaFactory
{
    public static MetadataSchema getInstance(Context context) throws AuthorizeException {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }
//        UUID uuid = UUID.randomUUID();      
//        ObjectIdentifier oid = new ObjectIdentifier(uuid);
        //FIXME settare uuid (almeno)
        MetadataSchema metadataSchema = new MetadataSchema(context);
        return metadataSchema;
    }
}
