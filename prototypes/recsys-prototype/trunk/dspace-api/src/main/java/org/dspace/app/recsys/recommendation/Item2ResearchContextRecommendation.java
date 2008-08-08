/*
 * Item2ResearchContextRecommendation.java
 *
 * Version: $Revision $
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
package org.dspace.app.recsys.recommendation;

import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;

import java.util.*;
import java.text.*;

/**
 * <code>Item2ResearchContextRecommendation</code> objects are created as a
 * result of calculating the <code>relevance</code> of an <code>Item</code> to a
 * registered user's <code>ResearchContext</code> as a function of the metadata
 * extracted from <code>Bookmarked</code> <code>Item</code>s. The minimum
 * relevance threshold for a <code>ResearchContext</code> is set by the
 * repository administrator. The recalculation time is set by the repository
 * administrator.
 *
 * <p>
 * TODO: Allow the user to set the minimum relevance threshold.
 * </p>
 *
 * @author Desmond Elliott
 */
public class Item2ResearchContextRecommendation
        implements Comparable<Item2ResearchContextRecommendation>
{
    /** <code>ID</code> of this <code>Item2ResearchContextRecommendation</code>
      */    
    private int id;

    /** <code>ID</code> of the <code>Item</code> to be recommended */
    private int itemID;

    /** The relevance of the recommended <code>Item</code> to the
     * <code>ResearchContext</code> */
    private int relevance;

    /** <code>ID</code> of the <code>ResearchContext</code> this
     <code>Item2ResearchContextRecommendation</code> is made with respect to */
    private int researchContextID;

    /** <code>Date</code> this <code>Item2ResearchContextRecommendation</code>
     * was last updated */
    private Date lastUpdated;

    /** <code>DSpace Context</code> which is used to retrieve the encapsualted
     * <code>Item</code> and <code>ResearchContext</code> objects. */
    private Context context;

    /**
     * Creates a new <code>Item2ResearchContextRecommendation</code> object
     * which represents the relevance of an <code>Item</code> to a
     * <code>ResearchContext</code>.
     *
     * @param id the database <code>ID</code> to be associated with this
     *           <code>Item2ResearchContextRecommendation</code> object
     * @param context <code>DSpace Context</code> which is used to retrieve the
     *                encapsulated <code>Item</code> and
     *                <code>ResearchContext</code>
     */
    public Item2ResearchContextRecommendation(int id, Context context)
    {
        this.id = id;
            this.context = context;
    }

    /**
     * Gets the <code>ID</code> of this
     * <code>Item2ResearchContextRecommendation</code>
     *
     * @return the <code>ID</code> of this
     *         <code>Item2ResearchContextRecommendation</code>
     */
    public int getID()
    {
        return id;
    }

    /**
     * Gets the <code>ID</code> of the <code>ResearchContext</code> this
     * recommendation is with respect to
     *
     * @return the <code>ID</code> of the <code>ResearchContext</code> this
     *         recommendation is with respect to
     */
    public int getResearchContextID()
    {
        return researchContextID;
    }

    /**
     * Sets the <code>ID</code> of the <code>ResearchContext</code> this
     * recommendation is with respect to
     *
     * @param id the <code>ID</code> of the <code>ResearchContext</code> this
     *           recommendation is with respect to
     */
    public void setResearchContextID(int id)
    {
        researchContextID = id;
    }

    /**
     * Gets the <code>ID</code> of the recommended <code>Item</code>
     *
     * @return the ID of the recommended <code>Item<code>
     */
    public int getItemID()
    {
        return itemID;
    }

    /**
     * Sets the <code>ID</code> of the recommended <code>Item</code>
     *
     * @param itemID the ID of the recommended <code>Item<code>
     */
    public void setItemID(int itemID)
    {
        this.itemID = itemID;
    }

    /**
     * Gets the recommended <code>Item</code>
     *
     * @return item the recommended <code>Item</code>
     */
    public Item getItem()
    {
        return ItemDAOFactory.getInstance(context).retrieve(getItemID());
    }

    /**
     * Gets the relevance of the recommended <code>Item</code> to the
     * <code>ResearchContext</code>
     *
     * @return the relevance of the recommended <code>Item</code> to the
     *         <code>ResearchContext</code>
     */
    public int getRelevance()
    {
        return relevance;
    }

    /**
     * Sets the relevance of the recommended <code>Item</code> to the
     * <code>ResearchContext</code>
     *
     * @param relevance the relevance of the recommended <code>Item</code> to
     *                  the <code>ResearchContext</code>
     */
    public void setRelevance(int relevance)
    {
        this.relevance = relevance;
    }

    /**
     * Gets the <code>Date</code> this
     * <code>Item2ResearchContextRecommendation</code> was last updated
     *
     * @param date the <code>Date</code> this
     *             <code>Item2ResearchContextRecommendation</code> was last
     *             updated
     */
    public void setLastUpdated(Date date)
    {
        lastUpdated = date;
    }

    /**
     * Sets the <code>Date</code> this
     * <code>Item2ResearchContextRecommendation</code> was last updated
     *
     * @param lastUpdated the <code>Date</code> this
     *                    <code>Item2ResearchContextRecommendation</code> was
     *                    last updated.The expected format of the
     *                    <code>String</code> argument is:
     *                    <code>EEE MMM d HH:mm:ss z yyyy</code>.
     *
     * @see SimpleDateFormat
     */
    public void setLastUpdated(String lastUpdated)
    {
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");
        try
        {
            this.lastUpdated = sdf.parse(lastUpdated);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Gets the <code>Date</code> this
     * <code>Item2ResearchContextRecommendation</code> was last updated
     *
     * @return the <code>Date</code> this <code>Item2ItemRecommendation</code>
     *         was last updated
     */
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Compares two <code>Item2ResearchContextRecommendation</code> objects by
     * their <code>relevance</code>.
     *
     * <p>
     * The term <code>less relevant</code> means the <code>relevance</code>
     * of this <code>Item</code> to this <code>ResearchContext</code> is less
     * than the <code>relevance</code> of the recommended <code>Item</code>
     * and the <code>ResearchContext</code> supplied in the argument. The
     * opposite principle applies to the term <code>more relevant</code>.
     * </p>
     *
     * @param item2ResearchContextRecommendation
     *            the <code>Item2ResearchContextRecommendation</code> to be
     *            compared
     *
     * @return the value 0 if the argument
     * <code>Item2ResearchContextRecommendation</code> has the same relevance; a
     * value of less than 0 if this
     * <code>Item2ResearchContextRecommendation</code> is less relevant; a value
     * of greater than 0 if this <code>Item2ResearchContextRecommendation</code>
     * is more relevant.
     *
     */
    public int compareTo(Item2ResearchContextRecommendation
            item2ResearchContextRecommendation)
    {
        if (this.relevance == item2ResearchContextRecommendation.getRelevance())
        {
            return 0;
        }
        else if (this.relevance >
                item2ResearchContextRecommendation.getRelevance())
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
