package org.dspace.content.factory;

import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class MetadataValueFactory
{
    public static MetadataValue getInstance(Context context) {
        //FIXME uuid?
        MetadataValue metadataValue = new MetadataValue(context);
        return metadataValue;
    }
}
