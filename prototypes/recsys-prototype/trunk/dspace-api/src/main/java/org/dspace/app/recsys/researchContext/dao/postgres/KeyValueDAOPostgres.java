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

import org.dspace.app.recsys.researchContext.dao.KeyValueDAO;
import org.dspace.app.recsys.researchContext.KeyValue;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>KeyValueDAOPostgres</code> defines
 * database-specific code for the manipulation of
 * <code>KeyValue</code>s.
 *
 * @author Desmond Elliott
 */
public class KeyValueDAOPostgres extends KeyValueDAO
{

    /**
     * Creates and returns a new <code>KeyValueDAOPostgres</code>
     * object which can be used be clients to interface with the database layer.
     *
     * @param context <code>Context</code> to be used by the
     *                <code>DatabaseManager</code>
     */
    public KeyValueDAOPostgres(Context context)
    {
        this.context = context;
    }

    /** @inheritDoc */
    public KeyValue create() {
        try
        {
            TableRow row = DatabaseManager.create(context, "quambo_key_value");
            DatabaseManager.update(context, row);
            return new KeyValue(row.getIntColumn("key_value_id"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public KeyValue retrieve(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_key_value",
                                                id);

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public KeyValue retrieve(UUID uuid) {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
              "quambo_key_value", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve a <code>KeyValue</code> from the database given a
     * <code>TableRow</code>
     *
     * @param row <code>TableRow</code> retrieved from the database
     * @return A <code>KeyValue</code> object populated with values from the
     *         <code>TableRow</code> object
     */
    private KeyValue retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }
        try
        {
            KeyValue kvp = new KeyValue(row.getIntColumn("key_value_id"));

            kvp.setKey(row.getStringColumn("key"));
            kvp.setValue(row.getIntColumn("value"));
            kvp.setType(row.getStringColumn("type"));
            kvp.setEssenceID(row.getIntColumn("essence_id"));
            kvp.setResearchContextID(row.getIntColumn("research_context_id"));

            return kvp;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public void update(KeyValue kvp) {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_key_value",
              kvp.getKeyValueID());

            if (row == null)
            {
                throw new RuntimeException("recsys:key_value:does_not_exist");
            }

            row.setColumn("key", kvp.getKey());
            row.setColumn("value", kvp.getValue());
            row.setColumn("essence_id", kvp.getEssenceID());
            row.setColumn("type", kvp.getType());
            row.setColumn("research_context_id", kvp.getResearchContextID());

            DatabaseManager.update(context, row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public void delete(int id) {
        try
        {
            TableRow row = DatabaseManager.find(context, "quambo_key_value",
              id);

            if (row == null)
            {
                throw new RuntimeException("recsys:key_value:does_not_exist");
            }

            DatabaseManager.delete(context, row);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** @inheritDoc */
    public List<KeyValue> getKeyValues(int essenceID) {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
              "SELECT key_value_id from quambo_key_value where essence_id = " +
                "?", essenceID);

            List<TableRow> tableRows = tri.toList();

            tri.close();

            return returnAsList(tableRows);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a List of <code>TableRow</code>s of
     * <code>KeyValue</code>s into a <code>List</code> of
     * <code>KeyValue</code>s.
     *
     * @param tr a List of <code>TableRow</code>s, most likely returned from a
     *            prior <code>SQL</code> query.
     * @return a <code>List</code> of <code>KeyValue</code>s
     */
    private List<KeyValue> returnAsList(List<TableRow> tr)
    {
        try
        {
            List<KeyValue> keyValues = new ArrayList<KeyValue>();

            for (TableRow row: tr)
            {
                keyValues.add(retrieve(row.getIntColumn("key_value_id")));
            }
            
            return keyValues;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}