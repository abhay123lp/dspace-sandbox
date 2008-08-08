/*
 * Item2ItemRecommendation.java
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

import java.util.Date;
import java.util.UUID;
import java.text.SimpleDateFormat;

/**
 * <code>Item2ItemRecommendation</code> objects are created as a result of a
 * similarity calculation between two repository items. It represents the
 * similarity of the first item to the second item. The site administrator
 * can set the minimum similarity threshold in <code>dspace.cfg</code>.
 *
 * <p>
 * Determining recommendations in real-time is not thoroughly tested and could
 * result in <code>java.lang.OutOfMemory</code> errors due to the nature of the
 * process.
 * </p>
 *
 * @author Desmond Elliott
 */
public class Item2ItemRecommendation implements
        Comparable<Item2ItemRecommendation> {

    /** <code>ID</code> of this <code>Item2ItemRecommendation</code> object */
    public int id;

    /** <code>ID</code> of the first <code>Item</code>*/
    public int item1ID;

    /** <code>ID</code> of second <code>Item</code> */
    public int item2ID;

    /** The <code>similiarity</code> of first <code>Item</code> to
     *  the second <code>Item</code> */
    public int similarity;

    /** The <code>Date</code> this object was last updated */
    public Date lastUpdated;

    /** The <code>UUID</code> of this <code>Item2ItemRecommendation</code>
     * object</code> */
    public UUID uuid;

    /** <code>DSpace Context</code> used to retrieve the encapsulated
     * <code>Item</code>s */
    private Context context;

    /**
     * Creates a new <code>Item2ItemRecommendation</code> object which will
     * represent a recommendation of the first <code>Item</code> to the second
     * <code>Item</code>.
     *
     * @param id the database ID to be associated with this
     *           <code>Item2ItemRecommendation</code> object
     * @param context DSpace <code>Context</code> which is used to retrieve the
     *                encapsulated <code>Item</code>s 
     */
    public Item2ItemRecommendation(int id, Context context)
    {
        this.id = id;
        this.context = context;
    }

    /**
     * Gets the ID of this <code>Item2ItemRecommendation</code> object
     *
     * @return the ID of this <code>Item2ItemRecommendation</code> object
     */
    public int getID()
    {
        return id;
    }

    /**
     * Gets the <code>ID</code> of the first <code>Item</code>
     *
     * @return the ID of the first <code>Item<code>
     */
    public int getItem1ID()
    {
        return item1ID;
    }

    /**
     * Sets the <code>ID</code> of the first <code>Item</code>
     *
     * @param item1ID the <code>ID</code> of the first <code>Item</code>
     */
    public void setItem1ID(int item1ID)
    {
        this.item1ID = item1ID;
    }

    /**
     * Gets the Item object which represents the first <code>Item</code>
     *
     * @return item <code>Item</code> object which represents the first
     *              <code>Item</code>
     */
    public Item getItem1()
    {
        return ItemDAOFactory.getInstance(context).retrieve(item1ID);
    }

    /**
     * Get the <code>ID</code> of the second <code>Item</code>
     *
     * @return the <code>ID</code> of the second <code>Item<code>
     */
    public int getItem2ID()
    {
        return item2ID;
    }

    /**
     * Sets the <code>ID</code> of the second <code>Item</code>
     *
     * @param recommendedItemID the <code>ID</code> of the second
     *                          <code>Item</code>
     */
    public void setItem2ID(int recommendedItemID)
    {
        this.item2ID = recommendedItemID;
    }

    /**
     * Gets the <code>Item</code> object which represents the second
     * <code>Item</code>
     *
     * @return an <code>Item</code> which represents the
     *         second <code>Item</code>
     */
    public Item getItem2()
    {
        return ItemDAOFactory.getInstance(context).retrieve(item2ID);
    }

    /**
     * Gets the <code>similarity</code> of the first <code>Item</code> to
     * the second<code>Item</code>
     *
     * @return the <code>similarity</code> of the first
     *         <code>Item</code> to the second <code>Item</code>
     */
    public int getSimilarity()
    {
        return similarity;
    }

    /**
     * Sets the <code>similarity</code> of the first <code>Item</code> to
     * the second <code>Item</code>
     *
     * @param similarity the <code>similarity</code> of the first
     *                   <code>Item</code> to the second <code>Item</code>
     */
    public void setSimilarity(int similarity)
    {
        this.similarity = similarity;
    }

    /**
     * Gets the <code>Date</code> this <code>Item2ItemRecommendation</code> was
     * last updated
     *
     * @return the <code>Date</code> this <code>Item2ItemRecommendation</code>
     *         was last updated
     */
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Gets the <code>Date</code> this <code>Item2ItemRecommendation</code> was
     * last updated
     *
     * @param lastUpdated the <code>Date</code> this
     *                    <code>Item2ItemRecommendation</code> was last updated
     */
    public void setLastUpdated(Date lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Sets the <code>Date</code> this <code>Item2ItemRecommendation</code> was
     * last updated
     *
     * @param lastUpdated the <code>Date</code> this
     *                    <code>Item2ItemRecommendation</code> was last updated.
     *                    The expected format of this <code>String</code> is:
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
     * Gets the <code>UUID</code> of this <code>Item2ItemRecommendation</code>
     * object
     *
     * @return the <code>UUID</code> of this
     *         <code>Item2ItemRecommendation</code> object
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * Sets the <code>UUID</code> of this <code>Item2ItemRecommendation</code>
     * object
     *
     * @param uuid the <code>UUID</code> of this
     *         <code>Item2ItemRecommendation</code> object
     */
    public void setUUID(UUID uuid)
    {
        this.uuid = uuid;
    }

    /**
     * Compares two <code>Item2ItemRecommendation</code> objects by their
     * <code>similarity</code>
     *
     * <p>
     * The term <code>less similar</code> means the <code>similarity</code>
     * between the first <code>Item</code> and the second
     * <code>Item</code> is less than the <code>similarity</code> between the
     * first <code>Item</code> and the second <code>Item</code> of the
     * argument. The opposite principle applies to the term
     * <code>more similar</code>.
     * </p>
     *
     * @param item2ItemRecommendation the <code>Item2ItemRecommendation</code>
     *                                to be compared
     *
     * @return the value 0 if the argument <code>Item2ItemRecommendation</code>
     * has the same similarity; a value of less than 0 if this
     * <code>Item2ItemRecommendation</code> is less similar; a value of greater
     * than 0 if this <code>Item2ItemRecommendation</code> is more similar.
     *
     */
    public int compareTo(Item2ItemRecommendation item2ItemRecommendation)
    {
        if (this.similarity == item2ItemRecommendation.getSimilarity())
        {
            return 0;
        }
        else if (this.similarity < item2ItemRecommendation.getSimilarity())
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
