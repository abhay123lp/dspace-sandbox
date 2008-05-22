package org.dspace.adapters.rdf.vocabularies;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class DS
{

    private static final ValueFactory vf = ValueFactoryImpl.getInstance();

    public static final String NAMESPACE = "http://www.dspace.org/objectModel#";
    
    public static final URI NS = vf.createURI(NAMESPACE);
        
    public static final URI Bitstream = vf.createURI(NAMESPACE, "Bitstream");
    
    public static final URI Bundle = vf.createURI(NAMESPACE, "Bundle");

    public static final URI Collection = vf.createURI(NAMESPACE, "Collection");
    
    public static final URI Community= vf.createURI(NAMESPACE, "Community");
    
    public static final URI EPerson = vf.createURI(NAMESPACE, "EPerson");
    
    public static final URI Group = vf.createURI(NAMESPACE, "Group");

    public static final URI Item = vf.createURI(NAMESPACE, "Item");

    public static final URI Object = vf.createURI(NAMESPACE, "DSpaceObject");
    
    public static final URI Policy = vf.createURI(NAMESPACE, "Policy");
    
    public static final URI Site = vf.createURI(NAMESPACE, "Site");

    public static final URI BitstreamFormat = vf.createURI(NAMESPACE, "BitstreamFormat");

    public static final URI hasCommunity = vf.createURI(NAMESPACE, "hasCommunity");
    
    public static final URI isPartOfSite = vf.createURI(NAMESPACE, "isPartOfSite");

    public static final URI isPartOfCommunity = vf.createURI(NAMESPACE, "isPartOfCommunity");

    public static final URI hasSubCommunity = vf.createURI(NAMESPACE, "hasSubCommunity");

    public static final URI isPartOfCollection = vf.createURI(NAMESPACE, "isPartOfCollection");
    
    public static final URI hasCollection = vf.createURI(NAMESPACE, "hasCollection");
    
    public static final URI logo = vf.createURI(NAMESPACE, "logo");
    
    public static final URI hasItem = vf.createURI(NAMESPACE, "hasItem");

    public static final URI isPartOfItem = vf.createURI(NAMESPACE, "isPartOfItem");
    
    public static final URI hasBitstream = vf.createURI(NAMESPACE, "hasBitstream");
    
    public static final URI hasBitstreamFormat = vf.createURI(NAMESPACE, "hasBitstreamFormat");

    public static URI support = vf.createURI(NAMESPACE, "support");
    
}

