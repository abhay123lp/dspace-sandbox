package org.dspace.authorize;

import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifierMint;
import org.dspace.uri.SimpleIdentifier;

public class ResourcePolicyFactory
{
    public static ResourcePolicy getInstance(Context context) {
        SimpleIdentifier sid = ObjectIdentifierMint.mintSimple();
        ResourcePolicy rp = new ResourcePolicy(context);
        rp.setSimpleIdentifier(sid);
        
        //FIXME associare a resourcepolicy un uuid
        //row.setColumn("uuid", sid.getUUID().toString());
        
        return rp;
    }
}
