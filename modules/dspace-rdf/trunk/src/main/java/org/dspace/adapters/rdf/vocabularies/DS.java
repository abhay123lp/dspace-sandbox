package org.dspace.adapters.rdf.vocabularies;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class DS
{

    private static final ValueFactory vf = ValueFactoryImpl.getInstance();

    public static final String NAMESPACE = "http://www.dspace.org/objectModel#";
    
    public static final URI NS = vf.createURI(NAMESPACE);
        
    public static final URI Bitstream = vf.createURI(NAMESPACE, "Bitstream");
    
    public static final URI Bundle = vf.createURI(NAMESPACE, "Bundle");

    public static final URI collection = vf.createURI(NAMESPACE, "collection");
    
    public static final URI Collection = vf.createURI(NAMESPACE, "Collection");
    
    public static final URI collectionURL = vf.createURI(NAMESPACE, "collectionURL");

    public static final URI community = vf.createURI(NAMESPACE, "community");

    public static final URI Community= vf.createURI(NAMESPACE, "Community");

    public static final URI communityURL = vf.createURI(NAMESPACE, "communityURL");
    
    public static final URI EPerson = vf.createURI(NAMESPACE, "EPerson");
    
    public static final URI Group = vf.createURI(NAMESPACE, "Group");

    public static final Value Item = vf.createURI(NAMESPACE, "Item");

    public static final URI logo = vf.createURI(NAMESPACE, "logoURL");

    public static final URI Object = vf.createURI(NAMESPACE, "DSpaceObject");
    
    public static final URI Policy = vf.createURI(NAMESPACE, "Policy");
    
    public static final URI Site = vf.createURI(NAMESPACE, "Site");

}
