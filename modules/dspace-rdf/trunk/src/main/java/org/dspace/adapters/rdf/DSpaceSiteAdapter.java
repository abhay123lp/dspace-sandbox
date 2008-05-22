package org.dspace.adapters.rdf;

import java.sql.SQLException;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.dspace.adapters.rdf.vocabularies.DC;
import org.dspace.adapters.rdf.vocabularies.DCMITYPE;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.adapters.rdf.vocabularies.DS;
import org.dspace.adapters.rdf.vocabularies.ORE;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceSiteAdapter extends DSpaceObjectAdapter
{

    public void handleNamespaces() throws RDFHandlerException
    {
        RDFHandler rdfHandler = getRDFHander();
        rdfHandler.handleNamespace("rdf", RDF.NAMESPACE);
        rdfHandler.handleNamespace("dc", DC.NAMESPACE);
        rdfHandler.handleNamespace("dcterms", DCTERMS.NAMESPACE);
        rdfHandler.handleNamespace("ds", DS.NAMESPACE);
        rdfHandler.handleNamespace("ore", ORE.NAMESPACE);
    }
    
    @Override
    public void handle(DSpaceObject object) throws RDFHandlerException
    {
        handle((Site) object);
    }
    
    private void handleResourceMap(URI uri, DSpaceObject object, URI aggregation) throws RDFHandlerException, DatatypeConfigurationException, SQLException
        {
            RDFHandler rdfHandler = getRDFHander();

            // describe type as resource map
            rdfHandler.handleStatement(valueFactory.createStatement(
                    uri, RDF.TYPE, ORE.ResourceMap));
               
            GregorianCalendar cal = new GregorianCalendar();

            XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            
            /** 
             * kinda hacky, but gets around strange bug when Gregcal is
             * year, month, day is set by hand.
             */
            xmlCal.setFractionalSecond(null);
            xmlCal.setHour(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

            rdfHandler.handleStatement(valueFactory.createStatement(uri,
                    DCTERMS.modified_, valueFactory.createLiteral(xmlCal)));
            
            // describe type as resource map
            rdfHandler.handleStatement(valueFactory.createStatement(uri,
                    DC.creator_, valueFactory.createLiteral(ConfigurationManager.getProperty("dspace.name"))));
            
            // identify aggregation
            rdfHandler.handleStatement(valueFactory.createStatement(
                    uri, ORE.describes, aggregation));
    }
    
    public void handle(Site object) throws RDFHandlerException
    {
        try
        {
            RDFHandler rdfHandler = getRDFHander();

            URI rem = valueFactory.createURI(getMetadataURL(object) + "#rem");

            URI aggregation = valueFactory.createURI(getMetadataURL(object));

            this.handleResourceMap(rem, object, aggregation);    
              
            /* =================================================================
             * The following statements are typing for DCMI Collections
             * =================================================================
             */
            
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    RDF.TYPE, DS.Site));
            
            // describe type as collection (seems appropriate for RDF)
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    RDF.TYPE, DCMITYPE.Collection));

            // describe type as collection (specification says this is manditory)
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DC.type_, DCMITYPE.Collection));
            
            // make statements about it
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    RDF.TYPE, ORE.Aggregation));
            
            /* =================================================================
             * The following is a dc.identifier, title, creator and abstract for DCMI Collections 
             * =================================================================
             */
            // This isn't very useful because it isn't resolvable...
            //rdfHandler.handleStatement(valueFactory.createStatement(thisDSObject,
            //        DC.identifier_, valueFactory.createURI("hdl:" + object.getHandle())));

            // give it a title
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DC.title_, valueFactory.createLiteral(object.getName())));

            
            /* =================================================================
             * List all the Communities or Collections within this community.
             * =================================================================
             */
            for(Community community : Community.findAllTop(getContext()))
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.hasPart_, 
                        valueFactory.createURI(getMetadataURL(community))));
            }
            
            /*
            for(Community community : Community.findAllTop(getContext()))
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        ORE.aggregates, 
                        valueFactory.createURI(getMetadataURL(community) + "#aggregation")));
                
                getContext().removeCached(community, community.getID());
            }
            */
            
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
        catch (DatatypeConfigurationException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }

    }

    public void handleChildren(DSpaceObject object) throws RDFHandlerException 
    {
        try
        {
            for (Community comm : Community.findAll(getContext()))
            {
                getFactory().getAdapter(comm).handle(comm);
                getContext().removeCached(comm, comm.getID());
            }
           
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
    }
    
    


}
