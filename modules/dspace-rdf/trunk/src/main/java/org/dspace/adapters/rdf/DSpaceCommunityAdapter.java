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
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceCommunityAdapter extends DSpaceObjectAdapter
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
    
    public void handle(DSpaceObject object) throws RDFHandlerException
    {
        handle((Community) object);
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
    
    public void handle(Community object) throws RDFHandlerException
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
                    RDF.TYPE, DS.Community));
            
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
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DC.identifier_, valueFactory.createLiteral("hdl:" + object.getHandle(), DCTERMS.URI)));


            // give it a title
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DC.title_, valueFactory.createLiteral(object.getName())));

            String shortDesc = object.getMetadata("short_description");
            if (shortDesc != null && !shortDesc.trim().equals(""))
            {
                shortDesc = cleanHTML(shortDesc);
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation, DCTERMS.abstract_, valueFactory
                                .createLiteral(shortDesc)));
            }

            String intro = object.getMetadata("introductory_text");
            if (intro != null && !intro.trim().equals("")){
                intro = cleanHTML(intro);
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation, DCTERMS.abstract_, valueFactory
                                .createLiteral(intro)));
            }
            
            
            /* =================================================================
             * List all the Communities or Site this community is a part of.
             * =================================================================
             */
            Community parent = object.getParentCommunity();
            
            if(parent != null)
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.isPartOf_, 
                        valueFactory.createURI(getMetadataURL(object.getParentCommunity()))));
            }
            else
            {
                Site site = (Site)Site.find(getContext(), 0);
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.isPartOf_, 
                        valueFactory.createURI(getMetadataURL(site))));
            }

            
            /* =================================================================
             * List all the Communities or Collections within this community.
             * =================================================================
             */
            for(Community community : object.getSubcommunities())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.hasPart_, 
                        valueFactory.createURI(getMetadataURL(community))));
            }

            for (Collection coll : object.getCollections())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.hasPart_, 
                        valueFactory.createURI(getMetadataURL(coll))));
            }
            
            /* =================================================================
             * Dig around for a Logo Image.
             * =================================================================
             */
            Bitstream logo = object.getLogo();
            if (logo != null)
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation, DS.logo, valueFactory
                                .createLiteral(getBitstreamURL(logo))));
            }

            rdfHandler.handleStatement(valueFactory.createStatement(
                    aggregation,
                    ORE.analogousTo, 
                    valueFactory.createURI(HandleManager.getCanonicalForm(object.getHandle()))));
            
            /* =================================================================
             * List all the Communities this collection is a part of.
             * =================================================================
             
            try
            {
                Community parent = object.getParentCommunity();
                
                if(parent != null)
                {
                    rdfHandler.handleStatement(valueFactory.createStatement(
                            aggregation,
                            ORE.isAggregatedBy, 
                            valueFactory.createURI(getMetadataURL(parent) + "#aggregation")));
                    
                }
                else
                {
                    Site site = (Site)Site.find(getContext(), 0);
                    rdfHandler.handleStatement(valueFactory.createStatement(
                            aggregation,
                            ORE.isAggregatedBy, 
                            valueFactory.createURI(getMetadataURL(site) + "#aggregation")));
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            
            
            for(Community community : object.getSubcommunities())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        ORE.aggregates, 
                        valueFactory.createURI(getMetadataURL(community) + "#aggregation")));
                
                getContext().removeCached(community, community.getID());
            }

            getContext().clearCache();

            for (Collection coll : object.getCollections())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        ORE.aggregates, 
                        valueFactory.createURI(getMetadataURL(coll) + "#aggregation")));
                
                getContext().removeCached(coll, coll.getID());
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
        Community community = (Community) object;
        
        try
        {
            for (Community comm : community.getSubcommunities())
            {
                getFactory().getAdapter(comm).handle(comm);
                getContext().removeCached(comm, comm.getID());
            }
            
            for (Collection coll : community.getCollections())
            {
                getFactory().getAdapter(coll).handle(coll);
                getContext().removeCached(coll, coll.getID());
            }
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
    }
}
