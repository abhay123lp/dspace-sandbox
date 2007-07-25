/*
 * HistoryDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.history.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.history.History;
import org.dspace.history.HistoryManager;
import org.dspace.history.HistoryState;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * @author James Rutherford
 */
public class HistoryDAOPostgres extends HistoryDAO
{
    public HistoryDAOPostgres(Context context)
    {
        this.context = context;
    }

    public History create()
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "history");

            return new History(context, row.getIntColumn("history_id"));
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void update(History history)
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "history", history.getID());
            row.setColumn("creation_date", history.getDateCreated());
            row.setColumn("checksum", history.getChecksum());
            log.info("HistoryDAOPostgres.update(): " + history.toString());
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public History retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "history", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public History retrieve(String checksum)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "history",
                    "checksum", checksum);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private History retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        History history =
            new History(context, row.getIntColumn("history_id"));
        history.setDateCreated(row.getDateColumn("creation_date"));
        history.setChecksum(row.getStringColumn("checksum"));

        return history;
    }

    public HistoryState createState()
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "historystate");

            return new HistoryState(context, row.getIntColumn("history_state_id"));
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public void updateState(HistoryState state)
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "historystate", state.getID());
            row.setColumn("object_id", state.getObjectID());
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public String retrievePreviousStateID(String objectID)
    {
        Connection connection = null;
        PreparedStatement statement = null;

        try
        {
            try
            {
                String sql = "SELECT MAX(history_state_id) " +
                    "FROM HistoryState WHERE object_id = ?";

                connection = DatabaseManager.getConnection();
                statement = connection.prepareStatement(sql);
                statement.setString(1, objectID);

                ResultSet results = statement.executeQuery();

                return results.next() ? results.getString(1) : null;
            }
            finally
            {
                if (statement != null)
                {
                    statement.close();
                }

                if (connection != null)
                {
                    connection.close();
                }
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
