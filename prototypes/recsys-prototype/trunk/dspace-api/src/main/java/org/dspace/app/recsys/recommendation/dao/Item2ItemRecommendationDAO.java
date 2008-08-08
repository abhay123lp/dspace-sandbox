/*
 * Item2ItemRecommendationDAO.java
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
package org.dspace.app.recsys.recommendation.dao;

import org.dspace.app.recsys.recommendation.Item2ItemRecommendation;
import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * <code>Item2ItemRecommendationDAO</code> handles database access to
 * create, retrieve, update, and delete <code>Item2ItemRecommendation</code>s.
 * Any non database-specific code must be declared in this class instead of
 * <code>Item2ItemRecommendationDAOPostgres</code>.
 *
 * @author Desmond Elliott
 */
public abstract class Item2ItemRecommendationDAO {

    /** Logs data to <code>dspace.log</code> */
    protected Logger log = Logger.getLogger(Item2ItemRecommendationDAO.class);

    /** <code>DSpace Context</code> used for database access */
    protected Context context;

    /**
     * Creates and returns a new <code>Item2ItemRecommendation</code> object by
     * creating a new table row.
     *
     * @param itemID Used to create or update the last updated status for the
     *               item
     *
     * @return A new <code>Item2ItemRecommendation</code> object with
     *         <code>ID</code> equal to the <code>ID</code> of the database
     *         table row created.
     */
    public abstract Item2ItemRecommendation create(int itemID);

    /**
     * Retrieves an <code>Item2ItemRecommendation</code> object from the
     * database based on the argument <code>id</code>. If the the
     * <code>ID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param id the <code>ID</code> of the <code>Item2ItemRecommendation</code>
     *           to retrieve from the database
     * @return a <code>Item2ItemRecommendation</code> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract Item2ItemRecommendation retrieve(int id);

    /**
     * Retrieves an <code>Item2ItemRecommendation</code> object from the
     * database based on the argument <code>uuid</code>. If the the
     * <code>UUID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param uuid the <code>UUID</code> of the
     *             <code>Item2ItemRecommendation</code> to retrieve from the
     *             database
     * @return a <code>Item2ItemRecommendation</code> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract Item2ItemRecommendation retrieve(UUID uuid);

    /**
     * Updates the database row that represents the
     * <code>Item2ItemRecommendation</code> object based on the <code>ID</code>
     * of the argument object. If the the <code>ID</code> does not exist in the
     * database, or any of the fields in the argument object are null,
     * this method will not succeed.
     *
     * @param i2ir the <code>Item2ItemRecommendation</code> to write to the
     *             database
     */
    public abstract void update(Item2ItemRecommendation i2ir);

    /**
     * Deletes the database row of the <code>Item2ItemRecommendation</code>
     * object represented by the <code>id</code> argument. If a database row in
     * the <code>quambo_item2item_recommendation</code> table does not exist
     * with the argument <code>id</code>, an Exception is thrown.
     *
     * @param id the <code>ID</code> of the
     *           <code>Item2ItemRecommendation</code> object
     */
    public abstract void delete(int id);

    /**
     * Gets all <code>Item2ItemRecommendation</code> objects stored for the
     * <code>Item</code> argument.
     *
     * @param ID <code>Item</code> to get
     *             <code>Item2ItemRecommendation</code>(s) for
     *
     * @return A <code>List</code> of the
     *         <code>Item2ItemRecommendation</code>(s) stored in the database
     *         which have <code>item</code> as the <code>viewed Item</code>.
     */
    public abstract List<Item2ItemRecommendation> getRecommendations(int ID);

    /**
     * Determines the item-to-item recommendations for the arugment
     * <code>Item</code>. Initially evaluates each existing
     * <code>Item2ItemRecommendation</code> and determines
     * if it is still above the minimum similarity threshold for
     * recommendations. Proceeds to then evaluate every <code>Item</code>
     * in the repository and creates new <code>Item2ItemRecommendation</code>
     * objects as necessary.
     *
     * <p>
     * Does nothing if recommendations have been calculated in the previous
     * n hours, definable in the quambo.recalculation-delta variable in
     * dspace.cfg. This is an attempt to prevent hammering the server.
     * </p>
     *
     * @param item the <code>Item</code> to calculate recommendations for
     */
    public void calculateRecommendations(Item item)
    {
        log.info(LogManager.getHeader(context, "calculateRecommendations",
                                      "start"));

        Hashtable<Integer, List<String>> h = getAllMetadata();

        String definedThreshold =
                ConfigurationManager.getProperty("quambo.similarity-threshold")
                                    .trim();

        float threshold = Float.parseFloat(definedThreshold);

        if (needsRecalculation(item.getID()))
        {
            List<StoredRecommendation> stored = new ArrayList<StoredRecommendation>();
            updateRecommendations(item.getID(), h, threshold);
            determineNewRecommendations(item.getID(), h, threshold, stored);
            writeRecommendations(stored);
        }

        log.info(LogManager.getHeader(context, "calculateRecommendations",
                                      "stop"));
    }

    /**
     * Determines the item-to-item recommendations for every <code>Item</code>
     * stored in the repository. This method retrieves all <code>Item</code>
     * metadata before performing calculations to reduce repetitive
     * database access.
     */
    public void calculateAllRecommendations()
    {
        log.info(LogManager.getHeader(context, "calculateAllRecommendations",
                                      "start"));

        Hashtable<Integer, List<String>> metadata = getAllMetadata();

        String definedThreshold =
                ConfigurationManager.getProperty("quambo.similarity-threshold")
                                    .trim();

        float threshold = Float.parseFloat(definedThreshold);

        Set<Integer> itemIDs = metadata.keySet();
        List<StoredRecommendation> stored = new ArrayList<StoredRecommendation>();

        log.info(LogManager.getHeader(context, "determineRecommendations",
                                      "start"));

        for(Integer itemID: itemIDs)
        {
            updateRecommendations(itemID, metadata, threshold);
            determineNewRecommendations(itemID, metadata, threshold, stored);
        }
        log.info(LogManager.getHeader(context, "determineNewRecommendations",
                                      "stop"));           

        writeRecommendations(stored);        

        log.info(LogManager.getHeader(context, "calculateAllRecommendations",
                                      "stop"));
    }

   /**
     * Updates existing <code>Item2ItemRecommendations</code> for
     * <code>item</code>. Deletes existing <code>Item2ItemRecommendations</code>
     * which are no longer applicable.
     *
     * @param ID <code>Item</code> to determine item-to-item recommendations
     *             for against the entire collection of <code>Item</code>s in
     *             the repository.
     *
     * @param h Hashtable storing the list of terms for every Item
    *  @param threshold minimum similarity threshold for recommendations
     */
    private void updateRecommendations(int ID,
                                       Hashtable<Integer, List<String>> h,
                                       float threshold)
    {
        List<Item2ItemRecommendation> recommendations = getRecommendations(ID);

        String alg =
                ConfigurationManager.getProperty("quambo.similarity-algorithm")
                                    .trim();

        for (Item2ItemRecommendation i2ir: recommendations)
        {
            float similarity;

            if (alg.equals("cosine"))
            {
                similarity = getCosineSimilarity(h.get(ID),
                                h.get(i2ir.getItem2ID()));
            }
            else if (alg.equals("jaccard"))
            {
                similarity = getJaccardSimilarity(h.get(ID),
                                           h.get(i2ir.getItem2ID()));
            }
            else
            {
                throw new RuntimeException("quambo.similarity-" +
                        "algorithm incorrectly defined");
            }

            // Similarity between these items has changed
            if (!(Math.abs(similarity - i2ir.similarity) < 0.000001))
            {
                if (threshold - similarity > 0.00001)
                {
                    delete(i2ir.getID());
                    continue;
                }

                i2ir.setSimilarity((int)Math.ceil(similarity * 5));
            }

            i2ir.setLastUpdated(new Date());

            update(i2ir);
        }
    }

    /**
     * Calculates item-to-item recommendations for the <code>Item</code> passed
     * as an argument. Only creates new <code>Item2ItemRecommendations</code> if
     * the <code>similarity</code> of the argument <code>Item</code> and another
     * <code>Item</code> is greater than the <code>minimum similarity
     * threshold.
     *
     * @param itemID <code>Item</code> to determine item-to-item recommendations
     *             for against the entire collection of <code>Item</code>s in
     *             the repository.
     *
     * @param metadata Hashtable storing the list of terms for every Item
     * @param threshold minimum similarity threshold for recommendations
     * @param stored List of tuples holding Item IDs and similarities to be
     *               written later
     */
    private void determineNewRecommendations(int itemID,
                                      Hashtable<Integer, List<String>> metadata,
                                      float threshold,
                                      List<StoredRecommendation> stored)
    {
        String algorithmProperty =
                ConfigurationManager.getProperty("quambo.similarity-algorithm")
                                    .trim();

        Set<Integer> IDs = metadata.keySet();

        for (Integer i: IDs)
        {
            if (!isRecommended(itemID, i))
            {
                if (i != itemID)
                {
                    float sim;

                    if (algorithmProperty.equals("cosine"))
                    {
                        sim = getCosineSimilarity(metadata.get(itemID),
                                metadata.get(i));
                    }
                    else if (algorithmProperty.equals("jaccard"))
                    {
                        sim = getJaccardSimilarity(metadata.get(itemID),
                                                   metadata.get(i));
                    }
                    else
                    {
                        throw new RuntimeException("quambo.similarity-" +
                                "algorithm incorrectly defined");
                    }

                    if (sim - threshold > 0.00001)
                    {
                        stored.add(new StoredRecommendation(itemID, i,
                                                    (short)Math.ceil(sim * 5)));
                    }
                }
            }
        }       
    }

    /**
     * Gets this similarity of the <code>firstItem</code> to the
     * <code>secondItem</code> in the repository. This method is an
     * implementation of the Jaccard Similarity Index.
     *
     * @param firstItemTerms The terms from first item to use in this comparison
     * @param secondItemTerms The terms from second item to use in this
     *                        comparison
     * @return the similarity of the firstItem to the secondItem
     */
    public float getJaccardSimilarity(List<String> firstItemTerms,
                                         List<String> secondItemTerms)
    {
        // Calculate the size of firstItem, secondItem, and the common terms
        Set<String> allTerms = new HashSet<String>();
        allTerms.addAll(firstItemTerms);
        int firstItemSize = allTerms.size();
        Set<String> secondItemSet = new HashSet<String>();
        secondItemSet.addAll(secondItemTerms);
        int secondItemSize = secondItemSet.size();
        allTerms.addAll(secondItemSet);
        int commonTermsSize = (firstItemSize + secondItemSize) -
                              allTerms.size();

        // Determine the Jaccard Similarity
        return (float) commonTermsSize / (float) allTerms.size();
    }

    /**
     * Gets this similarity of the <code>firstItem</code> to the
     * <code>secondItem</code> in the repository. This method is an
     * implementation of the Cosine Similarity.
     *
     * @param firstItemTerms The terms from first item to use in this comparison
     * @param secondItemTerms The terms from second item to use in this
     *                        comparison
     * @return the similiarity of the firstItem to the secondItem
     */
    public float getCosineSimilarity(List<String> firstItemTerms,
                                   List<String> secondItemTerms)
    {
        // Calculate the size of firstItem, secondItem, and the common terms
        Set<String> allTerms = new HashSet<String>();
        allTerms.addAll(firstItemTerms);
        int firstItemSize = allTerms.size();
        Set<String> secondItemSet = new HashSet<String>();
        secondItemSet.addAll(secondItemTerms);
        int secondItemSize = secondItemSet.size();
        allTerms.addAll(secondItemSet);
        int commonTermsSize = (firstItemSize + secondItemSize) -
                              allTerms.size();

        // Determine the Cosine similarity
        return (float) (commonTermsSize) /
               (float) (Math.pow((float) firstItemSize, 0.5f) *
                        Math.pow((float) secondItemSize, 0.5f));

    }

    /**
     * Writes all new Item2ItemRecommendations to the database.
     *
     * @param stored the List of tuples containing items which need to be
     *               written
     */
    public void writeRecommendations(List<StoredRecommendation> stored)
    {
        log.info(LogManager.getHeader(context, "writeRecommendations",
                                      "start"));

        for (StoredRecommendation s: stored)
        {
            Item2ItemRecommendation i2ir = create(s.getItem1ID());
            i2ir.setItem2ID(s.getItem2ID());
            i2ir.setSimilarity(s.getSimilarity());
            update(i2ir);
        }

        log.info(LogManager.getHeader(context, "writeRecommendations",
                                      "stop"));
    }

    /**
     * Determines if the <code>item2</code> is already a
     * <code>recommended Item</code> for <code>item1</code>.
     *
     * <p>
     * <code>item2ID</code> and <code>item1ID</code> can refer to the same
     * <code>Item</code>, however, it does not make sense because an
     * <code>Item</code> would always provide a similarity of 1.0 against
     * itself.
     * </p>
     *
     * @param item1ID the first item
     * @param item2ID the second item
     * @return true if <code>item2</code> is already recommendation for
     *         <code>item1</code>
     */
    public abstract boolean isRecommended(int item1ID, int item2ID);

    /**
     * Determines if the recommendations for the argument <code>Item</code>
     * need to be recalculated. The recommendations need to be recalculated if
     * they were last updated more than 24 hours ago.
     *
     * @param itemID <code>Item</code> to determine re-calculation status of
     * @return true if <code>item</code> needs its recommended items to be
     *         recaluclated; false if <code>item</code> does not need its
     *         recommedned items to be recalculated.
     */
    public abstract boolean needsRecalculation(int itemID);

    /**
     * Gets <code>quambo.similarity.terms</code> as defined in
     * <code>dspace.cfg</code>. Does not perform error checking.
     * 
     * @return a List of the terms to be used to extract metadata from items for
     *         the purposes of comparison.
     */
    private List<String> getDefinedAttributes()
    {
        // Extract the defined terms from dspace.cfg
        String terms =
                ConfigurationManager.getProperty("quambo.similarity.terms");

        String[] splitTerms = terms.split(",");

        for (int i = 0; i < splitTerms.length; i++)
        {
            splitTerms[i] = splitTerms[i].trim();
        }

        return Arrays.asList(splitTerms);
    }
    
    /**
     * Extracts metadata from all items and returns a Hashtable of Integers and
     * Strings representing the metadata terms.
     *
     * @return a List of Strings representing the metadata terms
     */
    private Hashtable<Integer, List<String>> getAllMetadata()
    {
        log.info(LogManager.getHeader(context, "getAllMetadata",
                                      "start"));

        // Extract the defined terms from dspace.cfg
        List<String> attributes = getDefinedAttributes();

        Hashtable<Integer, List<String>> h =
                new Hashtable<Integer, List<String>>();

        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        List<Item> items = itemDAO.getItems();

        for (Item i: items)
        {
            List<String> itemTerms = new ArrayList<String>();

            // Based on the attributes defined in dspace.cfg, extract metadata
            // from each item
            for (String attribute: attributes)
            {
                DCValue[] terms = i.getMetadata(attribute);

                for (DCValue d: terms)
                {
                    itemTerms.add(d.value);
                }
            }

            h.put(i.getID(), itemTerms);
        }

        log.info(LogManager.getHeader(context, "getAllMetadata",
                                      "stop"));
        
        return h;
    }
    
    /**
     * Inner-class which acts as a tuple for temporarily storing the IDs and
     * similarity of recommendations to be written after the calculation process
     */
    class StoredRecommendation
    {
        private int item1ID;
        private int item2ID;
        private short similarity;

        StoredRecommendation(int item1ID, int item2ID, short similarity)
        {
            this.item1ID = item1ID;
            this.item2ID = item2ID;
            this.similarity = similarity;
        }

        public int getItem1ID() {
            return item1ID;
        }

        public void setItem1ID(int item1ID) {
            this.item1ID = item1ID;
        }

        public int getItem2ID() {
            return item2ID;
        }

        public void setItem2ID(int item2ID) {
            this.item2ID = item2ID;
        }

        public short getSimilarity() {
            return similarity;
        }

        public void setSimilarity(short similarity) {
            this.similarity = similarity;
        }        
    }
}
