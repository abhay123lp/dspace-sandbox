package org.dspace.browse;

import org.dspace.core.ApplicationService;
import org.dspace.core.Context;

public class BrowseManager
{
    public static String getMaxSortValue(String sort, int itemID, Context context) throws BrowseException {
        if(sort=="sort_value") {
            int indexNumber = Integer.getInteger(sort);
            return new Integer(ApplicationService.findMaxForItemIndex(indexNumber, itemID, context)).toString();
        } else {
            return ApplicationService.findMaxForMetadataIndex(itemID, context);
        }
    }
    
    public static int getMaxOffset(String column, String value) {
        return 0;
    }

}
