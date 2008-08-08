/*
 * Item2ItemRecommendationDAOPostgres.java
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

import org.dspace.app.recsys.recommendation.dao.Item2ItemRecommendationDAO;
import org.dspace.app.recsys.recommendation.Item2ItemRecommendation;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

import java.util.*;
import java.text.SimpleDateFormat;

/**
 * <code>Item2ItemRecommendationDAOPostgres</code> defines 
 * database-specific code for the manipulation of
 * <code>Item2ItemRecommendation</code>s.
 *
 * @author Desmond Elliott
 */
public class Item2ItemRecommendationDAOPostgres extends
        Item2ItemRecommendationDAO
{

    /**
     * Creates and returns a new <code>Item2ItemRecommendationDAOPostgres</code>
     * object which can be used be clients to interface with the database layer.
     *
     * @param context <code>Context</code> to be used by the
     *                <code>DatabaseManager</code>
     */
    public Item2ItemRecommendationDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public Item2ItemRecommendation create(int itemID) {
        try
        {
            TableRow row = DatabaseManager.create(context,
                    "quambo_item2item_recommendation");

            UUID uuid = UUID.randomUUID();
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("i2ir_id");
            Item2ItemRecommendation i2ir =
                    new Item2ItemRecommendation(id, context);

            i2ir.setUUID(uuid);
            i2ir.setItem1ID(itemID);

            row = retrieveStatus(itemID);
            
            if (row != null)
            {
                row.setColumn("last_updated", new Date().toString());
                DatabaseManager.update(context, row);
            }
            else
            {
                // Create the status table row and populate it
                row = DatabaseManager.create(context,
                                      "quambo_item2item_recommendation_status");

                row.setColumn("last_updated", new Date().toString());
                row.setColumn("item_id", itemID);
                DatabaseManager.update(context, row);
            }

            return i2ir;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public Item2ItemRecommendation retrieve(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "quambo_item2item_recommendation", id);

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public Item2ItemRecommendation retrieve(UUID uuid) {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2item_recommednation", "uuid", uuid);

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves an <code>Item2ItemRecommendation</code> object from the
     * database based on the <code>row</code> argument. If the the
     * <code>TableRow</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param row the <code>TableRow</code> of the
     *            <code>Item2ItemRecommendation</code> to retrieve from the
     *            database
     * @return an <code>Item2ItemRecommendation</code> object if it exists in
     *         the database, otherwise returns <code>null</code>.
     */
    private Item2ItemRecommendation retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }
        else
        {
            int id = row.getIntColumn("i2ir_id");
            Item2ItemRecommendation i2ir =
                    new Item2ItemRecommendation(id, context);

            i2ir.setItem1ID(row.getIntColumn("item1_id"));
            i2ir.setItem2ID(row.getIntColumn("item2_id"));
            i2ir.setSimilarity(row.getIntColumn("similarity"));
            i2ir.setUUID(UUID.fromString(row.getStringColumn("uuid")));

            row = retrieveStatus(i2ir.getItem1ID());

            i2ir.setLastUpdated(row.getStringColumn("last_updated"));

            return i2ir;
        }
    }

    /**
     * Gets the last updated status of the <code>Item</code>
     *
     * @param itemID the <code>Item</code> to determine the last udpated time of
     * @return the last updated status of the <code>Item</code>
     */
    private TableRow retrieveStatus(int itemID)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2item_recommendation_status", "item_id",
                    itemID);

            if (row == null)
            {
                return null;
            }

            return row;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public void update(Item2ItemRecommendation i2ir)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "quambo_item2item_recommendation", i2ir.getID());

            if (row == null)
            {
                throw new RuntimeException();
            }
            else
            {
                row.setColumn("item1_id", i2ir.getItem1ID());
                row.setColumn("item2_id", i2ir.getItem2ID());
                row.setColumn("similarity", i2ir.getSimilarity());
                
                updateStatus(i2ir);

                DatabaseManager.update(context, row);
                context.commit();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the last_updated value for the Item2ItemRecommendation object
     * passed to this method as an argument.
     *
     * @param i2ir the Item2ItemRecommendation which needs it's last_updated
     *             value to be updated.
     */
    private void updateStatus(Item2ItemRecommendation i2ir)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2item_recommendation_status",
                    "item_id", i2ir.getItem1ID());

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
    public void delete(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "quambo_item2item_recommendation", id);

            if (row == null)
            {
                throw new RuntimeException();
            }
            else
            {
                deleteStatusRow(row.getIntColumn("item1_id"));                            
                DatabaseManager.delete(context, row);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the table row which stores the last_updated data for the
     * Item2ItemRecommendation with ID id.
     *
     * @param id the ID of the Item2ItemRecommendation object
     */
    private void deleteStatusRow(int id)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_item2item_recommendation_status",
                    "item_id", id);

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
    public List<Item2ItemRecommendation> getRecommendations(int ID) {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT i2ir_id FROM " +
                    "quambo_item2item_recommendation WHERE item1_id = ?",
                    ID
            );

            return returnAsList(tri);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public boolean isRecommended(int item1ID, int item2ID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT i2ir_id FROM " +
                    "quambo_item2item_recommendation WHERE item1_id = ? AND " +
                    "item2_id = ?", item1ID, item2ID);

            return tri.hasNext();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
   public boolean needsRecalculation(int itemID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                "SELECT last_updated from " +
                "quambo_item2item_recommendation_status" +
                " WHERE item_id = ?", itemID);

            // There are no entries in the database so we need to calculate
            // recommendations for this item
            if (!tri.hasNext())
            {
                return true;
            }

            Date d =
              stringToDate(tri.toList().get(0).getStringColumn("last_updated"));

            Date now = new Date();

            int recalculationDelta =
                Integer.parseInt(ConfigurationManager.getProperty("quambo." +
                                                 "recalculation-days-delta"));

            recalculationDelta = recalculationDelta * 60 * 1000;

            // Given the current time in ms and the time of the oldest
            // recommendation stored in ms, if the difference is > n days,
            // return true

            return now.getTime() - d.getTime() > recalculationDelta;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Converts a <code>TableRowIterator</code> of
     * <code>Item2ItemRecommandation</code>s into a <code>List</code> of
     * <code>Item2ItemRecommandation</code>s.
     *
     * @param tri a <code>TableRowIterator</code>, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a sorted <code>List</code> of
     *         <code>Item2ItemRecommandation</code>s
     */
    private List<Item2ItemRecommendation> returnAsList(TableRowIterator tri)
    {
        List<Item2ItemRecommendation> list =
                new ArrayList<Item2ItemRecommendation>();

        try
        {
            for (TableRow row: tri.toList())
            {
                int id = row.getIntColumn("i2ir_id");
                list.add(retrieve(id));
            }
            return list;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
