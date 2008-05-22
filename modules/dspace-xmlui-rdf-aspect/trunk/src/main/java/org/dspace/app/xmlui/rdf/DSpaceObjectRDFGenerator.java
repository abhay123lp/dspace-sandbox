package org.dspace.app.xmlui.rdf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.adapters.rdf.DSpaceXMLReader;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.openrdf.rio.RDFHandlerException;
import org.xml.sax.SAXException;


public class DSpaceObjectRDFGenerator extends AbstractGenerator
{

    /** The objects identifier */
    private String handle = null;
    
    /**
     * Setup configuration for this request
     */
    @SuppressWarnings("unchecked")
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);
        this.handle = par.getParameter("handle",null);
    }
    
    public void generate() throws IOException, SAXException,
            ProcessingException
    {
        try {
            Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = null;
            
            if(handle == null || Site.getSiteHandle().equals(handle))
            {
                dso = Site.find(context, 0);
            }
            else
            {
                dso = HandleManager.resolveToObject(context, handle);
            }
            
            if (dso == null)
            {
                // If we were unable to find a handle then return page not found.
                throw new ResourceNotFoundException("Unable to find DSpace object matching the given handle: "+handle);
            }

            Request request = ObjectModelHelper.getRequest(objectModel);
            
            String baseUri = request.getScheme() + "://";
            
            baseUri += request.getServerName();
            
            int port = request.getServerPort();
            
            if(port != 80)
            {
                baseUri += ":" + port;
            }
            
            baseUri += request.getContextPath();

            
            DSpaceXMLReader parser = new DSpaceXMLReader();
            parser.setContext(context);
            parser.setContentHandler(contentHandler);
            parser.parse(dso);

        }
        catch (RDFHandlerException e)
        {
            throw new SAXException(e.getMessage(),e);
        }
        catch (SQLException e)
        {
            throw new SAXException(e.getMessage(),e);
        }
    }

}
