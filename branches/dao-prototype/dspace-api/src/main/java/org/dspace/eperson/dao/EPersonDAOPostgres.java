/*
 * EPersonDAOPostgres.java
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
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson.EPersonMetadataField;
import org.dspace.eperson.EPersonDeletionException;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class EPersonDAOPostgres extends EPersonDAO
{
    public EPersonDAOPostgres(Context context)
    {
        this.context = context;
    }

    @Override
    public EPerson create() throws AuthorizeException
    {
        EPerson eperson = super.create();

        try
        {
            UUID uuid = UUID.randomUUID();

            TableRow row = DatabaseManager.create(context, "eperson");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("eperson_id");

            return super.create(id, uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public EPerson retrieve(int id)
    {
        EPerson eperson = super.retrieve(id);

        if (eperson != null)
        {
            return eperson;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "eperson", id);

            if (row == null)
            {
                log.warn("eperson " + id + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public EPerson retrieve(UUID uuid)
    {
        EPerson eperson = super.retrieve(uuid);

        if (eperson != null)
        {
            return eperson;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "eperson",
                    "uuid", uuid.toString());

            if (row == null)
            {
                log.warn("eperson " + uuid + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private EPerson retrieve(TableRow row)
    {
        int id = row.getIntColumn("eperson_id");
//        EPerson eperson = new EPerson(context, id);
        EPerson eperson = new EPerson(context, row);
//        populateEPersonFromTableRow(eperson, row);

        context.cache(eperson, id);

        return eperson;
    }

    @Override
    public void update(EPerson eperson) throws AuthorizeException
    {
        super.update(eperson);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "eperson", eperson.getID());

            if (row != null)
            {
                update(eperson, row);
            }
            else
            {
                throw new RuntimeException("Didn't find eperson " +
                        eperson.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(EPerson eperson, TableRow row)
        throws AuthorizeException
    {
        try
        {
            populateTableRowFromEPerson(eperson, row);
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME We need link() and unlink() for EPerson <--> Group mapping
     */
    @Override
    public void delete(int id)
        throws AuthorizeException, EPersonDeletionException
    {
        super.delete(id);

        // check for presence of eperson in tables that
        // have constraints on eperson_id
        Vector constraintList = getDeleteConstraints(id);

        // if eperson exists in tables that have constraints
        // on eperson, throw an exception
        if (!constraintList.isEmpty())
        {
            throw new EPersonDeletionException(constraintList);
        }

        try
        {
            // Remove any group memberships first
            DatabaseManager.updateQuery(context,
                    "DELETE FROM EPersonGroup2EPerson WHERE eperson_id = ? ", id);

            // Remove any subscriptions
            DatabaseManager.updateQuery(context,
                    "DELETE FROM subscription WHERE eperson_id = ? ", id);

            // Remove ourself
            DatabaseManager.delete(context, "eperson", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(", sqle);
        }
    }

    @Override
    public List<EPerson> getEPeople(int sortField)
    {
        String s;

        switch (sortField)
        {
            case EPerson.ID:
                s = "eperson_id";
                break;
            case EPerson.EMAIL:
                s = "email";
                break;
            case EPerson.LANGUAGE:
                s = "language";
                break;
            case EPerson.NETID:
                s = "netid";
                break;
            case EPerson.LASTNAME:
            default:
                s = "lastname";
        }

        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT eperson_id FROM eperson ORDER BY " + s);

            List<EPerson> epeople = new ArrayList<EPerson>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("eperson_id");
                epeople.add(retrieve(id));
            }

            return epeople;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(", sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateEPersonFromTableRow(EPerson eperson, TableRow row)
    {
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));

        eperson.setIdentifier(new ObjectIdentifier(uuid));
        for (EPerson.EPersonMetadataField f :
                EPerson.EPersonMetadataField.values())
        {
            eperson.setMetadata(f, row.getStringColumn(f.toString()));
        }
        eperson.setCanLogIn(row.getBooleanColumn("can_log_in"));
        eperson.setRequireCertificate(
                row.getBooleanColumn("require_certificate"));
        eperson.setSelfRegistered(row.getBooleanColumn("self_registered"));
    }

    private void populateTableRowFromEPerson(EPerson eperson, TableRow row)
    {
        for (EPerson.EPersonMetadataField f :
                EPerson.EPersonMetadataField.values())
        {
            row.setColumn(f.toString(), eperson.getMetadata(f));
        }
        row.setColumn("can_log_in", eperson.canLogIn());
        row.setColumn("require_certificate", eperson.getRequireCertificate());
        row.setColumn("self_registered", eperson.getSelfRegistered());
    }

    /**
     * Check for presence of EPerson in tables that have constraints on
     * EPersons. Called by delete() to determine whether the eperson can
     * actually be deleted.
     * 
     * An EPerson cannot be deleted if it exists in the item, workflowitem, or
     * tasklistitem tables.
     * 
     * @return Vector of tables that contain a reference to the eperson.
     */
    private Vector getDeleteConstraints(int id)
    {
        Vector tableList = new Vector();

        try
        {
            // check for eperson in item table
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT item_id from item where submitter_id=? ", id);

            if (tri.hasNext())
            {
                tableList.add("item");
            }

            tri.close();
            
            // check for eperson in workflowitem table
            tri = DatabaseManager.query(context,
                    "SELECT workflow_id from workflowitem where owner=? ",
                    id);

            if (tri.hasNext())
            {
                tableList.add("workflowitem");
            }

            tri.close();
            
            // check for eperson in tasklistitem table
            tri = DatabaseManager.query(context,
                    "SELECT tasklist_id from tasklistitem where eperson_id=? ",
                    id);

            if (tri.hasNext())
            {
                tableList.add("tasklistitem");
            }

            tri.close();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(", sqle);
        }
        
        // the list of tables can be used to construct an error message
        // explaining to the user why the eperson cannot be deleted.
        return tableList;
    }
}
