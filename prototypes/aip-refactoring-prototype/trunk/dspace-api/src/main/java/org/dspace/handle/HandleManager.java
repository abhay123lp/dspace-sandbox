/*
 * HandleManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.handle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Interface to the <a href="http://www.handle.net" target=_new>CNRI Handle
 * System </a>.
 * 
 * <p>
 * Currently, this class simply maps handles to local facilities; handles which
 * are owned by other sites (including other DSpaces) are treated as
 * non-existent.
 * </p>
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class HandleManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HandleManager.class);

    /** Private Constructor */
    private HandleManager()
    {
    }

    /**
     * Return the local URL for handle, or null if handle cannot be found.
     * 
     * The returned URL is a (non-handle-based) location where a dissemination
     * of the object referred to by handle can be obtained.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle
     * @return The local URL
     * @exception SQLException
     *                If a database error occurs
     */
    public static String resolveToURL(Context context, String handle)
            throws SQLException
    {
        TableRow dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null)
        {
            return null;
        }

        String url = ConfigurationManager.getProperty("dspace.url")
                + "/handle/" + handle;

        if (log.isDebugEnabled())
        {
            log.debug("Resolved " + handle + " to " + url);
        }

        return url;
    }

    /**
     * Transforms handle into the canonical form <em>hdl:handle</em>.
     * 
     * No attempt is made to verify that handle is in fact valid.
     * 
     * @param handle
     *            The handle
     * @return The canonical form
     */
    public static String getCanonicalForm(String handle)
    {
        //        return "hdl:" + handle;
        return "http://hdl.handle.net/" + handle;
    }

    /**
     * Create a new Handle entry by finding the highest number in an
     * existing Handle suffix and adding one.
     * Since the handle table may include Handles from restored or
     * ingested AIPs there is no relationship between the primary key
     * and the handle suffix, as used to be assumed in the old
     * implementation of this function.  Now we have to grovel over
     * the whole table to find the highest existing Handle suffix.
     * Fortunately this is fairly efficient (~50 mSec on a typical server for
     * 10,000 Handles) and it only gets called once in the lifecycle of
     * each newly submitted object.
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            The DSpaceObject to create a handle for
     * @return The newly created handle
     * @exception SQLException
     *                If a database error occurs
     */
    public static String createHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        // get prefix without trailing '/' if any..
        String handlePrefix = ConfigurationManager.getProperty("handle.prefix");
        if (handlePrefix.endsWith("/"))
            handlePrefix = handlePrefix.substring(0, handlePrefix.length()-1);

        /*
         * XXX FIXME: Note that this contains a race condition; while we
         * run the query and compute a new Handle suffix, another process
         * might be doing doing the same thing and reaching the same
         * conclusion.  The second transaction will fail because the handle
         * column has a unique constraint, so it will not corrupt the table.
         * The ideal solution would be to wrap a lock like:
         *   LOCK TABLE handle IN ACCESS EXCLUSIVE MODE;
         * around the SELECT and subsequent INSERT, but this deadlocks because
         * the existing Context's transaction already a read-lock on the
         * Handle table, with no way to release it.  I don't believe it's
         * worth making the "ACCESS EXCLUSIVE" lock in the existing
         * context since (a) there is no telling how long that transaction
         * may stay open or even if it gets closed at all; (b) it blocks
         * ALL other threads that need ANY access to the "handle" table for
         * an unnecessarily long time in any case.  As a compromise, keeping
         * the race condition within the smallest possible window seems the
         * best of some poor alternatives.
         *
         * Use a temporary context so the new Handle entry is determined
         * and created within as small a time as possible, to narrow the
         * window for this race condition.
         */
        Context tempContext = new Context();
        try
        {
            // XXX FIXME: this is only implemented for PostgreSQL;
            //    no Oracle server available to test on.

            // find largest existing suffix.
            final String query =
                "SELECT MAX(TO_NUMBER(SPLIT_PART(handle,'/',2),'999999999')) "+
                "AS maxid FROM handle WHERE handle LIKE ? ;";
            TableRow maxRow = DatabaseManager.querySingle(tempContext, query, handlePrefix+"/%");
            int maxID = -1;
            if (maxRow != null)
                maxID = maxRow.getIntColumn("maxid");

            // if no max ID could be computed, check whether table is empty,
            // in which case we can take the first ID.
            if (maxID < 0)
            {
                TableRow ctRow = DatabaseManager.querySingle(tempContext,
                    "SELECT COUNT(handle_id) AS count FROM handle;");
                if (ctRow == null || ctRow.getLongColumn("count") != 0)
                    throw new SQLException("Failed finding maximum of Handle suffixes, and handle table not empty; check log.");
                maxID = 0;
            }

            // assemble new Handle
            ++maxID;
            String newHandle = new StringBuffer(handlePrefix)
                .append("/").append(String.valueOf(maxID)).toString();

            TableRow handle = DatabaseManager.create(tempContext, "Handle");
            handle.setColumn("handle", newHandle);
        	handle.setColumn("resource_type_id", dso.getType());
        	handle.setColumn("resource_id", dso.getID());
        	DatabaseManager.update(tempContext, handle);
        
        	if (log.isDebugEnabled())
                log.debug("Created new handle \""+newHandle+"\" for "+dso.toString());
            return newHandle;
        }
        catch (SQLException e)
        {
            log.error("Got SQL error allocating hew Handle suffix: ",e);
            tempContext.abort();
            tempContext = null;
            throw e;
        }
        finally
        {
            if (tempContext != null)
                tempContext.complete();
        }
    }

    /**
     * Creates a handle entry, or updates one if it already exists,
     * for a handle supplied by the caller.  It is an error to rebind
     * a Handle that is occupied by an object that actualy exists,
     * although deleted objects are overwritten automatically.
     *
     * If the object is null it creates or sets an "unbound" handle.
     * This is required by the AIP system when it is in the process of
     * restoring internal AIPs.  The concept of an unbound handle is
     * ALSO needed to save the spot in the Handle table after an Item is
     * deleted, so that Handle doesn't get reused inadvertently on a
     * different resource.
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            DSpaceObject - MAY be null to make handle unbound.
     * @param suppliedHandle
     *            existing handle, must be a valid string.
     * @return the Handle
     */
    public static String createHandle(Context context, DSpaceObject dso,
            String suppliedHandle) throws SQLException
    {
        // replace existing handle if there is one, since it may have
        // e.g. been created by InternalAIP during restoration.
        TableRow row = DatabaseManager.findByUnique(context, "Handle",
                         "handle", suppliedHandle);

        // if no existing handle found, create a new entry.
        if (row == null)
        {
            row = DatabaseManager.create(context, "Handle");
            row.setColumn("handle", suppliedHandle);
        }

        // otherwise make sure this handle is not bound to an existing object
        else if (!(row.isColumnNull("resource_type_id") &&
                   row.isColumnNull("resource_id")))
        {
            DSpaceObject oldDso = resolveToObject(context, suppliedHandle);
            if (oldDso != null)
                throw new SQLException("Cannot rebind Handle;  handle "+suppliedHandle+" is already bound to another object: "+oldDso.toString());
        }

        if (dso == null)
        {
            row.setColumnNull("resource_type_id");
            row.setColumnNull("resource_id");
        	if (log.isDebugEnabled())
                log.debug("Created unbound handle "+suppliedHandle);
        }
        else
        {
            row.setColumn("resource_type_id", dso.getType());
            row.setColumn("resource_id", dso.getID());
            if (log.isDebugEnabled())
                log.debug("Created predetermined handle "+suppliedHandle+" for "+dso.toString());
        }
        DatabaseManager.update(context, row);
        return suppliedHandle;
	}

    /**
     * Removes binding of Handle to a DSpace object, while leaving the
     * Handle in the table so it doesn't get reallocated.  The AIP
     * implementation also needs it there for foreign key references.
     *
     * @param context DSpace context
     * @param dso DSpaceObject whose Handle to unbind.
     */
    public static void unbindHandle(Context context, DSpaceObject dso)
        throws SQLException
    {
        TableRow row = getHandleInternal(context, dso.getType(), dso.getID());
        if (row != null)
        {
            row.setColumnNull("resource_type_id");
            row.setColumnNull("resource_id");
            DatabaseManager.update(context, row);
        }
        else
            log.warn("Cannot find Handle entry to unbind for object="+dso.toString());
    }

    /**
     * Return the object which handle maps to, or null. This is the object
     * itself, not a URL which points to it.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The object which handle maps to, or null if handle is not mapped
     *         to any object.
     * @exception SQLException
     *                If a database error occurs
     */
    public static DSpaceObject resolveToObject(Context context, String handle)
            throws SQLException
    {
        TableRow dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null)
        {
            if (handle.equals(Site.getSiteHandle()))
                return Site.find(context, 0);
            return null;
        }

        // handles can be left unbound intentionally, e.g. for
        // internal AIPs of deleted or not-yet-existant objects.
        if ((dbhandle.isColumnNull("resource_type_id"))
                || (dbhandle.isColumnNull("resource_id")))
        {
            log.debug("Request for unbound Handle, handle="+handle);
            return null;
        }

        // What are we looking at here?
        int handletypeid = dbhandle.getIntColumn("resource_type_id");
        int resourceID = dbhandle.getIntColumn("resource_id");

        if (handletypeid == Constants.ITEM)
        {
            Item item = Item.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to item "
                        + ((item == null) ? (-1) : item.getID()));
            }

            return item;
        }
        else if (handletypeid == Constants.COLLECTION)
        {
            Collection collection = Collection.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to collection "
                        + ((collection == null) ? (-1) : collection.getID()));
            }

            return collection;
        }
        else if (handletypeid == Constants.COMMUNITY)
        {
            Community community = Community.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to community "
                        + ((community == null) ? (-1) : community.getID()));
            }

            return community;
        }

        throw new IllegalStateException("Unsupported Handle Type "
                + Constants.typeText[handletypeid]);
    }

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            The object to obtain a handle for
     * @return The handle for object, or null if the object has no handle.
     * @exception SQLException
     *                If a database error occurs
     */
    public static String findHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        TableRow row = getHandleInternal(context, dso.getType(), dso.getID());
        if (row == null)
        {
            if (dso.getType() == Constants.SITE)
                return Site.getSiteHandle();
            else
                return null;
        }
        else
            return row.getStringColumn("handle");
    }

    /**
     * Return all the handles which start with prefix.
     * 
     * @param context
     *            DSpace context
     * @param prefix
     *            The handle prefix
     * @return A list of the handles starting with prefix. The list is
     *         guaranteed to be non-null. Each element of the list is a String.
     * @exception SQLException
     *                If a database error occurs
     */
    static List getHandlesForPrefix(Context context, String prefix)
            throws SQLException
    {
        String sql = "SELECT handle FROM handle WHERE handle LIKE ? ";
        TableRowIterator iterator = DatabaseManager.queryTable(context, null, sql, prefix+"%");
        List results = new ArrayList();

        while (iterator.hasNext())
        {
            TableRow row = (TableRow) iterator.next();
            results.add(row.getStringColumn("handle"));
        }
        
        iterator.close();

        return results;
    }

    /**
     * return Handle corresponding to database ID
     * inverse of getID()
     *
     * @param context DSpace context
     * @param id database row of the Handle entry
     * @return The handle in this row, or null there is no such row.
     * @exception SQLException
     *                If a database error occurs
     */
    public static String find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "Handle", id);
        return (row == null) ? null : row.getStringColumn("handle");
    }

    /**
     * Returns database row number of this Handle's record, or -1 if not found.
     * Inverse of find()
     *
     * @param context DSpace context
     * @param handle The handle in this row, or null there is no such row.
     * @return database row number of the Handle entry
     * @exception SQLException
     *                If a database error occurs
     */
    public static int getID(Context context, String handle)
        throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "Handle",
                         "handle", handle);
        return (row == null) ? -1 : row.getIntColumn("handle_id");
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     * 
     * @param context
     *            DSpace context
     * @param type
     *            The type of object
     * @param id
     *            The id of object
     * @return The handle for object, or null if the object has no handle.
     * @exception SQLException
     *                If a database error occurs
     */
    private static TableRow getHandleInternal(Context context, int type, int id)
            throws SQLException
    {   	
        String sql = "SELECT * FROM Handle WHERE resource_type_id = ? " +
      				 "AND resource_id = ?";
        return DatabaseManager.querySingleTable(context, "Handle", sql, type, id);
    }

    /**
     * Find the database row corresponding to handle.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The database row corresponding to the handle
     * @exception SQLException
     *                If a database error occurs
     */
    private static TableRow findHandleInternal(Context context, String handle)
            throws SQLException
    {
        if (handle == null)
        {
            throw new IllegalArgumentException("Handle is null");
        }

        return DatabaseManager
                .findByUnique(context, "Handle", "handle", handle);
    }
}
