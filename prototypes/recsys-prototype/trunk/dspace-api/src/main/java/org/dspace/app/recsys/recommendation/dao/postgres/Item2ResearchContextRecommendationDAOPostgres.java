/*
 * Item2ResearchContextRecommendationDAOPostgres.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.recsys.recommendation.dao.postgres;

import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAO;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * <code>Item2ResearchContextRecommendationDAOPostgres</code> defines
 * database-specific code for the manipulation of
 * <code>Item2ResearchContextRecommendation</code>s.
 *
 * @author Desmond Elliott
 */
public class Item2ResearchContextRecommendationDAOPostgres extends
        Item2ResearchContextRecommendationDAO
{

    /**
     * Creates and returns a new
     * <code>Item2ResearchContextRecommendationDAOPostgres</code>
     * object which can be used be clients to interface with the database layer.
     *
     * @param context <code>Context</code> to be used by the
     *                <code>DatabaseManager</code>
     */
    public Item2ResearchContextRecommendationDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public Item2ResearchContextRecommendation create()
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context,
                    "quambo_item2research_context_recommendation");

            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("i2rcr_id");

            // Create the status table row and populate it
            row = DatabaseManager.create(context,
                          "quambo_item2research_context_recommendation_status");

            row.setColumn("last_updated", new Date().toString());
            row.setColumn("i2rcr_id", id);
            DatabaseManager.update(context, row);

            return new Item2ResearchContextRecommendation(id, context);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public Item2ResearchContextRecommendation retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "quambo_item2research_context_recommendation", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public Item2ResearchContextRecommendation retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2research_context_recommendation",
                    "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Retrieves an <code>Item2ResearchContextRecommendation</code> object from
     * the database based on the <code>row</code> argument. If the the
     * <code>TableRow</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param row the <code>TableRow</code> of the
     *            <code>Item2ResearchContextRecommendation</code> to retrieve
     *            from the database
     * @return an <code>Item2ResearchContextRecommendation</code> object if it
     *         exists in the database, otherwise returns <code>null</code>.
     */
    private Item2ResearchContextRecommendation retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }
        else
        {
            int id = row.getIntColumn("i2rcr_id");
            Item2ResearchContextRecommendation i2rcr =
                    new Item2ResearchContextRecommendation(id, context);

            i2rcr.setItemID(row.getIntColumn("item_id"));
            i2rcr.setRelevance(row.getIntColumn("relevance"));
            i2rcr.setResearchContextID(row.getIntColumn("research_context_id"));

            try
            {
                TableRow status = DatabaseManager.findByUnique(context,
                    "quambo_item2research_context_recommendation_status",
                    "i2rcr_id", i2rcr.getID());

                i2rcr.setLastUpdated(status.getStringColumn("last_updated"));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            return i2rcr;
        }
    }

    /** @inheritDoc */
    public void update(Item2ResearchContextRecommendation i2rcr)
    {
        try
        {
            int id = i2rcr.getID();
            TableRow row = DatabaseManager.find(context,
                    "quambo_item2research_context_recommendation", id);

            if (row != null)
            {
                row.setColumn("item_id", i2rcr.getItemID());
                row.setColumn("relevance", i2rcr.getRelevance());
                row.setColumn("research_context_id",
                              i2rcr.getResearchContextID());

                DatabaseManager.update(context, row);                
                context.commit();

                updateStatus(i2rcr);
            }
            else
            {
                throw new RuntimeException("Didn't find recommendation " + id);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Updates the last_updated value for the
     * <code>Item2ResearchContextRecommendation</code> object
     * passed to this method as an argument.
     *
     * @param i2rcr the <code>Item2ResearchContextRecommendation</code> which
     *              needs it's last_updated value to be updated.
     */
    private void updateStatus(Item2ResearchContextRecommendation i2rcr)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2research_context_recommendation_status",
                    "i2rcr_id", i2rcr.getID());

            if (row == null)
            {
                throw new RuntimeException();
            }
            else
            {
                row.setColumn("last_updated", new Date().toString());

                DatabaseManager.update(context, row);
                context.commit();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    

    /** @inheritDoc */
    public void delete(int id)
    {
        deleteStatusRow(id);
        
        try
        {
            DatabaseManager.delete(context,
                    "quambo_item2research_context_recommendation", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Deletes the table row which stores the last_updated data for the
     * <code>Item2ResearchContextRecommendation</code> with ID id.
     *
     * @param id the ID of the <code>Item2ResearchContextRecommendation</code> 
     */
    private void deleteStatusRow(int id)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2research_context_recommendation_status",
                    "i2rcr_id", id);

            if (row == null)
            {
                throw new RuntimeException();
            }
            else
            {
                DatabaseManager.delete(context, row);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public boolean isRecommended(int itemID, ResearchContext researchContext)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT i2rcr_id FROM " +
                    "quambo_item2research_context_recommendation " +
                    "WHERE item_id = ? AND research_context_id = ?",
                    itemID, researchContext.getID());

            boolean result = tri.hasNext();
            tri.close();

            return result;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public List<Item2ResearchContextRecommendation>
        getRecommendations(int researchContextID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT i2rcr_id FROM " +
                    "quambo_item2research_context_recommendation " +
                    "WHERE research_context_id = ? ORDER BY relevance DESC",
                    researchContextID);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public boolean needsRecalculation(ResearchContext r)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                "SELECT last_updated from " +
                "quambo_item2research_context_recommendation_status" +
                " WHERE i2rcr_id = " +
                "(SELECT i2rcr_id FROM" +
                " quambo_item2research_context_recommendation " +
                "WHERE research_context_id = ? LIMIT 1)",
                r.getID());

            List<Date> dates = new ArrayList<Date>();

            // There are no entries in the database so we need to calculate
            // recommendations for this item
            if (!tri.hasNext())
            {
                return true;
            }

            for (TableRow row: tri.toList())
            {
                dates.add(stringToDate(row.getStringColumn("last_updated")));
            }

            // For all last_updated dates in the database,
            // sort them in ascending order
            Date[] date = dates.toArray(new Date[dates.size()]);
            Arrays.sort(date);

            Date now = new Date();
            Date oldest = date[0];

            int recalculationDelta =
                Integer.parseInt(ConfigurationManager.getProperty("quambo." +
                                                 "recalculation-days-delta"));

            recalculationDelta = recalculationDelta * 60 * 1000;

            // Given the current time in ms and the time of the oldest
            // recommendation stored in ms, if the difference is > n days,
            // return true

            return now.getTime() - oldest.getTime() > recalculationDelta;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a <code>TableRowIterator</code> of
     * <code>Item2ResearchContextRecommandation</code>s into a <code>List</code>
     * of <code>Item2ResearchContextRecommandation</code>s.
     *
     * @param tri a <code>TableRowIterator</code>, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a sorted <code>List</code> of
     *         <code>Item2ResearchContextRecommandation</code>s
     */
    private List<Item2ResearchContextRecommendation>
        returnAsList(TableRowIterator tri)
    {
        List<Item2ResearchContextRecommendation>
                item2ResearchContextRecommendations =
                new ArrayList<Item2ResearchContextRecommendation>();

        try
        {
            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("i2rcr_id");
                item2ResearchContextRecommendations.add(retrieve(id));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return item2ResearchContextRecommendations;
    }

    /**
     * Converts a String into a Date object of format:
     * <code>EEE MMM d HH:mm:ss z yyyy</code>.
     *
     * @param date <code>String</code> to convert to a <code>Date</code>
     * @return a Date in the format defined above
     */
    private Date stringToDate(String date)
    {
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");
        try
        {
            return sdf.parse(date);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }    
}
