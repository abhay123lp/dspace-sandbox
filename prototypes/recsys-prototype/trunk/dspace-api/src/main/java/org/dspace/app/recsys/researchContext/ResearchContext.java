/*
 * ResearchContext.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.recsys.researchContext;

import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.researchContext.dao.KeyValueDAOFactory;
import org.dspace.app.recsys.researchContext.dao.KeyValueDAO;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;

import org.dspace.eperson.EPerson;
import org.dspace.core.Context;

import java.util.*;

/**
 * A <code>ResearchContext</code> attempts to characterise a user's explicit
 * interactions with DSpace to generate recommendations.
 *
 * <p>
 * A <code>ResearchContext</code> is a container object which stores references
 * to <code>Bookmark</code>s, <code>Item2ResearchContextRecommendation</code>s,
 * and metadata representing a registered user's interactions with the
 * repository, known as the <code>Essence</code>.
 * </p>
 *
 * @author Desmond Elliott 
 */
public class ResearchContext implements Comparable<ResearchContext>
{
    /** <code>ID</code> of this <code>ResearchContext</code */
    private int id;

    /** DSpace <code>Context</code> which is used to retrieve the encapsulated
      * <code>Item</code> or <code>ResearchContext</code> */
    private Context context;

    /** User-defined <code>name</code> of this <code>ResearchContext</code> */
    private String name;

    /** The <code>EPerson</code> who created and owns this
     * <code>ResearchContext</code> */
    private EPerson eperson;

    /** The <code>Bookmarks</code> associated with this
     * <code>ResearchContext</code> */
    private Set<Bookmark> bookmarks;

    /** The <code>Essences</code> which support this
     * <code>ResearchContext</code>, there can be more than
     * one if <code>remote ResearchContexts</code> feed this one.
     */
    private Set<Essence> essences;

    /** The metadata representing the owner's interactions with this
     * <code>ResearchContext</code> */
    private Set<KeyValue> keyValueObjects;

    /** The <code>Item-to-ResearchContext</code> recommendations for this
     * <code>ResearchContext</code> */
    private Set<Item2ResearchContextRecommendation> recommendations;

    /** The <code>UUID</code> of this <code>ResearchContext</code> */
    private UUID uuid;

    /** The <code>ID</code> of the <code>Essence</code> created when this
     * <code>ResearchContext</code> was created */
    private int localEssenceID;

    /**
     * Creates a new <code>ResearchContext</code> and empty containers for
     * <code>Bookmarks</code>, <code>Recommendations</code>, and the
     * <code>Essence</code>.
     * 
     * @param id the <code>database ID</code> of this
     *           <code>ResearchContext</code>.
     * @param context a <code>DSpace context</code> to allow retrieval of
     *                encapsulated objects
     */
    public ResearchContext(int id, Context context)
    {
        this.id = id;
        bookmarks = new HashSet<Bookmark>();
        essences = new HashSet<Essence>();
        keyValueObjects = new HashSet<KeyValue>();
        recommendations = new HashSet<Item2ResearchContextRecommendation>();
        this.context = context;
    }

    /**
     * Gets the <code>ID</code> of this <code>ResearchContext</code>
     *
     * @return the <code>ID</code> of this <code>ResearchContext</code>
     */
    public int getID()
    {
        return id;
    }

    /**
     * Sets the <code>UUID</code> of this <code>ResearchContext</code>
     *
     * @param uuid <code>UUID</code> to assign
     */
    public void setUUID(UUID uuid)
    {
        this.uuid = uuid;
    }

    /**
     * Gets the <code>UUID</code> of this <code>ResearchContext</code>
     *
     * @return the <code>UUID</code> of this <code>ResearchContext</code>
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * Gets the name of this <code>ResearchContext</code>
     *
     * @return the name of this <code>ResearchContext</code>
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of this <code>ResearchContext</code>
     *
     * @param name name to set this <code>ResearchContext</code>'s name to.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the <code>EPerson</code> who created this
     * <code>ResearchContext</code>
     *
     * @return the <code>EPerson</code> who created this
     *         <code>ResearchContext</code>
     */
    public EPerson getEperson()
    {
        return eperson;
    }

    /**
     * Sets the <code>EPerson</code> who owns this <code>ResearchContext</code>.
     *
     * @param eperson the <code>EPerson</code> to set as owner.
     */
    public void setEperson(EPerson eperson)
    {
        this.eperson = eperson;
    }

    /**
     * Adds a <code>Boomark</code> to this <code>ResearchContext</code>
     *
     * @param b the <code>Bookmark</code> to add
     */
    public void addBookmark(Bookmark b)
    {
        bookmarks.add(b);
    }

    /**
     * Removes a <code>Bookmark</code> from this <code>ResearchContext</code>
     *
     * @param b the <code>Bookmark</code> to remove
     */
    public void removeBookmark(Bookmark b)
    {
        for (Bookmark bookmark: getBookmarks())
        {
            if (bookmark.getID() == b.getID())
            {
                bookmarks.remove(bookmark);
                return;
            }
        }
    }

    /**
     * Removes all <code>Bookmarks</code> from this <code>ResearchContext</code>
     */
    public void clearBookmarks()
    {
        bookmarks = new HashSet<Bookmark>();
    }

    /**
     * Retrieves all <code>Bookmarks</code> from this
     * <code>ResearchContext</code>
     *
     * @return a <code>Set</code> containing all <code>Bookmarks</code>
     */
    public Set<Bookmark> getBookmarks()
    {
        return bookmarks;
    }

    /**
     * Retrieve all <code>Essences</code> associated with this
     * <code>ResearchContext</code>
     *
     * @return a <code>Set</code> of <code>Essence</code>s
     */
    public Set<Essence> getEssences() {
        return essences;
    }

    /**
     * Set the <code>Essences</code> of this <code>ResearchContext</code>
     *
     * @param essences a <code>Set</code> of <code>Essence</code>s
     */
    public void setEssences(Set<Essence> essences) {
        this.essences = essences;
    }

    /**
     * Adds an <code>Essence</code> to this <code>ResearchContext</code>
     *
     * @param e the <code>Essence</code> to add
     */
    public void addEssence(Essence e)
    {
        essences.add(e);
    }

    /**
     * Removes an <code>Essence</code> from this <code>ResearchContext</code>,
     * given its <code>ID</code>
     *
     * @param id the <code>ID</code> of the <code>Essence</code> to remove
     */
    public void removeEssence(int id)
    {
        for (Essence e: getEssences())
        {
            if (e.getID() == id)
            {
                essences.remove(e);
                return;
            }
        }
    }

    /**
     * Returns the <code>KeyValue</code> objects for this
     * <code>ResearchContext</code>
     *
     * @return the <code>Set</code> of the <code>KeyValue</code> objects
     */
    public Set<KeyValue> getKeyValueObjects()
    {
        return keyValueObjects;
    }

    /**
     * Returns the <code>Value</code> of a <code>KeyValue</code> object given a
     * key, a type, and an essenceID
     *
     * @param key the <code>Key</code> of the <code>KeyValue</code>
     * @param type the <code>Type</code> of the <code>KeyValue</code>
     * @param essenceID the <code>Essence ID</code> of the <code>KeyValue</code>
     * @return the <code>Value</code> of the <code>KeyValue</code> which matches
     *         the arguments or 0 if no such <code>KeyValue</code> object exists
     */
    public int getValue(String key, String type, int essenceID)
    {
        for (KeyValue kv: getKeyValueObjects())
        {
            if (kv.getKey().equals(key) &&
                kv.getType().equals(type) &&
                kv.getEssenceID() == essenceID)
            {
                return kv.getValue();
            }
        }

        // Key does not exist, return value of 0
        return 0;
    }

    /**
     * Adds a <code>KeyValue</code> to this <code>ResearchContext</code>. If the
     * metadata being added already exists, add the new value to the existing
     * value; otherwise, create a new <code>KeyValue</code> and add it to the
     * <code>Essence</code>.
     *
     * @param key the <code>Key</code> of the <code>KeyValue</code>
     * @param value the <code>Value</code> of the <code>KeyValue</code>
     * @param type the <code>Type</code> of the <code>KeyValue</code>
     * @param essenceID the <code>Essence ID</code> of the <code>KeyValue</code>
     */
    public void addKeyValue(String key, int value, String type, int essenceID)
    {
        for (KeyValue kv: getKeyValueObjects())
        {
            if (kv.getKey().equals(key) &&
                kv.getType().equals(type) &&
                kv.getEssenceID() == essenceID)
            {
                kv.setValue(kv.getValue() + value);

                return;
            }
        }

        // There is not a KeyValue object to augment so create a new object
        addNewKeyValue(key, value, type, essenceID);
    }

    /**
     * Private method for creating a new <code>KeyValue</code> object and adding
     * it to the <code>Essence</code>.
     *
     * @param key the <code>Key</code> of the <code>KeyValue</code>
     * @param value the <code>Value</code> of the <code>KeyValue</code>
     * @param type the <code>Type</code> of the <code>KeyValue</code>
     * @param essenceID the <code>Essence ID</code> of the <code>KeyValue</code>
     */
    private void addNewKeyValue(String key, int value, String type,
                                int essenceID)
    {
        KeyValueDAO kvpDAO = KeyValueDAOFactory.getInstance(context);
        KeyValue newKeyValue = kvpDAO.create();
        newKeyValue.setKey(key);
        newKeyValue.setValue(value);
        newKeyValue.setType(type);
        newKeyValue.setEssenceID(essenceID);
        newKeyValue.setResearchContextID(getID());

        getKeyValueObjects().add(newKeyValue);
    }

    /**
     * Changes the <code>Value</code> of a <code>KeyValue</code> by a positive
     * or negative amount and removes the <code>KeyValue</code> if the change
     * results in a value of 0.
     *
     * @param key the <code>Key</code> of the <code>KeyValue</code>
     * @param delta the positive or negative change to make to the
     *              <code>Value</code>
     * @param type the <code>Type</code> of the <code>KeyValue</code>
     * @param essenceID the <code>Essence ID</code> of the <code>KeyValue</code>
     */
    public void changeKeyValue(String key, int delta, String type,
                               int essenceID)
    {
        for (KeyValue kv: getKeyValueObjects())
        {
            if (kv.getKey().equals(key) &&
                kv.getType().equals(type) &&
                kv.getEssenceID() == essenceID)
            {
                kv.setValue(kv.getValue() + delta);

                if (kv.getValue() == 0)
                {
                    removeKeyValue(key, type, essenceID);
                    KeyValueDAOFactory.getInstance(context)
                                      .delete(kv.getKeyValueID());
                }

                return;
            }
        }
    }

    /**
     * Removes a <code>KeyValue</code> object from the <code>Essence</code>
     *
     * @param key the <code>Key</code> of the <code>KeyValue</code>
     * @param type the <code>Type</code> of the <code>KeyValue</code>
     * @param essenceID the <code>Essence ID</code>
     */
    public void removeKeyValue(String key, String type, int essenceID)
    {
        for (KeyValue kv: getKeyValueObjects())
        {
            if (kv.getKey().equals(key) &&
                kv.getType().equals(type) &&
                kv.getEssenceID() == essenceID)
            {
                getKeyValueObjects().remove(kv);
                
                return;
            }
        }
    }

    /**
     * Determines if a <code>KeyValue</code> with a given <code>Key</code>
     * exists in any of this <code>ResearchContext's</code>
     * <code>Essences</code>
     *
     * @param key the <code>Key</code> to find
     * @return <code>true</code> if at least one <code>KeyValue's</code>
     *         <code>Key</code> is equal to key
     */
    public boolean hasKeyValue(String key)
    {
        for (KeyValue kv: getKeyValueObjects())
        {
            String kvKey = kv.getKey();
            if (kvKey.equals(key))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a <code>Hashtable</code> of <code>KeyValues</code> and the font
     * size they should be for representation in the UI
     *
     * @return a <code>Hashtable</code> of <code>KeyValues</code> and font sizes
     *         for presentation on <code>ResearchContext</code> page.
     */
    public Hashtable<KeyValue,Integer> getEssenceCloud()
    {
        Hashtable<KeyValue, Integer> cloud = new Hashtable<KeyValue, Integer>();
        int largestKey = getLargestKey();

        for (KeyValue kv: getKeyValueObjects())
        {
            float modifier = (26 - 10) / (float)Math.log(largestKey);
            int size = (int) (Math.log(kv.getValue()) * modifier) + 10;

            cloud.put(kv, size);
        }

        return cloud;
    }

    /**
     * Gets the size of the largest <code>KeyValue</code>, not the name of the
     * largest <code>KeyValue</code>
     * 
     * @return an integer of the size of the largest <code>KeyValue</code>
     */
    private int getLargestKey()
    {
        int value = 0;

        for (KeyValue kv: getKeyValueObjects())
        {
            value = kv.getValue() > value ? kv.getValue() : value;
        }

        return value;
    }

    /**
     * Gets the <code>Recommendations</code> for this
     * <code>ResearchContext</code>
     *
     * @return a <code>Set</code> of
     *         <code>Item2ResearchContextRecommendations</code>
     */
    public Set<Item2ResearchContextRecommendation> getRecommendations()
    {
        return recommendations;
    }

    /**
     * Adds a <code>Recommendation</code> to this <code>ResearchContext</code>
     *
     * @param i the <code>Recommendation</code> to add
     */
    public void addRecommendation(Item2ResearchContextRecommendation i)
    {
        recommendations.add(i);
    }

    /**
     * Removes a <code>Recommendation</code> from this
     * <code>ResearchContext</code>
     *
     * @param i the <code>Recommendation</code> to remove
     */
    public void removeRecommendation(Item2ResearchContextRecommendation i)
    {
        recommendations.remove(i);
    }

    /**
     * Removes all <code>Recommendations</code> from this
     * <code>ResearchContext</code>
     */
    public void clearRecommendations()
    {
        recommendations = new HashSet<Item2ResearchContextRecommendation>();
    }

    /**
     * Gets the <code>ID</code> of the <code>Essence</code> created when this
     * <code>ResearchContext</code> was created
     *
     * @return the <code>ID</code> of the <code>Essence</code> created when this
     *         <code>ResearchContext</code> was created
     */
    public int getLocalEssenceID() {
        return localEssenceID;
    }

    /**
     * Sets the <code>ID</code> of the <code>Essence</code> created when this
     * <code>ResearchContext</code> is created
     *
     * @param localEssenceID the ID</code> of the Essence</code>
     */
    public void setLocalEssenceID(int localEssenceID) {
        this.localEssenceID = localEssenceID;
    }

    /**
     * Determines if this is the initial <code>ResearchContext</code> for this
     * <code>EPerson</code>
     *
     * @return <code>true</code> if this is the initial
     *         <code>ResearchContext</code>, else <code>false</code>.
     */
    public boolean isInitialResearchContext()
    {
        return ResearchContextDAOFactory.getInstance(context)
                                        .isInitialResearchContext(uuid);
    }

    /**
     * Compares the <code>ID</code> of this <code>ResearchContext</code> with
     * the <code>ID</code> of another <code>ResearchContext</code>. This method
     * can be used to ensure <code>ResearchContext's</code> are
     * always returned in a sorted order.
     *
     * @param researchContext the <code>ResearchContext</code> to be compared
     *
     * @return the value 0 if the argument <code>ResearchContext</code> has the
     * same ID; a value of less than 0 if this <code>ResearchContext</code> has
     * a lower ID; a value of greater than 0 if this
     * <code>ResearchContext</code> has a higher ID.
     */
    public int compareTo(ResearchContext researchContext) {
        int ID = new Integer(this.getID()).compareTo(researchContext.getID());

        if (ID == 0)
        {
            return 0;
        }
        else if (ID < 0)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
