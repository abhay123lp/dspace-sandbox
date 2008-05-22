package org.dspace.adapters.rdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;

public class AdapterSupport
{
    
    protected ValueFactory valueFactory = ValueFactoryImpl.getInstance();
    
    private RDFHandler rdfHandler = null;
    
    private Context context = null;
    
    protected String baseUri = ConfigurationManager.getProperty("dspace.url");
    
    private String metadataServiceUri = "/handle";

    public AdapterSupport()
    {
        super();
    }

    public Context getContext()
    {
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    /**
     * 
     * @param handler
     */
    public void setRDFHandler(RDFHandler handler)
    {
        this.rdfHandler = handler;
    }

    /**
     * 
     * @return
     */
    public RDFHandler getRDFHander()
    {
        return rdfHandler;
    }

    public void setBaseUri(String baseUri)
    {
        this.baseUri = baseUri;
        
    }
    
    /**
     * 
     * @return
     */
    public String getBaseUri()
    {
        return baseUri;
    }

    public String getMetadataServiceUri()
    {
        return metadataServiceUri;
    }

    public void setMetadataServiceUri(String metadataServiceUri)
    {
        this.metadataServiceUri = metadataServiceUri;
    }

    /**
     * Ret
     * 
     * @param handle
     * @return
     */
    protected String getMetadataURL(DSpaceObject object)
    {
        // Same URIs as history uses
        return getMetadataURL(object.getHandle());
    }

    protected String getMetadataURL(Site site)
    {
        return getBaseUri();
    }

    public String getMetadataURL(String identifier)
    {
        // Same URIs as history uses
        return getBaseUri() + getMetadataServiceUri() + "/" + identifier;
    }

    /**
     * Return the bitstream location of the the Collections Logo
     * 
     * @param bitstream
     * @return
     * @throws UnsupportedEncodingException 
     */
    protected String getBitstreamURL(Bitstream bitstream)
    {
        String url = getBaseUri()
        + "/retrieve/"
        + bitstream.getID();
        
        try
        {
            url += "/" + Util.encodeBitstreamName(bitstream.getName(),
                    Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            url += "/" + bitstream.getName();
            e.printStackTrace();
        }
        
        return url;
    }

    /**
     * Ret
     * 
     * @param handle
     * @return
     */
    protected String getBitstreamURL(Item item, Bitstream bitstream)
    {
        String url = getBaseUri() + "/bitstream/handle/" + item.getHandle() + "/"
        + bitstream.getSequenceID();
        
        try
        {
            url += "/" + Util.encodeBitstreamName(bitstream.getName(),
                    Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            url += "/" + bitstream.getName();
            e.printStackTrace();
        }
        
        return url;
    }

    /**
     * Generate a default URI for a String value.
     * 
     * @param string
     * @return
     */
    public String generateDefaultURI(String string)
    {
        try
        {
            DigestInputStream dis = new DigestInputStream(
                    new ByteArrayInputStream(string.getBytes()), MessageDigest
                            .getInstance("SHA"));
            byte[] buf = new byte[1028];
    
            while (dis.read(buf) >= 0)
            {
            }
    
            String digest = Utils.toHex(dis.getMessageDigest().digest());
    
            return "sha:" + digest;
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    
        return null;
    }

}