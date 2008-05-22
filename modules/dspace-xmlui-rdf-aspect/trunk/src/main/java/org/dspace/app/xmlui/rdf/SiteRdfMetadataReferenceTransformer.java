package org.dspace.app.xmlui.rdf;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.xml.sax.SAXException;

public class SiteRdfMetadataReferenceTransformer extends AbstractDSpaceTransformer
        implements CacheableProcessingComponent
{

    /** Cached validity object */
    private SourceValidity validity;
   
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = Site.find(context, 0);
            return HashUtil.hash(dso.getHandle());
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * The validity object will include the item being viewed, 
     * along with all bundles & bitstreams.
     */
    public SourceValidity getValidity() 
    {
        if (this.validity == null)
        {
            try {
                DSpaceObject dso = Site.find(context, 0);
                DSpaceValidity validity = new DSpaceValidity();
                validity.add(dso);
                this.validity =  validity.complete();
            } 
            catch (Exception e)
            {
                // Ignore all errors and just invalidate the cache.
            }
        }
        return this.validity;
    }

    
    /**
     * Add the item's title and trail links to the page's metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("rdf", "meta").addContent(contextPath+"/metadata/handle/"+ Site.getSiteHandle() + "/rdf.xml");
        
    }
    
    /**
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");
        
        List alt = options.addList("alternatives");

        alt.setHead("Test Logos");

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        
        alt
            .addItem("RDF for this source.", null)
            .addFigure("http://www.w3.org/RDF/icons/rdf_metadata_button.32", contextPath+"/rdf/" + dso.getHandle(), "RDF Resource Description Framework Metadata Icon")
            .addContent("This is the alt text");
        
    }
    */
}
