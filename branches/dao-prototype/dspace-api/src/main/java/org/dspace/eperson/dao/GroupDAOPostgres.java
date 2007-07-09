/*
 * GroupDAOPostgres.java
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
package org.dspace.content.dao;

import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.proxy.GroupProxy;
import org.dspace.content.uri.ObjectIdentifier;

/**
 * @author James Rutherford
 */
public class GroupDAOPostgres extends GroupDAO
{
    public GroupDAOPostgres(Context context)
    {
        this.context = context;
    }

    public Group create() throws AuthorizeException
    {
        Group group = super.create();

        try
        {
            UUID uuid = UUID.randomUUID();

            TableRow row = DatabaseManager.create(context, "epersongroup");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("eperson_group_id");

            return super.create(id, uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Group retrieve(int id)
    {
        Group group = super.retrieve(id);

        if (group != null)
        {
            return group;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "epersongroup", id);

            if (row == null)
            {
                log.warn("group " + id + " not found");
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

    public Group retrieve(UUID uuid)
    {
        Group group = super.retrieve(uuid);

        if (group != null)
        {
            return group;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "epersongroup", "uuid", uuid);

            if (row == null)
            {
                log.warn("group " + uuid + " not found");
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

    private Group retrieve(TableRow row)
    {
        int id = row.getIntColumn("eperson_group_id");

        Group group = super.retrieve(id);

        if (group != null)
        {
            return group;
        }

        group = new GroupProxy(context, id);
        populateGroupFromTableRow(group, row);

        context.cache(group, id);

        return group;
    }

    public void update(Group group) throws AuthorizeException
    {
        super.update(group);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "epersongroup", group.getID());

            if (row != null)
            {
                update(group, row);
            }
            else
            {
                throw new RuntimeException("Didn't find group " +
                        group.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Group group, TableRow row) throws AuthorizeException
    {
        try
        {
            populateTableRowFromGroup(group, row);
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME We need link() and unlink() for EPerson <--> Group and
     * Group <--> Group mapping
     */
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            // Remove any group memberships first
            DatabaseManager.updateQuery(context,
                    "DELETE FROM epersongroup2eperson " +
                    "WHERE eperson_group_id = ? ",
                    id);

            // remove any group2groupcache entries
            DatabaseManager.updateQuery(context,
                    "DELETE FROM group2groupcache " +
                    "WHERE parent_id = ? OR child_id = ? ",
                    id, id);

            // Now remove any group2group assignments
            DatabaseManager.updateQuery(context,
                    "DELETE FROM group2group " +
                    "WHERE parent_id = ? OR child_id = ? ",
                    id, id);

            // don't forget the new table
            DatabaseManager.updateQuery(myContext,
                    "DELETE FROM epersongroup2workspaceitem " +
                    "WHERE eperson_group_id = ? ",
                    id);

            // Remove ourself
            DatabaseManager.delete(context, "epersongroup", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Group> getGroups(int sortField)
    {
        String s;

        switch (sortField)
        {
            case ID:
                s = "eperson_group_id";
                break;
            case NAME:
                s = "name";
                break;
            default:
                s = "name";
        }

        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "epersongroup",
                    "SELECT eperson_group_id FROM epersongroup ORDER BY " + s);

            List<Group> groups = new ArrayList<Group>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("eperson_group_id");
                groups.add(retrieve(id));
            }

            return groups;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Group> search(String query)
    {
	    return search(query, -1, -1);
    }

    public List<Group> search(Context context, String query,
            int offset, int limit)
	{
		String params = "%" + query.toLowerCase() + "%";
		String dbquery =
            "SELECT eperson_group_id FROM epersongroup " +
            "WHERE name ILIKE ? " +
            "OR eperson_group_id = ? " +
            "ORDER BY name ASC";

		if (offset >= 0 && limit > 0)
        {
			dbquery += " LIMIT " + limit + " OFFSET " + offset;
		}

        // When checking against the eperson-id, make sure the query can be
        // made into a number
		Integer int_param;
		try
        {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e)
        {
			int_param = new Integer(-1);
		}

        try
        {
            TableRowIterator tri = DatabaseManager.query(context, dbquery,
                    new Object[] {params, int_param});

            List<Group> groups = new ArrayList<Group>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("eperson_group_id");
                groups.add(retrieve(id));
            }

            return groups;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
	}

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateTableRowFromGroup(Group group, TableRow row)
    {
        row.setColumn("name", group.getName());
    }

    /**
     * Regenerate the group cache AKA the group2groupcache table in the
     * database - meant to be called when a group is added or removed from
     * another group
     */
    private void rethinkGroupCache() throws SQLException
    {
        // read in the group2group table
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "group2group",
                "SELECT * FROM group2group");

        Map parents = new HashMap();

        while (tri.hasNext())
        {
            TableRow row = (TableRow) tri.next();

            Integer parentID = new Integer(row.getIntColumn("parent_id"));
            Integer childID = new Integer(row.getIntColumn("child_id"));

            // if parent doesn't have an entry, create one
            if (!parents.containsKey(parentID))
            {
                Set children = new HashSet();

                // add child id to the list
                children.add(childID);
                parents.put(parentID, children);
            }
            else
            {
                // parent has an entry, now add the child to the parent's record
                // of children
                Set children = (Set) parents.get(parentID);
                children.add(childID);
            }
        }
        
        tri.close();

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash

//        Iterator i = parents.keySet().iterator();

        for (Integer parentID : parents.keySet())
        {
//            Integer parentID = (Integer) i.next();

            Set myChildren = getChildren(parents, parentID);

            Iterator j = myChildren.iterator();

            for (Integer childID : myChildren)
            {
                // child of a parent
//                Integer childID = (Integer) j.next();

                ((Set) parents.get(parentID)).add(childID);
            }
        }

        // empty out group2groupcache table
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2groupcache WHERE id >= 0");

        // write out new one
        Iterator pi = parents.keySet().iterator(); // parent iterator

        while (pi.hasNext())
        {
            Integer parent = (Integer) pi.next();

            Set children = (Set) parents.get(parent);
            Iterator ci = children.iterator(); // child iterator

            while (ci.hasNext())
            {
                Integer child = (Integer) ci.next();

                TableRow row = DatabaseManager.create(myContext,
                        "group2groupcache");

                int parentID = parent.intValue();
                int childID = child.intValue();

                row.setColumn("parent_id", parentID);
                row.setColumn("child_id", childID);

                DatabaseManager.update(myContext, row);
            }
        }
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     * 
     * @param parents
     *            Map of parent,child relationships
     * @param parent
     *            the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    private Set getChildren(Map parents, Integer parent)
    {
        Set myChildren = new HashSet();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
            return myChildren;

        // got this far, so we must have children
        Set children = (Set) parents.get(parent);

        // now iterate over all of the children
        Iterator i = children.iterator();

        while (i.hasNext())
        {
            Integer childID = (Integer) i.next();

            // add this child's ID to our return set
            myChildren.add(childID);

            // and now its children
            myChildren.addAll(getChildren(parents, childID));
        }

        return myChildren;
    }
}
