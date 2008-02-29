package org.dspace.content.factory;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class MetadataFieldFactory
{
    public static MetadataField getInstance(Context context) throws AuthorizeException {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }
        //FIXME uuid?
        MetadataField metadataField = new MetadataField(context);
        return metadataField;
    }
}
