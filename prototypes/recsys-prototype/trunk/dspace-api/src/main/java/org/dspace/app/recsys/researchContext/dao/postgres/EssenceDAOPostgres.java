/*
 * EssenceDAOPostgres.java
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

import org.dspace.app.recsys.researchContext.dao.EssenceDAO;
import org.dspace.app.recsys.researchContext.dao.KeyValueDAO;
import org.dspace.app.recsys.researchContext.dao.KeyValueDAOFactory;
import org.dspace.app.recsys.researchContext.Essence;
import org.dspace.app.recsys.researchContext.KeyValue;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.core.Context;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>EssenceDAOPostgres</code> defines
 * database-specific code for the manipulation of
 * <code>Essences</code>s.
 *
 * @author Desmond Elliott
 */
public class EssenceDAOPostgres extends EssenceDAO
{

    /**
     * Creates and returns a new <code>EssenceDAOPostgres</code>
     * object which can be used be clients to interface with the database layer.
     *
     * @param context <code>Context</code> to be used by the
     *                <code>DatabaseManager</code>
     */
    public EssenceDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public Essence create() {
        try
        {
            TableRow row = DatabaseManager.create(context, "quambo_essence");
            DatabaseManager.update(context, row);
            return new Essence(row.getIntColumn("essence_id"), context);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public Essence retrieve(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_essence", id);

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public Essence retrieve(UUID uuid) {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
              "quambo_essence", "uuid", uuid);

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve an <code>Essence</code> from the database given a
     * <code>TableRow</code>
     *
     * @param row <code>TableRow</code> retrieved from the database
     * @return An <code>Essence</code> object populated with values from the 
     *         <code>TableRow</code> object
     */
    private Essence retrieve(TableRow row)
    {
        try
        {
            if (row == null)
            {
                return null;
            }

            Essence e = new Essence(row.getIntColumn("essence_id"), context);
            e.setUri(row.getStringColumn("uri"));
            e.setWeight(row.getIntColumn("weight"));
            e.setResearchContextID(row.getIntColumn("research_context_id"));
            e.setName(row.getStringColumn("name"));

            return e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public void update(Essence e) {
        try
        {
            TableRow row = DatabaseManager.find(context,
                                                "quambo_essence",
                                                e.getID());

            if (row == null)
            {
                throw new RuntimeException("recsys:essence:does_not_exist");
            }

            row.setColumn("uri", e.getUri());
            row.setColumn("weight", e.getWeight());
            row.setColumn("research_context_id", e.getResearchContextID());
            row.setColumn("name", e.getName());

            DatabaseManager.update(context, row);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /** @inheritDoc */
    public void delete(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context,
                                                "quambo_essence",
                                                id);

            KeyValueDAO kvDAO = KeyValueDAOFactory.getInstance(context);
            List<KeyValue> keyValues = kvDAO.getKeyValues(id);

            for(KeyValue kv: keyValues)
            {
                kvDAO.delete(kv.getKeyValueID());
            }

            DatabaseManager.delete(context, row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public List<Essence> getEssences(int researchContextID) {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
              "SELECT essence_id FROM quambo_essence WHERE " +
                "research_context_id = ?", researchContextID);

            List<TableRow> tableRows = tri.toList();

            tri.close();

            return returnAsList(tableRows);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public boolean isLocalEssence(int essenceID, int researchContextID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context, "SELECT " +
              "local_essence_id FROM quambo_research_context WHERE " +
              "research_context_id = ?", researchContextID);

             return tri.toList().get(0)
                                .getIntColumn("local_essence_id") == essenceID;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a <code>TableRowIterator</code> of
     * <code>Essence</code>s into a <code>List</code> of
     * <code>Essence</code>s.
     *
     * @param tr a List of <code>TableRow</code>s, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a sorted <code>List</code> of
     *         <code>Essence</code>s
     */
    private List<Essence> returnAsList(List<TableRow> tr)
    {
        try
        {
            List<Essence> essences = new ArrayList<Essence>();

            for (TableRow row: tr)
            {
                essences.add(retrieve(row.getIntColumn("essence_id")));
            }

            return essences;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }    
}
