/*
 * ResearchContextDAOPostgres.java
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
package org.dspace.app.recsys.researchContext.dao.postgres;

import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.app.recsys.researchContext.KeyValue;
import org.dspace.app.recsys.researchContext.Essence;
import org.dspace.app.recsys.bookmark.Bookmark;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAOFactory;
import org.dspace.app.recsys.bookmark.dao.BookmarkDAO;
import org.dspace.app.recsys.researchContext.dao.*;
import org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAOFactory;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.content.Item;

import java.sql.SQLException;
import java.util.*;

/**
 * <code>ResearchContextDAOPostgres</code> defines
 * database-specific code for the manipulation of
 * <code>ResearchContext</code> objects.
 *
 * @author Desmond Elliott
 */
public class ResearchContextDAOPostgres extends ResearchContextDAO
{

    /**
     * Creates and returns a new <code>ResearchContextDAOPostgres</code>
     * object which can be used be clients to interface with the database layer.
     *
     * @param context <code>Context</code> to be used by the
     *                <code>DatabaseManager</code>
     */
    public ResearchContextDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public ResearchContext create(String URI) {

        try
        {
            TableRow row =
                    DatabaseManager.create(context, "quambo_research_context");

            row.setColumn("uuid", UUID.randomUUID().toString());
            DatabaseManager.update(context, row);

            ResearchContext r =
                    new ResearchContext(row.getIntColumn("research_context_id"),
                                        context);

            r.setUUID(UUID.fromString(row.getStringColumn("uuid")));

            // A Research Context needs an essence
            EssenceDAO eDAO = EssenceDAOFactory.getInstance(context);
            Essence e = eDAO.create();
            e.setWeight(1);
            e.setResearchContextID(r.getID());
            e.setUri(URI + row.getStringColumn("uuid"));
            e.setName("Local");
            eDAO.update(e);

            r.addEssence(e);
            r.setLocalEssenceID(e.getID());

            return r;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public ResearchContext retrieve(int id) {

        try
        {
            TableRow row = DatabaseManager.find(context,
                                                "quambo_research_context", id);
            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public ResearchContext retrieve(UUID uuid) {

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
              "quambo_research_context", "uuid", uuid.toString());
            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    private ResearchContext retrieve(TableRow row)
    {
        if (row != null)
        {
            ResearchContext r =
                    new ResearchContext(row.getIntColumn("research_context_id"),
                                        context);

            r.setEperson(EPersonDAOFactory.getInstance(context)
                                          .retrieve(row.getIntColumn("eperson_id")));
            r.setName(row.getStringColumn("name"));
            r.setUUID(UUID.fromString(row.getStringColumn("uuid")));
            r.setLocalEssenceID(row.getIntColumn("local_essence_id"));

            /* Populate the Research Context datastructures using these
               auxilliary methods to extract data from
               database table rows. */

            populateBookmarks(r);
            populateEssences(r);
            populateKeyValues(r);
            populateItemRecommendations(r);

            return r;
        }
        return null;
    }

    /** @inheritDoc */
    public void update(ResearchContext r) {

        try
        {
            int id = r.getID();
            TableRow row = DatabaseManager.find(context,
                                                "quambo_research_context", id);

            if (row != null)
            {
                row.setColumn("research_context_id", id);
                row.setColumn("eperson_id", r.getEperson().getID());
                row.setColumn("name", r.getName());
                row.setColumn("uuid", r.getUUID().toString());
                row.setColumn("local_essence_id", r.getLocalEssenceID());

                /* Use these auxilliary methods to write to the appropriate
                database table rows. */

                populateBookmarkTable(r);
                populateEssenceTable(r);
                populateKeyValueTable(r);
                populateItemRecommendationTable(r);

                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find research context " + id);
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
        ResearchContext rc = retrieve(id);
        if (isInitialResearchContext(rc.getUUID()))
        {
            return;
        }
        try
        {
            // Delete every Item2ResearchContextRecommendation tied to this
            // research context
            Item2ResearchContextRecommendationDAO dao =
                    Item2ResearchContextRecommendationDAOFactory.getInstance(context);
            List<Item2ResearchContextRecommendation> recommendations =
                    dao.getRecommendations(id);
            for (Item2ResearchContextRecommendation i: recommendations)
            {
                dao.delete(i.getID());
            }

            // Delete every Bookmark tied to this research context
            BookmarkDAO bookmarkDAO = BookmarkDAOFactory.getInstance(context);
            List<Bookmark> bookmarkedItems = bookmarkDAO.getBookmarks(id);
            for (Bookmark b: bookmarkedItems)
            {
                bookmarkDAO.delete(b.getID());
            }

            EssenceDAO eDAO = EssenceDAOFactory.getInstance(context);
            List<Essence> essences = eDAO.getEssences(id);
            for (Essence e: essences)
            {
                eDAO.delete(e.getID());
            }

            DatabaseManager.delete(context, "quambo_research_context", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public List<ResearchContext> getAllResearchContexts() {

        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
              "SELECT research_context_id FROM quambo_research_context " +
                "ORDER BY research_context_id");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public List<ResearchContext> getResearchContexts(EPerson eperson) {

        try
        {

            TableRowIterator tri = DatabaseManager.query(context,
              "SELECT research_context_id FROM quambo_research_context WHERE" +
                " eperson_id = ? ORDER BY research_context_id ASC", eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /** @inheritDoc */
    public List<ResearchContext> getResearchContexts(EPerson e,
                                                     boolean isBookmarked,
                                                     Item i)
    {
        if (isBookmarked)
        {
            try
            {
                TableRowIterator tri = DatabaseManager.query(context,
                  "SELECT research_context_id FROM quambo_research_context " +
                  "WHERE research_context_id in (SELECT research_context_id " +
                  "FROM quambo_bookmark WHERE item_id = ?) AND eperson_id = ?" +
                  " ORDER BY research_context_id ASC",
                  i.getID(), e.getID());

                return returnAsList(tri);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
        else
        {
            HashSet<ResearchContext> bookmarked =
              new HashSet<ResearchContext>(getResearchContexts(e, true, i));

            HashSet<ResearchContext> notBookmarked =
              new HashSet<ResearchContext>(getResearchContexts(e));

            HashSet<ResearchContext> bucket = new HashSet<ResearchContext>();

            for (ResearchContext r: notBookmarked)
            {
                for (ResearchContext s: bookmarked)
                {
                    if (r.getID() == s.getID())
                    {
                        bucket.add(r);
                    }
                }
            }

            notBookmarked.removeAll(bucket);
            return new ArrayList<ResearchContext>(notBookmarked);
        }
    }

    /** @inheritDoc */
    public boolean isInitialResearchContext(UUID uuid)
    {
        try
        {
            UUID initialUUID;
            TableRowIterator tri = DatabaseManager.query(context,
                                   "SELECT initial_research_context_uuid " +
                                   "FROM quambo_eperson WHERE eperson_id = ?",
                                   context.getCurrentUser().getID());

            if (tri.hasNext())
            {
                TableRow row = tri.next();
                initialUUID = UUID.fromString(
                  row.getStringColumn("initial_research_context_uuid"));
                tri.close();
            }
            else
            {
                return false;
            }

            return initialUUID.equals(uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

     /**
     * Converts a <code>TableRowIterator</code> of
     * <code>Essence</code>s into a <code>List</code> of
     * <code>Essence</code>s.
     *
     * @param tri a List of <code>TableRow</code>s, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a sorted <code>List</code> of
     *         <code>Essence</code>s
     */
    private List<ResearchContext> returnAsList(TableRowIterator tri)
    {
        List<ResearchContext> r = new ArrayList<ResearchContext>();

        try
        {
            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("research_context_id");
                r.add(retrieve(id));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return r;
    }

    /**
     * Gets the Bookmarked Items associated with this ResearchContext and
     * adds them to this ResearchContext's Set of Bookmarks
     *
     * @param researchContext the ResearchContext to retrieve Bookmarks for
     */
    private void populateBookmarks(ResearchContext researchContext)
    {
        List<Bookmark> bookmarks = BookmarkDAOFactory.getInstance(context).
                                   getBookmarks(researchContext.getID());

        for (Bookmark b: bookmarks)
        {
            researchContext.addBookmark(b);
        }
    }

    /**
     * Write the in-memory state of the Bookmark Set back to
     * the <code>quambo_bookmark</code> table
     *
     * @param researchContext the ResearchContext which needs its Bookmarked
     *                        Items written to the database
     */
    private void populateBookmarkTable(ResearchContext researchContext)
    {
        BookmarkDAO dao = BookmarkDAOFactory.getInstance(context);

        for (Bookmark bookmark : researchContext.getBookmarks())
        {
            dao.update(bookmark);
        }
    }

    /**
     * Retrieve rows from the <code>quambo_item2item_recommendation</code> table
     * and populates the ResearchContext's recommendation HashSet.
     *
     * @param researchContext The Research Context to populate
     */
    private void populateItemRecommendations(ResearchContext researchContext)
    {
        List<Item2ResearchContextRecommendation> recommendations =
          Item2ResearchContextRecommendationDAOFactory.getInstance(context).
            getRecommendations(researchContext.getID());

        for (Item2ResearchContextRecommendation i: recommendations)
        {
            researchContext.addRecommendation(i);
        }
    }

    /**
     * Write the in-memory state of the Recommendation HashSet
     * back to the quambo_item2item_recomendation table.
     *
     * @param r the ResearchContext which needs its Recommendations written to
     *          the database
     */
    private void populateItemRecommendationTable(ResearchContext r)
    {
        for (Item2ResearchContextRecommendation i : r.getRecommendations())
        {
            Item2ResearchContextRecommendationDAOFactory.getInstance(context)
                                                        .update(i);
        }
    }      

    /**
     * Retrieve rows from the <code>quambo_key_value</code> table
     * and populates the ResearchContext's KeyValue Set.
     *
     * @param r the Research Context to populate
     */
    private void populateKeyValues(ResearchContext r)
    {
        List<Essence> essences = EssenceDAOFactory.getInstance(context)
                                                  .getEssences(r.getID());

        for (Essence e: essences)
        {
            List<KeyValue> kv = KeyValueDAOFactory.getInstance(context)
                                                  .getKeyValues(e.getID());

            for (KeyValue k: kv)
            {
                r.getKeyValueObjects().add(k);
            }
        }   
    }

    /**
     * Write the in-memory state of the KeyValue Set
     * back to the quambo_key_value table.
     *
     * @param r the ResearchContext which needs its KeyValues written to
     *          the database
     */
    private void populateKeyValueTable(ResearchContext r)
    {
        try
        {
            Iterator<KeyValue> i = r.getKeyValueObjects().iterator();
            KeyValueDAO kvDAO = KeyValueDAOFactory.getInstance(context);

            while (i.hasNext())
            {
                kvDAO.update(i.next());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve rows from the <code>quambo_essence</code> table
     * and populates the ResearchContext's Essence List.
     *
     * @param r the Research Context to populate
     */
    private void populateEssences(ResearchContext r)
    {
        try
        {
            List<Essence> essenceList = EssenceDAOFactory.getInstance(context).
                                        getEssences(r.getID());

            Set<Essence> essences = r.getEssences();

            for (Essence e: essenceList)
            {
                essences.add(e);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the in-memory state of the Essence List
     * back to the quambo_essence table.
     *
     * @param r the ResearchContext which needs its Essences written to
     *          the database
     */
    private void populateEssenceTable(ResearchContext r)
    {
        try
        {
            Set<Essence> essences = r.getEssences();
            EssenceDAO eDAO = EssenceDAOFactory.getInstance(context);

            for (Essence e: essences)
            {
                eDAO.update(e);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
