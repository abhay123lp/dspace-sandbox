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
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceCollectionAdapter extends DSpaceObjectAdapter
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
    
    public void handle(DSpaceObject collection) throws RDFHandlerException 
    {
        handle((Collection) collection);
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
    
    public void handle(Collection object) throws RDFHandlerException 
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
                    RDF.TYPE, DS.Collection));
            
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

            // describe type as resource map
            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DC.creator_, valueFactory.createLiteral(ConfigurationManager.getProperty("dspace.name"))));
            
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
            
            /* =================================================================
             * Time lastModified TODO: We need real timestamps for changes in db...
             * =================================================================
             */
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

            rdfHandler.handleStatement(valueFactory.createStatement(aggregation,
                    DCTERMS.modified_, valueFactory.createLiteral(xmlCal)));
            
            /* =================================================================
             * List all the Communities this collection is a part of.
             * =================================================================
             */
            for(Community community : object.getCommunities())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        DCTERMS.isPartOf_, 
                        valueFactory.createURI(getMetadataURL(community))));
                
            }
            
            /* =================================================================
             * List all the Communities this collection is a part of.
             * =================================================================
             */
            for(Community community : object.getCommunities())
            {
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        ORE.isAggregatedBy, 
                        valueFactory.createURI(getMetadataURL(community))));
                
                getContext().removeCached(community, community.getID());
            }
            
            
            /*
             * show all the communities this collection is a member of
             */
            for(ItemIterator iter = object.getAllItems(); iter.hasNext();)
            {
                Item item = iter.next();
                rdfHandler.handleStatement(valueFactory.createStatement(
                        aggregation,
                        ORE.aggregates, 
                        valueFactory.createURI(getMetadataURL(item))));

                item.decache();
            }   
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
    
    
    @SuppressWarnings("unchecked")
    public void handleChildren(DSpaceObject object) throws RDFHandlerException
    {
        Collection collection = (Collection)object;
        
        try
        {
            String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
            BrowserScope scope = new BrowserScope(getContext());
            scope.setCollection(collection);
            
            scope.setResultsPerPage(100);
            scope.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
            scope.setOrder(SortOption.DESCENDING);
            
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    scope.setSortBy(so.getNumber());
                    scope.setOrder(SortOption.DESCENDING);
                }
            }

            BrowseEngine be = new BrowseEngine(getContext());
                
            java.util.List<Item> items = be.browse(scope).getResults();

            for (Item item : items)
            {
                //if(item.getOwningCollection().equals(collection))
                    getFactory().getAdapter(item).handle(item);
                    
                item.decache();
            }
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
        catch (BrowseException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
        catch (SortException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }

    }

}
