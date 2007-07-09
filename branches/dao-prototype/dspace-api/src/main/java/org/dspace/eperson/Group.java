/*
 * Group.java
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
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing a group of e-people.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class Group extends DSpaceObject
{
    // findAll sortby types
    public static final int ID = 0; // sort by ID

    public static final int NAME = 1; // sort by NAME (default)

    /** log4j logger */
    private static Logger log = Logger.getLogger(Group.class);

    private Context context;
    private GroupDAO dao;

    private String name;

    /** lists of epeople and groups in the group */
    private List<EPerson> epeople;
    private List<Group> groups;

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;
    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    public Group(Context context, int id)
    {
        this.context = context;
        this.dao = GroupDAOFactory.getInstance(context);

        epeople = new ArrayList<EPerson>();
        groups = new ArrayList<Group>();
    }

    @Override
    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addMember(EPerson e)
    {
        if (isMember(e))
        {
            return;
        }

        epeople.add(e);
    }

    public void addMember(Group g)
    {
        if (isMember(g))
        {
            return;
        }

        groups.add(g);
    }

    public void removeMember(EPerson e)
    {
        epeople.remove(e);
    }

    public void removeMember(Group g)
    {
        groups.remove(g);
    }

    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (id == 0)
        {
            return true;
        }

        return epeople.contains(e);
    }

    public boolean isMember(Group g)
    {
        return groups.contains(g);
    }

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     *
     * @param c
     *            context
     * @param groupid
     *            group ID to check
     */
    public static boolean isMember(Context c, int groupid) throws SQLException
    {
        // special, everyone is member of group 0 (anonymous)
        if (groupid == 0)
        {
            return true;
        }

        // first, check for membership if it's a special group
        // (special groups can be set even if person isn't authenticated)
        if (c.inSpecialGroup(groupid))
        {
            return true;
        }

        EPerson currentuser = c.getCurrentUser();

        // only test for membership if context contains a user
        if (currentuser != null)
        {
//            return epersonInGroup(c, groupid, currentuser);
            Set groupIDs = Group.allMemberGroupIDs(c, currentuser);

            return groupIDs.contains(new Integer(groupid));
        }

        // currentuser not set, return FALSE
        return false;
    }

    public List<Group> getSubGroups()
    {
        return groups;
    }

    /**
     * Use getSubGroups() instead.
     */
    @Deprecated
    public Group[] getMemberGroups()
    {
        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * Get all of the groups that an eperson is a member of
     *
     * @param c
     * @param e
     * @return
     * @throws SQLException
     */
    public static Group[] allMemberGroups(Context c, EPerson e)
            throws SQLException
    {
        List groupList = new ArrayList();

        Set myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator i = myGroups.iterator();

        while (i.hasNext())
        {
            groupList.add(Group.find(c, ((Integer) i.next()).intValue()));
        }

        return (Group[]) groupList.toArray(new Group[0]);
    }

    /**
     * get Set of Integers all of the group memberships for an eperson
     *
     * @param c
     * @param e
     * @return Set of Integer groupIDs
     * @throws SQLException
     */
    public static Set allMemberGroupIDs(Context c, EPerson e)
            throws SQLException
    {
        // two queries - first to get groups eperson is a member of
        // second query gets parent groups for groups eperson is a member of

        TableRowIterator tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?",
                 e.getID());

        Set groupIDs = new HashSet();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            int childID = row.getIntColumn("eperson_group_id");

            groupIDs.add(new Integer(childID));
        }

        tri.close();

        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        Group[] specialGroups = c.getSpecialGroups();
        for(int j=0; j<specialGroups.length;j++)
            groupIDs.add(new Integer(specialGroups[j].getID()));

        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!

        String groupQuery = "";

        Iterator i = groupIDs.iterator();

        // Build a list of query parameters
        Object[] parameters = new Object[groupIDs.size()];
        int idx = 0;
        while (i.hasNext())
        {
            int groupID = ((Integer) i.next()).intValue();

            parameters[idx++] = new Integer(groupID);

            groupQuery += "child_id= ? ";
            if (i.hasNext())
                groupQuery += " OR ";
        }

        if ("".equals(groupQuery))
        {
            // don't do query, isn't member of any groups
            return groupIDs;
        }

        // was member of at least one group
        // NOTE: even through the query is built dynamicaly all data is
        // seperated into the the parameters array.
        tri = DatabaseManager.queryTable(c, "group2groupcache",
                "SELECT * FROM group2groupcache WHERE " + groupQuery,
                parameters);

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            int parentID = row.getIntColumn("parent_id");

            groupIDs.add(new Integer(parentID));
        }

        tri.close();

        return groupIDs;
    }

    /**
     * Find the group by its name - assumes name is unique
     *
     * @param context
     * @param name
     *
     * @return Group
     */
    public static Group findByName(Context context, String name)
            throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "epersongroup",
                "name", name);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            Group fromCache = (Group) context.fromCache(Group.class, row
                    .getIntColumn("eperson_group_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Group(context, row);
            }
        }
    }

    /**
     * Finds all groups in the site
     *
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Group.ID or Group.NAME
     *
     * @return array of all groups in the site
     */
    public static Group[] findAll(Context context, int sortField)
            throws SQLException
    {
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
    {
        return (EPerson[]) epeople.toArray(new Eperson[0]);
    }

    /**
     * Return true if group has no members
     */
    public boolean isEmpty()
    {
        if ((epeople.size() == 0) && (groups.size() == 0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Redo eperson mappings if they've changed
        if (epeopleChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(context,
                    "delete from epersongroup2eperson where eperson_group_id= ? ",
                    getID());

            for (EPerson eperson : group.getMembers())
            {
                TableRow mappingRow = DatabaseManager.create(context,
                        "epersongroup2eperson");
                mappingRow.setColumn("eperson_id", eperson.getID());
                mappingRow.setColumn("eperson_group_id", group.getID());
                DatabaseManager.update(context, mappingRow);
            }

            epeopleChanged = false;
        }

        // Redo Group mappings if they've changed
        if (groupsChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(myContext,
                    "delete from group2group where parent_id= ? ",
                    getID());

            // Add new mappings
            for (Group child : group.getSubGroups())
            {
                Group child = (Group) i.next();

                TableRow mappingRow = DatabaseManager.create(myContext,
                        "group2group");
                mappingRow.setColumn("parent_id", group.getID());
                mappingRow.setColumn("child_id", child.getID());
                DatabaseManager.update(context, mappingRow);
            }

            // groups changed, now change group cache
            rethinkGroupCache();

            groupsChanged = false;
        }

        log.info(LogManager.getHeader(myContext, "update_group", "group_id="
                + getID()));
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.GROUP;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    Group(Context context, TableRow row)
    {
        this(context, row.getIntColumn("eperson_group_id");
    }

    @Deprecated
    public static Group create(Context context) throws AuthorizeException
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.create();

        return group;
    }

    @Deprecated
    public void delete()
    {
        dao.delete(id);
    }

    @Deprecated
    public static Group find(Context context, int id)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(id);

        return group;
    }

    @Deprecated
    public static EPerson[] allMembers(Context context, Group group)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.getAllEpeople(group);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    @Deprecated
    public static Group[] search(Context context, String query)
    		throws SQLException
	{
	    return search(context, query, -1, -1);
	}

    @Deprecated
    public static Group[] search(Context context, String query,
            int offset, int limit)
	{
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.search(query, offset, limit);

        return (Group[]) groups.toArray(new Group[0]);
	}
}
