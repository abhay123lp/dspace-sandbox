/*
 * Harvest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.search;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.content.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Utility class for extracting information about items, possibly just within a
 * certain community or collection, that have been created, modified or
 * withdrawn within a particular range of dates.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Harvest
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Harvest.class);
    
    /**
     * Obtain information about items that have been created, modified or
     * withdrawn within a given date range. You can also specify 'offset' and
     * 'limit' so that a big harvest can be split up into smaller sections.
     * <P>
     * Note that dates are passed in the standard ISO8601 format used by DSpace
     * (and OAI-PMH).
     * <P>
     * FIXME: Assumes all in_archive items have public metadata
     * 
     * @param context
     *            DSpace context
     * @param scope
     *            a Collection, Community, or <code>null</code> indicating the scope is
     *            all of DSpace
     * @param startDate
     *            start of date range, or <code>null</code>
     * @param endDate
     *            end of date range, or <code>null</code>
     * @param offset
     *            for a partial harvest, the point in the overall list of
     *            matching items to start at. 0 means just start at the
     *            beginning.
     * @param limit
     *            the number of matching items to return in a partial harvest.
     *            Specify 0 to return the whole list (or the rest of the list if
     *            an offset was specified.)
     * @param items
     *            if <code>true</code> the <code>item</code> field of each
     *            <code>HarvestedItemInfo</code> object is filled out
     * @param collections
     *            if <code>true</code> the <code>collectionIdentifiers</code>
     *            field of each <code>HarvestedItemInfo</code> object is
     *            filled out
     * @param withdrawn
     *            If <code>true</code>, information about withdrawn items is
     *            included
     * @return List of <code>HarvestedItemInfo</code> objects
     * @throws SQLException
     * @throws ParseException If the date is not in a supported format
     */
    public static List harvest(Context context, DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn)
            throws SQLException, ParseException
    {
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        ExternalIdentifierDAO identifierDAO =
            ExternalIdentifierDAOFactory.getInstance(context);
        ExternalIdentifier identifier = null;

        // Put together our query. Note there is no need for an
        // "in_archive=true" condition, we are using the existence of a
        // persistent identifier as our 'existence criterion'.
        String query =
            "SELECT p.value, p.type_id, p.resource_id, " +
            "i.withdrawn, i.last_modified " +
            "FROM externalidentifier p, item i";

        // We are building a complex query that may contain a variable 
        // about of input data points. To accomidate this while still 
        // providing type safty we build a list of parameters to be 
        // plugged into the query at the database level.
        List parameters = new ArrayList();
        
        if (scope != null)
        {
        	if (scope.getType() == Constants.COLLECTION)
        	{
        		query += ", collection2item cl2i";
        	}
        	else if (scope.getType() == Constants.COMMUNITY)
        	{
        		query += ", community2item cm2i";
        	}
        }       

        query += " WHERE p.resource_type_id=" + Constants.ITEM +
            " AND p.resource_id = i.item_id ";

        if (scope != null)
        {
        	if (scope.getType() == Constants.COLLECTION)
        	{
        		query += " AND cl2i.collection_id= ? " +
        	             " AND cl2i.item_id = p.resource_id ";
        		parameters.add(new Integer(scope.getID()));
        	}
        	else if (scope.getType() == Constants.COMMUNITY)
        	{
        		query += " AND cm2i.community_id= ? " +
						 " AND cm2i.item_id = p.resource_id";
        		parameters.add(new Integer(scope.getID()));
        	}
        }      
                
        if (startDate != null)
        {
        	query = query + " AND i.last_modified >= ? ";
        	parameters.add(toTimestamp(startDate, false));
        }

        if (endDate != null)
        {
            /*
             * If the end date has seconds precision, e.g.:
             * 
             * 2004-04-29T13:45:43Z
             * 
             * we need to add 999 milliseconds to this. This is because SQL
             * TIMESTAMPs have millisecond precision, and so might have a value:
             * 
             * 2004-04-29T13:45:43.952Z
             * 
             * and so <= '2004-04-29T13:45:43Z' would not pick this up. Reading
             * things out of the database, TIMESTAMPs are rounded down, so the
             * above value would be read as '2004-04-29T13:45:43Z', and
             * therefore a caller would expect <= '2004-04-29T13:45:43Z' to
             * include that value.
             * 
             * Got that? ;-)
             */
        	boolean selfGenerated = false;
            if (endDate.length() == 20)
            {
                endDate = endDate.substring(0, 19) + ".999Z";
                selfGenerated = true;
            }

        	query += " AND i.last_modified <= ? ";
            parameters.add(toTimestamp(endDate, selfGenerated));
        }
        
        if (withdrawn == false)
        {
            // Exclude withdrawn items
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                query += " AND withdrawn=0 ";
            }
            else
            {
                // postgres uses booleans
                query += " AND withdrawn=false ";
            }
        }

        // Order by item ID, so that for a given harvest the order will be
        // consistent. This is so that big harvests can be broken up into
        // several smaller operations (e.g. for OAI resumption tokens.)
        query += " ORDER BY p.resource_id";

        log.debug(LogManager.getHeader(context, "harvest SQL", query));
        
        // Execute
        Object[] parametersArray = parameters.toArray();
        TableRowIterator tri = DatabaseManager.query(context, query, parametersArray);
        List infoObjects = new LinkedList();
        int index = 0;

        // Process results of query into HarvestedItemInfo objects
        while (tri.hasNext())
        {
            TableRow row = tri.next();

            /*
             * This conditional ensures that we only process items within any
             * constraints specified by 'offset' and 'limit' parameters.
             */
            if ((index >= offset)
                    && ((limit == 0) || (index < (offset + limit))))
            {
                HarvestedItemInfo itemInfo = new HarvestedItemInfo();
                
                String value = row.getStringColumn("value");
                int typeID = row.getIntColumn("type_id");

                ExternalIdentifier.Type type = null;

                for (ExternalIdentifier.Type t : ExternalIdentifier.Type.values())
                {
                    if (t.getID() == typeID)
                    {
                        type = t;
                        break;
                    }
                }

                if (type == null)
                {
                    throw new RuntimeException(value + " not supported.");
                }

                identifier = identifierDAO.retrieve(type, value);

                itemInfo.context = context;
                itemInfo.identifier = identifier;
                itemInfo.itemID = row.getIntColumn("resource_id");
                itemInfo.datestamp = row.getDateColumn("last_modified");
                itemInfo.withdrawn = row.getBooleanColumn("withdrawn");

                if (collections)
                {
                    fillCollections(context, itemInfo);
                }

                if (items)
                {
                    itemInfo.item = itemDAO.retrieve(itemInfo.itemID);
                }

                infoObjects.add(itemInfo);
            }

            index++;
        }
        tri.close();

        return infoObjects;
    }

    /**
     * Get harvested item info for a single item. <code>item</code> field in
     * returned <code>HarvestedItemInfo</code> object is always filled out.
     * 
     * @param context
     *            DSpace context
     * @param identifier
     *            A persistent identifier.
     * @param collections
     *            if <code>true</code> the <code>collectionIdentifiers</code>
     *            field of the <code>HarvestedItemInfo</code> object is filled
     *            out
     * 
     * @return <code>HarvestedItemInfo</code> object for the single item, or
     *         <code>null</code>
     * @throws SQLException
     */
    public static HarvestedItemInfo getSingle(Context context,
            ExternalIdentifier identifier, boolean collections)
    {
        // FIXME: Assume identifier is item
        // FIXME: We should be passing an ObjectIdentifier in here, not a
        // ExternalIdentifier
        ObjectIdentifier oi = identifier.getObjectIdentifier();
        Item i = (Item) oi.getObject(context);

        if (i == null)
        {
            return null;
        }

        // Fill out OAI info item object
        HarvestedItemInfo itemInfo = new HarvestedItemInfo();

        itemInfo.context = context;
        itemInfo.item = i;
        itemInfo.identifier = identifier;
        itemInfo.withdrawn = i.isWithdrawn();
        itemInfo.datestamp = i.getLastModified();
        itemInfo.itemID = i.getID();

        // Get the sets
        if (collections)
        {
            fillCollections(context, itemInfo);
        }

        return itemInfo;
    }

    /**
     * Fill out the containers field of the HarvestedItemInfo object
     * 
     * @param context
     *            DSpace context
     * @param itemInfo
     *            HarvestedItemInfo object to fill out
     */
    private static void fillCollections(Context context,
            HarvestedItemInfo itemInfo)
    {
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);

        ObjectIdentifier oi = new ObjectIdentifier(itemInfo.itemID, Constants.ITEM);
        Item item = (Item) oi.getObject(context);

        List<Collection> parents = collectionDAO.getParentCollections(item);

        List<ExternalIdentifier> identifiers =
            new LinkedList<ExternalIdentifier>();

        for (Collection parent : parents)
        {
            identifiers.add(parent.getExternalIdentifier());
        }

        itemInfo.collectionIdentifiers = identifiers;
    }

    
    /**
     * Convert a String to a java.sql.Timestamp object
     * 
     * @param t The timestamp String
     * @param selfGenerated Is this a self generated timestamp (e.g. it has .999 on the end)
     * @return The converted Timestamp
     * @throws ParseException 
     */
    private static Timestamp toTimestamp(String t, boolean selfGenerated) throws ParseException
    {
        SimpleDateFormat df;
        
        // Choose the correct date format based on string length
        if (t.length() == 10)
        {
            df = new SimpleDateFormat("yyyy-MM-dd");
        }
        else if (t.length() == 20)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
        else if (selfGenerated)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        else {
            // Not self generated, and not in a guessable format
            throw new ParseException("", 0);
        }
        
        // Parse the date
        df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return new Timestamp(df.parse(t).getTime());
    }    
}
