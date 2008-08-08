/*
 * Bookmark.java
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
package org.dspace.app.recsys.bookmark;

import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A <code>Bookmark</code> is the object created when a user bookmarks an
 * <code>Item</code> into a <code>ResearchContext</code>. The act of bookmarking
 * an <code>Item</code> extracts metadata from the <code>Item</code> and adds it
 * to the local <code>Essence</code> of a <code>ResearchContext</code>.
 *
 * <p>
 * <code>Item</code>s and <code>ResearchContext</code>s are not stored in memory
 * with a <code>Bookmark</code>, their <code>ID</code>s are stored in memory and
 * the actual objects are only retrieved when needed.
 * </p>
 *
 * @author Desmond Elliott
 */
public class Bookmark implements Comparable<Bookmark>
{
    /** ID of the <code>Bookmark</code> */
    private int id;

    /** Date this <code>Bookmark</code> was created.
     * Used to the Atom feed of Bookmarks */
    private Date created;

    /** DSpace <code>Context</code> which is used to retrieve the encapsulated
     * <code>Item</code> or <code>ResearchContext</code> */
    private Context context;

    /** <code>ID</code> of the bookmarked <code>Item</code> */
    private int itemID;

    /** <code>ID</code> of the <code>ResearchContext</code> this Bookmark
     * belongs to */
    private int researchContextID;

    /**
     * Creates a new <code>Bookmark</code> object which will represent an
     * <code>Item</code> bookmarked into a particular
     * <code>ResearchContext</code>.
     *
     * @param id the database ID to be associated with this
     *           <code>Bookmark</code> object.
     * @param context a <code>DSpace Context</code> to allow database access
     */
    public Bookmark(int id, Context context)
    {
        this.id = id;
        this.context = context;
    }

    /**
     * Gets the ID of this <code>Bookmark</code> object
     *
     * @return the ID of this <code>Bookmark</code> object
     */
    public int getID()
    {
        return id;
    }

    /**
     * Sets the ID of this <code>Bookmark</code> object
     *
     * @param id the ID of this <code>Bookmark</code> object
     */
    public void setID(int id)
    {
        this.id = id;
    }

    /**
     * Gets the <code>Item</code> referred to by this <code>Bookmark</code>
     *
     * @return the <code>Item</code> referred to by this <code>Bookmark</code>
     */
    public Item getItem()
    {
        return ItemDAOFactory.getInstance(context).retrieve(getItemID());
    }

    /**
     * Gets the <code>ID</code> of the <code>Item</code> referred to by this
     * <code>Bookmark</code>
     *
     * @return the <code>ID</code> of the <code>Item</code> referred to by
     *         this <code>Bookmark</code>
     */
    public int getItemID()
    {
        return itemID;
    }

    /**
     * Sets the <code>ID</code> of the <code>Item</code> referred to by this
     * <code>Bookmark</code>
     *
     * @param itemID the <code>ID</code> of the <code>Item</code> this Bookmark
     *               will refer to. Not checked for validity.
     */
    public void setItemID(int itemID)
    {
        this.itemID = itemID;
    }

    /**
     * Gets the <code>ResearchContext</code> this <code>Bookmark</code>
     * is bookmarked into
     *
     * @return the <code>ResearchContext</code> this <code>Bookmark</code>
     *         is bookmarked into
     */
    public ResearchContext getResearchContext()
    {
        return ResearchContextDAOFactory.getInstance(context)
          .retrieve(getResearchContextID());
    }

    /**
     * Gets the <code>UUID</code> of the <code>ResearchContext</code> this
     * <code>Bookmark</code> is bookmarked into
     *
     * @return the <code>UUID</code> of the <code>ResearchContext</code> this
     *         <code>Bookmark</code> is bookmarked into
     */
    public int getResearchContextID()
    {
        return researchContextID;
    }

    /**
     * Sets the <code>ID</code> of the <code>ResearchContext</code> this
     * <code>Bookmark</code> is bookmarked into
     *
     * @param researchContextID the <code>ID</code> of the
     *                            <code>ResearchContext</code> this
     *                            <code>Bookmark</code> is bookmarked into
     */
    public void setResearchContextID(int researchContextID)
    {
        this.researchContextID = researchContextID;
    }

    /**
     * Gets the <code>Date</code> this <code>Bookmark</code> was created
     *
     * @return the <code>Date</code> this <code>Bookmark</code> was created
     */
    public Date getCreated()
    {
        return created;
    }

    /**
     * Sets the <code>Date</code> this <code>Bookmark</code> was created
     *
     * @param date the <code>Date</code> this <code>Bookmark</code> was created.
     *             The expected format of this <code>String</code> is:
     *             <code>EEE MMM d HH:mm:ss z yyyy</code>.
     *
     * @see SimpleDateFormat
     */
    public void setCreated(String date)
    {
        SimpleDateFormat sdf =
          new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");

        try
        {
            created = sdf.parse(date);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Compares two <code>Bookmark</code> objects by their <code>created</code>
     * <code>Date</code>.
     *
     * @param bookmark the <code>Bookmark</code> to be compared
     *
     * @return the value 0 if the argument <code>Bookmark</code> was created at
     * the same time; a value of less than 0 if this <code>Bookmark</code> was
     * created before; a value of greater than 0 if this <code>Bookmark</code>
     * was created after the <code>Bookmark</code> argument.
     */
    public int compareTo(Bookmark bookmark)
    {
        int created = getCreated().compareTo(bookmark.getCreated());

        if (created == 0)
        {
            return 0;
        }
        else if (created < 0)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}