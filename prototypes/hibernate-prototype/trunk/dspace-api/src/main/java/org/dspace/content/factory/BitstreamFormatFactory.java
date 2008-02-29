package org.dspace.content.factory;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

public class BitstreamFormatFactory {
    
    protected Logger log = Logger.getLogger(BitstreamFormatDAO.class);
    
    public static BitstreamFormat getInstance(Context context) throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can create bitstream formats");
        }
        UUID uuid = UUID.randomUUID();
        ObjectIdentifier oid = new ObjectIdentifier(uuid);
        BitstreamFormat bf = new BitstreamFormat(context);
        bf.setIdentifier(oid);
        
        return bf;
    }
}
