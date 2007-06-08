/*
 * PersistentIdentifierDAO.java
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
package org.dspace.content.uri.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.content.uri.Handle;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;

/**
 * @author James Rutherford
 */
public class PersistentIdentifierDAOPostgres extends PersistentIdentifierDAO
{
    public PersistentIdentifierDAOPostgres(Context context)
    {
        this.context = context;
    }

    /**
     * Creates a persistent identifier of the defualt type.
     */
    public PersistentIdentifier create(DSpaceObject dso)
    {
        // The default is the first entry in the list.
        return create(dso, "", pids[0].getType());
    }

    public PersistentIdentifier create(DSpaceObject dso, String canonicalForm)
    {
        Object[] bits = parseCanonicalForm(canonicalForm);
        PersistentIdentifier.Type type = (PersistentIdentifier.Type) bits[0];
        String value = (String) bits[1];

        return create(dso, value, type);
    }

    public PersistentIdentifier create(DSpaceObject dso, String value,
            PersistentIdentifier.Type type)
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "handle");

            switch (type)
            {
                case HANDLE:
                    if (value.equals(""))
                    {
                        String prefix =
                            ConfigurationManager.getProperty("handle.prefix");
                        value = prefix + "/" + row.getIntColumn("handle_id");
                    }
                    break;
                default:
                    throw new RuntimeException(":(");
            }

            PersistentIdentifier identifier = getInstance(dso, type, value);

            row.setColumn("handle", value);
            row.setColumn("resource_type_id", dso.getType());
            row.setColumn("resource_id", dso.getID());
            row.setColumn("type_id", identifier.getTypeID());
            DatabaseManager.update(context, row);

            if (log.isDebugEnabled())
            {
                log.debug("Created new persistent identifier for "
                        + Constants.typeText[dso.getType()] + " " + value);
            }

            dso.addPersistentIdentifier(identifier);

            return identifier;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public PersistentIdentifier retrieve(String canonicalForm)
    {
        Object[] bits = parseCanonicalForm(canonicalForm);
        PersistentIdentifier.Type type = (PersistentIdentifier.Type) bits[0];
        String value = (String) bits[1];

        DSpaceObject dso = null;

        int resourceID = -1;
        int resourceTypeID = -1;

        // If the type is NULL, then this is just an internal identifier
        if (type.equals(PersistentIdentifier.Type.NULL))
        {
            resourceTypeID = Integer.parseInt(value.substring(0,
                        value.indexOf("/")));
            resourceID =
                Integer.parseInt(value.substring(value.indexOf("/") + 1));

            ObjectIdentifier oi = new ObjectIdentifier(resourceID, resourceTypeID);
            dso = oi.getObject(context);

            return getInstance(dso, type, value);
        }

        if (type == null)
        {
            throw new RuntimeException(canonicalForm + " not a supported type");
        }

        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "persistentidentifier",
                    "SELECT resource_id, resource_type_id " +
                    "FROM persistentidentifier " +
                    "WHERE value = ? AND type_id = ?",
                    value, type.getID());

            List<TableRow> list = tri.toList();
            if (list.size() == 1)
            {
                TableRow row = list.get(0);
                resourceID = row.getIntColumn("resource_id");
                resourceTypeID = row.getIntColumn("resource_type_id");
            }
            else
            {
                throw new RuntimeException(canonicalForm + " not found");
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        ObjectIdentifier oi = new ObjectIdentifier(resourceID, resourceTypeID);
        dso = oi.getObject(context);

        return getInstance(dso, type, value);
    }

    public List<PersistentIdentifier> getPersistentIdentifiers(DSpaceObject dso)
    {
        try
        {
            List<PersistentIdentifier> list =
                new ArrayList<PersistentIdentifier>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "persistentidentifier",
                    "SELECT type_id, value FROM persistentidentifier " +
                    "WHERE resource_id = ? " +
                    "AND resource_type_id = ?",
                    dso.getID(), dso.getType());

            String value = null;

            for (TableRow row : tri.toList())
            {
                value = row.getStringColumn("value");
                int id = row.getIntColumn("type_id");

                // FIXME: Maybe throw an error if the value stored in the db
                // isn't in the enum?
                for (PersistentIdentifier pid : pids)
                {
                    if (pid.getTypeID() == id)
                    {
                        list.add(getInstance(dso, pid.getType(), value));
                        break;
                    }
                }
            }

            return list;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<PersistentIdentifier>
        getPersistentIdentifiers(PersistentIdentifier.Type type)
    {
        return getPersistentIdentifiers(type, "");
    }

    public List<PersistentIdentifier>
        getPersistentIdentifiers(PersistentIdentifier.Type type, String prefix)
    {
        try
        {
            List<PersistentIdentifier> list =
                new ArrayList<PersistentIdentifier>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "persistentidentifier",
                    "SELECT resource_id, resource_type_id, value " +
                    "FROM persistentidentifier " +
                    "WHERE type_id = ? " +
                    "AND value LIKE ?",
                    type.getID(), prefix + "%");

            for (TableRow row : tri.toList())
            {
                String value = row.getStringColumn("value");
                int resourceID = row.getIntColumn("resource_id");
                int resourceTypeID = row.getIntColumn("resource_type_id");

                ObjectIdentifier oi = new ObjectIdentifier(resourceID, resourceTypeID);
                DSpaceObject dso = oi.getObject(context);

                for (PersistentIdentifier pid : pids)
                {
                    if (type.equals(pid.getType()))
                    {
                        list.add(getInstance(dso, pid.getType(), value));
                        break;
                    }
                }
            }

            return list;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
