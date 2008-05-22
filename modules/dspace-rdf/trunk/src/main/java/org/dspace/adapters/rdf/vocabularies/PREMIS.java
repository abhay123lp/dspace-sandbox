package org.dspace.adapters.rdf.vocabularies;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class PREMIS
{
    private static final ValueFactory vf = ValueFactoryImpl.getInstance();

    public static final String NAMESPACE = "http://www.loc.gov/standards/premis/v1#";

    public static final URI NS = vf.createURI(NAMESPACE);
    
    public static final URI messageDigest = vf.createURI(NAMESPACE, "messageDigest");
    
    public static final URI messageDigestAlgorithm = vf.createURI(NAMESPACE, "messageDigestAlgorithm");
    
    public static final URI messageDigestOriginator = vf.createURI(NAMESPACE, "messageDigestOriginator");
    
    public static final URI size = vf.createURI(NAMESPACE, "size");
    
}
