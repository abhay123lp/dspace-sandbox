package org.dspace.adapters.rdf;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.AbstractXMLDisseminationCrosswalk;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.jdom.Namespace;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandlerException;
import org.xml.sax.ContentHandler;

/**
 * @author mdiggory
 *
 */
public class RDFDisseminationCrosswalk extends AbstractXMLDisseminationCrosswalk
{

    /**
     * RDF Namespace
     */
    public static final String RDF_URI = RDF.NAMESPACE;

    
    /* (non-Javadoc)
     * @see org.dspace.content.crosswalk.SAXDisseminationCrosswalk#canDisseminate(org.dspace.core.Context, org.dspace.content.DSpaceObject)
     */
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        return canDisseminate(dso);
    }

    /* (non-Javadoc)
     * @see org.dspace.content.crosswalk.DisseminationCrosswalk#canDisseminate(org.dspace.content.DSpaceObject)
     */
    public boolean canDisseminate(DSpaceObject dso)
    {
        if (dso instanceof Item)
            return true;

        return false;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.crosswalk.SAXDisseminationCrosswalk#disseminate(org.dspace.core.Context, org.dspace.content.DSpaceObject, org.xml.sax.ContentHandler)
     */
    public void disseminate(Context context, DSpaceObject dso,
            ContentHandler handler) throws CrosswalkException, IOException,
            SQLException, AuthorizeException
    {
        try
        {
            DSpaceXMLReader reader = new DSpaceXMLReader();
            reader.setContext(context);
            reader.setContentHandler(handler);
            reader.parse(dso);
        }
        catch (RDFHandlerException e)
        {
            throw new CrosswalkException(e.getMessage(),e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.content.crosswalk.StreamDisseminationCrosswalk#getMIMEType()
     */
    public String getMIMEType()
    {
        return "application/rdf+xml";
    }

    /* (non-Javadoc)
     * @see org.dspace.content.crosswalk.DisseminationCrosswalk#getNamespaces()
     */
    public org.jdom.Namespace[] getNamespaces()
    {
        return new Namespace[] { Namespace.getNamespace("rdf", RDF_URI) };
    }

    /**
     * Hack to support xsd schema requirement in OAI. Avoid using this schema
     * otherwise.
     * 
     * @see org.dspace.content.crosswalk.DisseminationCrosswalk#getSchemaLocation()
     */
    public String getSchemaLocation()
    {
        return "http://www.openarchives.org/OAI/2.0/rdf.xsd";
    }

}
