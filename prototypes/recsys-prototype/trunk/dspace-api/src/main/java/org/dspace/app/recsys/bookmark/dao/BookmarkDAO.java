/*
 * BookmarkDAO.java
 *
 * Version: $Revision: $
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
package org.dspace.app.recsys.bookmark.dao;

import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.researchContext.dao.*;
import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * <code>BookmarkDAO</code> handles database access to create, retrieve,
 * update, and delete <code>Bookmark</code>s. Any non database-specific code
 * must be declared in this class instead of <code>BookmarkDAOPostgres</code>.
 *
 * @author Desmond Elliott
 */
public abstract class BookmarkDAO
{
    /** Logs data to <code>dspace.log</code> */
    protected Logger log = Logger.getLogger(BookmarkDAO.class);

    /** <code>DSpace Context</code> which is used for database access */
    protected Context context;

    /**
     * Creates and returns a new <code>Boomark</code> object. A new
     * <code>Bookmark</code> object is created by creating a new table row
     * in the <code>quambo_bookmark</code> table.
     *
     * @return A new <code>Bookmark</code> object with <code>ID</code> equal to
     *         the <code>ID</code> of the database table row created.
     */
    public abstract Bookmark create();

    /**
     * Retrieves a <code>Bookmark</code> object from the database based on the
     * argument <code>id</code>. If the the <code>ID</code> does not exist in
     * the database, this method returns <code>null</code>.
     *
     * @param id the <code>ID</code> of the <code>Bookmark</code> to retrieve
     *           from the database
     * @return a <code>Bookmark</code> object if it exists in the database,
     *         otherwise returns <code>null</code>.
     */
    public abstract Bookmark retrieve(int id);

    /**
     * Retrieves a <code>Bookmark</code> object from the database based on the
     * argument <code>uuid</code>. If the the <code>UUID</code> does not exist
     * in the database, this method returns <code>null</code>.
     *
     * @param uuid the <code>UUID</code> of the <code>Bookmark</code> to
     *             retrieve from the database
     * @return a <code>Bookmark</code> object if it exists in the database,
     *         otherwise returns <code>null</code>.
     */
    public abstract Bookmark retrieve(UUID uuid);

    /**
     * Updates the database row that represents the <code>Bookmark</code> object
     * based on the <code>ID</code> of the argument object. If the the
     * <code>ID</code> does not exist in the database, or any of the fields in
     * the argument object are null, this method will not succeed.
     *
     * @param bookmark the <code>Bookmark</code> to write to the database
     *
     */
    public abstract void update(Bookmark bookmark);

    /**
     * Deletes the database row of the <code>Bookmark</code> object represented
     * by the <code>id</code> argument. If a database row in the
     * <code>quambo_bookmark</code> table does not exist with the argument
     * <code>id</code>, an Exception is thrown.
     *
     * @param id the <code>ID</code> of the <code>Bookmark</code> object
     */
    public abstract void delete(int id);

    /**
     * Determines in the <code>Item</code> with the ID given as an argument is
     * bookmarked into the <code>ResearchContext</code> argument. An Exception
     * is thrown if either the <code>Item</code> or
     * <code>ResearchContext</code> do not exist.
     *
     * @param itemID the ID of the <code>Item</code>
     * @param researchContext the <code>ResearchContext</code>
     * @return true if <code>item</item> is bookmarked in
     *         <code>researchContext</code>
     */
    public abstract boolean isBookmarked(int itemID, ResearchContext researchContext);

    /**
     * Gets all <code>Bookmark</code> objects stored, regardless of user.
     *
     * @return A <code>List</code> of all the <code>Bookmark</code>(s)
     *         stored in the database.
     */
    public abstract List<Bookmark> getAllBookmarks();

    /**
     * Gets all <code>Bookmark</code> objects given a
     * <code>ResearchContext</code> ID.
     *
     * @param researchContextID the <code>ResearchContext</code> to retrieve
     *                          <code>Bookmark</code> objects for
     * @return A <code>List</code> of all the <code>Bookmark</code>(s) stored in
     *         the database.
     */
    public abstract List<Bookmark> getBookmarks(int researchContextID);

    /**
     * Creates a new <code>Bookmark</code> object and associates the
     * <code>Item</code> and <code>ResearchContext</code> with the
     * <code>Bookmark</code> object.
     *
     * Adds metadata from the <code>Item</code> to the
     * <code>ResearchContext</code> <code>Essence</code>
     *
     * @param item            the <code>Item</code> to be encapsulated.
     * @param researchContext the <code>Research Context</code> to bookmark this
     *                        <code>Item</code> into
     */
    public void addBookmark(Item item, ResearchContext researchContext)
    {
        if (!isBookmarked(item.getID(), researchContext))
        {
            Bookmark b = create();
            b.setItemID(item.getID());
            b.setResearchContextID(researchContext.getID());
            update(b);

            log.info(LogManager.getHeader(context,
              "recsys:bookmark_created:id=" + b.getID(),
              ":item_id=" + item.getID() + ":research_context_id=" +
              researchContext.getID()));

            ResearchContextDAOFactory.getInstance(context).
                    addMetadata(item, researchContext);
        }
    }

    /**
     * Removes the <code>Bookmark</code> from a
     * <code>ResearchContext</code> based on the <code>Item</code> and
     * <code>ResearchContext</code> arguments.
     *
     * Removes metadata from the <code>ResearchContext</code>
     * <code>Essence</code> which was added by the <code>Item</code>
     *
     * @param item            the <code>Item</code> that is bookmarked.
     * @param researchContext the <code>Research Context</code> this
     *                        <code>Item</code> is bookmarked into
     */
    public void removeBookmark(Item item, ResearchContext researchContext)
    {
        for (Bookmark bookmark: getBookmarks(researchContext.getID()))
        {
            if (item.getID() == bookmark.getItemID())
            {
                researchContext.removeBookmark(bookmark);

                delete(bookmark.getID());

                log.info(LogManager.getHeader(context,
                  "recsys:bookmark_removed:id=" + bookmark.getID(),
                  ":resarch_context_id=" + researchContext.getID()));

                ResearchContextDAOFactory.getInstance(context).
                        removeMetadata(item, researchContext);

                break;
            }
        }
    }
}