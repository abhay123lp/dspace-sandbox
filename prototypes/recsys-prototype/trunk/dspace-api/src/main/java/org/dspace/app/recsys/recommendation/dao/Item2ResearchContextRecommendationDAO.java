/*
 * Item2ResearchContextRecommendationDAO.java
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

import org.apache.log4j.Logger;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.researchContext.Essence;
import org.dspace.app.recsys.researchContext.KeyValue;
import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAO;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAOFactory;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import java.util.*;


/**
 * <code>Item2ResearchContextRecommendationDAO</code> handles database
 * access to create, retrieve, update, and delete
 * <code>Item2ResearchContextRecommendation</code> objects. Any non
 * database-specific code must be declared in this class instead of
 * <code>Item2ResearchContextRecommendationDAOPostgres</code>.
 *
 * @author Desmond Elliott
 */
public abstract class Item2ResearchContextRecommendationDAO
{
    /** Logs data to <code>dspace.log</code> */
    protected Logger log =
            Logger.getLogger(Item2ResearchContextRecommendationDAO.class);

    /** <code>DSpace Context</code> which is used for database access */
    protected Context context;

    /**
     * Creates and returns a new <code>Item2ResearchContextRecommendation</code>
     * object by creating a new table row in the database.
     *
     * @return A new <code>Item2ResearchContextRecommendation</code> object
     *         with <code>ID</code> equal to the <code>ID</code> of the database
     *         table row created.
     */
    public abstract Item2ResearchContextRecommendation create();

    /**
     * Retrieves an <code>Item2ResearchContextRecommendation</code> object from
     * the database based on the argument <code>id</code>. If the the
     * <code>ID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param id the <code>ID</code> of the
     *           <code>Item2ResearchContextRecommendation</code> to retrieve
     *           from the database
     * @return a <code>Item2ResearchContextRecommendation</code> object if it
     *         exists in the database, otherwise returns <code>null</code>.
     */
    public abstract Item2ResearchContextRecommendation retrieve(int id);

    /**
     * Retrieves an <code>Item2ResearchContextRecommendation</code> object from
     * the database based on the argument <code>uuid</code>. If the the
     * <code>UUID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param uuid the <code>UUID</code> of the
     *             <code>Item2ResearchContextRecommendation</code> to retrieve
     *             from the database
     * @return a <code>Item2ResearchContextRecommendation</code> object if it
     *         exists in the database, otherwise returns <code>null</code>.
     */
    public abstract Item2ResearchContextRecommendation retrieve(UUID uuid);

    /**
     * Updates the database row that represents the
     * <code>Item2ResearchContextRecommendation</code> object based on the
     * <code>ID</code> of the argument object. If the the <code>ID</code> does
     * not exist in the database, or any of the fields in the argument object
     * are null, this method will not succeed.
     *
     * @param i2rcr the <code>Item2ResearchContextRecommendation</code> to write
     *              to the database
     */
    public abstract void update(Item2ResearchContextRecommendation i2rcr);

    /**
     * Deletes the database row of the
     * <code>Item2ResearchContextRecommendation</code> object represented by the
     * <code>id</code> argument. If a database row in the
     * <code>quambo_item2researchcontext_recommendation</code> table does not
     * exist with the argument <code>id</code>, an Exception is thrown.
     *
     * @param id the <code>ID</code> of the
     *           <code>Item2ResearchContextRecommendation</code> object
     */
    public abstract void delete(int id);

    /**
     * Gets a List of the <code>Item2ResearchContextRecommendation</code>
     * objects for the <code>ResearchContext</code> in the argument sorted by
     * relevance.
     *
     * @param researchContextID the <code>ResearchContext</code> to get
     *                          recommendations for
     * @return a sorted List of <code>Item2ResearchContextRecommendation</code>
     *         objects
     */
    public abstract List<Item2ResearchContextRecommendation>
    getRecommendations(int researchContextID);

    /**
     * Determines if the <code>item</code> is already a
     * <code>recommended Item</code> for the <code>researchContext</code>.
     *
     * @param itemID the Item
     * @param researchContext the ResearchContext
     * @return true if <code>item</code> is already recommendation for
     *         <code>researchContext</code>; false if <code>item</code> is not
     *         already a recommendation for <code>researchContext</code>.
     */
    public abstract boolean isRecommended(int itemID,
                                          ResearchContext researchContext);

    /**
     * Determines the item-to-research context recommendations for the arugment
     * <code>ResearchContext</code>. Initially evaluates each existing
     * <code>Item2ResearchContextRecommendation</code> and determines
     * if it is still above the minimum similarity threshold for
     * recommendations. Proceeds to then evaluate every <code>Item</code>
     * in the repository and creates new recommendations as necessary.
     *
     * @param r the <code>ResearchContext</code> to calculate recommendations
     *          for
     */
    public void calculateRecommendations(ResearchContext r)
    {
        Hashtable<Integer, List<String>> metadata = getAllItemMetadata();

        String definedThreshold =
                ConfigurationManager.getProperty("quambo.similarity-threshold")
                                    .trim();

        float threshold = Float.parseFloat(definedThreshold);

        if (needsRecalculation(r))
        {
            updateRecommendations(r, metadata, threshold);
            determineNewRecommendations(r, metadata, threshold);
        }
    }

    /**
     * Determines the item-to-research context recommendations for every
     * <code>ResearchContext</code>. This method retrieves all <code>Item</code>
     * metadata before performing calculations to reduce repetitive
     * database access.
     */
    public void calculateAllRecommendations()
    {
        log.info(LogManager.getHeader(context, "calculateAllRecommendations",
                                      "start"));

        Hashtable<Integer, List<String>> metadata = getAllItemMetadata();

        String definedThreshold =
                ConfigurationManager.getProperty("quambo.similarity-threshold")
                                    .trim();

        float threshold = Float.parseFloat(definedThreshold);

        log.info(LogManager.getHeader(context, "determineRecommendations",
                                      "start"));

        List<ResearchContext> list =
                ResearchContextDAOFactory.getInstance(context)
                                         .getAllResearchContexts();

        for(ResearchContext r: list)
        {
            updateRecommendations(r, metadata, threshold);
            determineNewRecommendations(r, metadata, threshold);
        }
        log.info(LogManager.getHeader(context, "determineRecommendations",
                                      "stop"));

        log.info(LogManager.getHeader(context, "calculateAllRecommendations",
                                      "stop"));
    }

    /**
     * Determines if the recommendations for the argument
     * <code>ResearchContext</code> need to be recalculated. The recommendations
     * need to be recalculated if they were last updated more than # days
     * specified in dspace.cfg ago.
     *
     * @param r <code>ResearchContext</code> to determine re-calculation status of
     * @return true if <code>r</code> needs its recommended items to be
     *         recaluclated; false if <code>r</code> does not need its
     *         recommedned items to be recalculated.
     */
    public abstract boolean needsRecalculation(ResearchContext r);

    /**
     * Updates <code>Item2ResearchContextRecommendation</code> for
     * <code>researchContext</code>. Deletes existing
     * <code>Item2ResearchContextRecommendation</code> which are no longer
     * applicable.
     *
     * @param r the <code>ResearchContext</code> to update
     *                        recommendations for
     * @param metadata Hashtable storing the list of terms for every Item
     * @param threshold minimum similarity threshold for recommendations*
     */
    public void updateRecommendations(ResearchContext r,
                                      Hashtable<Integer, List<String>> metadata,
                                      float threshold)
    {
        List<Item2ResearchContextRecommendation> currentRecommendations =
                getRecommendations(r.getID());

        String algorithmProperty =
                ConfigurationManager.getProperty("quambo.similarity-algorithm")
                        .trim();

        BookmarkDAO bDAO = BookmarkDAOFactory.getInstance(context);

        for (Item2ResearchContextRecommendation i2rcr: currentRecommendations)
        {
            if (bDAO.isBookmarked(i2rcr.getItem().getID(), r))
            {
                delete(i2rcr.getID());
                continue;
            }

            float relevance;

            if (algorithmProperty.equals("cosine"))
            {
                relevance =
                        getCosineRelevance(metadata.get(i2rcr.getItemID()), r);
            }
            else if (algorithmProperty.equals("jaccard"))
            {
                relevance = getJaccardRelevance(metadata.get(i2rcr.getItemID()),
                                                             r);
            }
            else
            {
                throw new RuntimeException("quambo.similarity-algorithm" +
                        "incorrectly defined");
            }

            if (!(Math.abs(relevance - i2rcr.getRelevance()) < 0.000001))
            {
                if (threshold - relevance > 0.00001)
                {
                    delete(i2rcr.getID());
                    continue;
                }

                i2rcr.setRelevance((int)Math.ceil(relevance * 5));
            }

            i2rcr.setLastUpdated(new Date());

            update(i2rcr);
        }
    }
    /**
     * Calculates research context-to-item recommendations for the
     * <code>ResearchContext</code> passed as an argument. Only creates new
     * <code>Item2ResearchContextRecommendation</code>s if the
     * <code>relevance</code> of the argument <code>Item</code> being processed
     * to the <code>ResearchContext</code> is greater than the
     * <code>minimum similarity threshold</code>.
     *
     * @param r <code>ResearchContext</code> to determine recommendations
     *             for against the entire collection of <code>Item</code>s in
     *             the repository.
     * @param metadata Hashtable storing the list of terms for every Item
     * @param threshold minimum similarity threshold for recommendations
     */
    public void determineNewRecommendations(ResearchContext r,
                                      Hashtable<Integer, List<String>> metadata,
                                      float threshold)
    {
        String algorithmProperty =
                ConfigurationManager.getProperty("quambo.similarity-algorithm")
                        .trim();

        Set<Integer> IDs = metadata.keySet();

        BookmarkDAO bDAO = BookmarkDAOFactory.getInstance(context);
        for (Integer i: IDs)
        {
            if (!isRecommended(i, r) && !bDAO.isBookmarked(i, r))
            {
                float similarity;

                if (algorithmProperty.equals("cosine"))
                {
                    similarity = getCosineRelevance(metadata.get(i), r);
                }
                else if (algorithmProperty.equals("jaccard"))
                {
                    similarity = getJaccardRelevance(metadata.get(i), r);
                }
                else
                {
                    throw new RuntimeException("quambo.similarity-algorithm" +
                            "incorrectly defined");
                }

                if (similarity - threshold > 0.00001)
                {
                    Item2ResearchContextRecommendation i2rcr = create();
                    i2rcr.setItemID(i);
                    i2rcr.setResearchContextID(r.getID());
                    i2rcr.setRelevance((int)Math.ceil(similarity * 5));
                    i2rcr.setLastUpdated(new Date().toString());
                    update(i2rcr);
                }
            }
        }
    }

    /**
     * Gets this relevance of the <code>Item</code> to the
     * <code>ResearchContext</code>. This method is an
     * implementation of the Cosine Similarity.
     *
     * @param itemTerms the Item to use in this comparison
     * @param r the ResearchContext to use in this comparison
     * @return the similiarity of the firstItem to the secondItem
     */
    public float getCosineRelevance(List<String> itemTerms, ResearchContext r)
    {
        // The Essence contains no KeyValue objects so there can be no
        // relevant items
        if (r.getKeyValueObjects().size() == 0)
        {
            return 0;
        }
        
        // Extract the defined terms from dspace.cfg
        List<String> definedAttributes = getDefinedAttributes();

        // Get all of the Research Context's KeyValue objects and put them into
        // an ArrayList
        List<String> essenceKeys = new ArrayList<String>();

        int essenceWeight = 0;

        for(KeyValue k: r.getKeyValueObjects())
        {
            essenceWeight += k.getValue();
            essenceKeys.add(k.getKey());
        }

        // Combine the item and ResearchContext keys into a HashSet
        
        HashSet<String> allTerms = new HashSet<String>();
        allTerms.addAll(essenceKeys);
        allTerms.addAll(itemTerms);

        HashSet<String> commonTerms = new HashSet<String>(allTerms);
        commonTerms.retainAll(essenceKeys);
        commonTerms.retainAll(itemTerms);

        int commonTermsWeight = 0;

        // Calculate the weighted size of the intersection and union
        for (String key: commonTerms)
        {
            if (r.hasKeyValue(key))
            {
                for (Essence e: r.getEssences())
                {
                    for (String attribute: definedAttributes)
                    {
                        commonTermsWeight +=
                                r.getValue(key, attribute, e.getID());
                    }
                }
            }
        }

        // Determine the Cosine similarity
        return (float) (commonTermsWeight) /
               (float) (Math.pow((float) itemTerms.size(), 0.5f) *
                        Math.pow((float) essenceWeight, 0.5f));

    }

    /**
     * Gets this relevance of the <code>Item</code> to the
     * <code>ResearchContext</code>. This method is an
     * implementation of the Jaccard Similarity.
     *
     * @param itemTerms The Item in question
     * @param r the Research Context
     * @return an integer representing the Jaccard Index between the EPerson's
     * interests and the Item's keywords
     *
     */
    public float getJaccardRelevance(List<String> itemTerms, ResearchContext r)
    {
        // The Essence contains no KeyValue objects so there can be no
        // relevant items
        if (r.getKeyValueObjects().size() == 0)
        {
            return 0;
        }

        // Extract the defined terms from dspace.cfg
        List<String> definedAttributes = getDefinedAttributes();

        // Get all of the Research Context's KeyValue objects and put them into
        // an ArrayList
        List<String> essenceKeys = new ArrayList<String>();

        for(KeyValue k: r.getKeyValueObjects())
        {
            essenceKeys.add(k.getKey());
        }

        // Combine the item and ResearchContext keys into a HashSet

        HashSet<String> allTerms = new HashSet<String>();
        allTerms.addAll(essenceKeys);
        allTerms.addAll(itemTerms);

        int allTermsWeight = 0;

        // Calculate the weighted size of the allTerms
        for (String key: allTerms)
        {
            if (r.hasKeyValue(key))
            {
                for (Essence e: r.getEssences())
                {
                    for (String attribute: definedAttributes)
                    {
                        allTermsWeight += r.getValue(key, attribute, e.getID());
                    }
                }
            }
            else
            {
                allTermsWeight++;
            }
        }

        HashSet<String> commonTerms = new HashSet<String>(allTerms);
        commonTerms.retainAll(essenceKeys);
        commonTerms.retainAll(itemTerms);

        int commonTermsWeight = 0;

        // Calculate the weighted size of the intersection and union
        for (String key: commonTerms)
        {
            if (r.hasKeyValue(key))
            {
                for (Essence e: r.getEssences())
                {
                    for (String attribute: definedAttributes)
                    {
                        commonTermsWeight +=
                                r.getValue(key, attribute, e.getID());
                    }
                }
            }
        }

        // Return the Jaccard Index
        return (float) commonTermsWeight / (float) allTermsWeight;
    }

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
        String definedAttributes =
                ConfigurationManager.getProperty("quambo.similarity.terms");

        String[] splitAttributes = definedAttributes.split(",");

        for (int i = 0; i < splitAttributes.length; i++)
        {
            splitAttributes[i] = splitAttributes[i].trim();
        }

        return Arrays.asList(splitAttributes);
    }

    /**
     * Extracts metadata from all items and returns a Hashtable of Integers and
     * Strings representing the metadata terms.
     *
     * @return a List of Strings representing the metadata terms
     */
    private Hashtable<Integer, List<String>> getAllItemMetadata()
    {
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

        return h;
    }    
}

