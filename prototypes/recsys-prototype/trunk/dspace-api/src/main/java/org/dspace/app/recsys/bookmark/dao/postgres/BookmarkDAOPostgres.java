/*
 * BookmarkDAOPostgres.java
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
package org.dspace.app.recsys.bookmark.dao.postgres;

import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAO;
import org.dspace.app.recsys.researchContext.ResearchContext;

import org.dspace.core.Context;

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;

import java.util.*;

/**
 * <code>BookmarkDAOPostgres</code> defines database-specific code
 * for the manipulation of <code>Bookmark</code>s.
 *
 * @author Desmond Elliott
 */
public class BookmarkDAOPostgres extends BookmarkDAO
{

    /**
     * Creates a new <code>Bookmark</code> Data Access Object for interfacing
     * with a PostgreSQL database.
     *
     * @param context DSpace <code>Context</code> to allow for
     *                <code>DatabaseManager</code> methods.
     */
    public BookmarkDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public Bookmark create()
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "quambo_bookmark");
            row.setColumn("created", new Date().toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("bookmark_id");

            Bookmark b = new Bookmark(id, context);
            b.setCreated(row.getStringColumn("created"));
            return b;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public Bookmark retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_bookmark", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public Bookmark retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "quambo_bookmark", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Retrieves a <code>Bookmark</code> object from the database based on the
     * argument <code>row</code>. If the the <code>TableRow</code> does not
     * exist in the database, this method returns <code>null</code>.
     *
     * @param row the <code>TableRow</code> of the <code>Bookmark</code> to
     *             retrieve from the database
     * @return a <code>Bookmark</code> object if it exists in the database,
     *         otherwise returns <code>null</code>.
     */
    private Bookmark retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }
        else
        {
            Bookmark b = new Bookmark(row.getIntColumn("bookmark_id"), context);
            b.setItemID(row.getIntColumn("item_id"));
            b.setResearchContextID(row.getIntColumn("research_context_id"));
            b.setCreated(row.getStringColumn("created"));

            return b;
        }
    }

    /** @inheritDoc */
    public void update(Bookmark bookmark)
    {
        try
        {
            int id = bookmark.getID();
            TableRow row = DatabaseManager.find(context, "quambo_bookmark", id);

            if (row != null)
            {
                row.setColumn("item_id", bookmark.getItemID());
                row.setColumn("research_context_id",
                  bookmark.getResearchContextID());

                row.setColumn("created", bookmark.getCreated().toString());
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new
                  RuntimeException("recsys:bookmark_not_found:id=" + id);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public void delete(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_bookmark", id);
            if (row != null)
            {
                DatabaseManager.delete(context, "quambo_bookmark", id);
            }
            else
            {
                throw new
                  RuntimeException("recsys:bookmark_not_deleted:id=" + id);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public boolean isBookmarked(int itemID, ResearchContext researchContext)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bookmark_id FROM quambo_bookmark " +
                    "WHERE research_context_id = ? AND item_id = ?",
                    researchContext.getID(), itemID);

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
    public List<Bookmark> getAllBookmarks()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bookmark_id FROM quambo_bookmark " +
                    "ORDER BY bookmark_id");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public List<Bookmark> getBookmarks(int researchContextID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                "SELECT bookmark_id FROM quambo_bookmark " +
                "WHERE research_context_id = ?", researchContextID);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Converts a <code>TableRowIterator</code> of <code>Bookmarks</code> into a
     * <code>List</code> of <code>Bookmarks</code>.
     *
     * @param tri a <code>TableRowIterator</code>, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a sorted <code>List</code> of <code>Bookmarks</code>
     */
    private List<Bookmark> returnAsList(TableRowIterator tri)
    {
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();

        try
        {
            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("bookmark_id");
                bookmarks.add(retrieve(id));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        Collections.sort(bookmarks);

        return bookmarks;
    }
}
