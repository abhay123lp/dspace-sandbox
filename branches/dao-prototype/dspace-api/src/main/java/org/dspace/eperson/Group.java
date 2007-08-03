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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
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
    protected GroupDAO dao;
    protected EPersonDAO epersonDAO;

    private String name;

    /** lists of epeople and groups in the group */
    private List<EPerson> epeople = new ArrayList<EPerson>();

    private List<Group> groups = new ArrayList<Group>();

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;

    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Construct a Group from a given context and tablerow
     * 
     * @param context
     * @param row
     */
    Group(Context context, TableRow row) throws SQLException
    {
        this.id = id;
        this.context = context;

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_group_id"));

        modifiedMetadata = false;
        clearDetails();
    }

        epeople = new ArrayList<EPerson>();
        groups = new ArrayList<Group>();

        context.cache(this, id);
    }

    /**
     * Create a new group
     * 
     * @param context
     *            DSpace context object
     */
    public static Group create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME - authorization?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson Group");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "epersongroup");

        Group g = new Group(context, row);

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                + g.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(), null));

        return g;
    }

    /**
     * get the ID of the group object
     * 
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("eperson_group_id");
    }

    /**
     * get name of group
     * 
     * @return name
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        modifiedMetadata = true;
        addDetails("name");
    }

    public void addMember(EPerson e)
    {
        if (isMember(e))
        {
            return;
        }

        epeople.add(e);

        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
    }

    public void addMember(Group g)
    {
        if (isMember(g))
        {
            return;
        }

        groups.add(g);

        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
    }

    public void removeMember(EPerson e)
    {
        loadData(); // make sure Group has data loaded

        if (epeople.remove(e))
        {
            epeopleChanged = true;
            myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
        }
    }

    public void removeMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        if (groups.remove(g))
        {
            groupsChanged = true;
            myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
        }
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

    public Group[] getMemberGroups()
    {
        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
    {
        List<Group> groupList = new ArrayList<Group>();

        Set<Integer> myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator i = myGroups.iterator();

        while (i.hasNext())
        {
            groupList.add(Group.find(c, ((Integer) i.next()).intValue()));
        }

        return (Group[]) groupList.toArray(new Group[0]);
    }

    /**
     * Return true if group has no members
     */
    public static Set<Integer> allMemberGroupIDs(Context c, EPerson e)
            throws SQLException
    {
        // two queries - first to get groups eperson is a member of
        // second query gets parent groups for groups eperson is a member of

        TableRowIterator tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?",
                 e.getID());

        Set<Integer> groupIDs = new HashSet<Integer>();

        while (tri.hasNext())
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     * 
     * @param c   
     *          DSpace context
     * @param g   
     *          Group object
     * @return   Array of EPerson objects
     * @throws SQLException
     */
    public static EPerson[] allMembers(Context c, Group g)
            throws SQLException
    {
        List<EPerson> epersonList = new ArrayList<EPerson>();

        Set<Integer> myEpeople = allMemberIDs(c, g);
        // now convert those Integers to EPerson objects
        Iterator i = myEpeople.iterator();

    /**
     * return type found in Constants
     */
    public static Set<Integer> allMemberIDs(Context c, Group g)
            throws SQLException
    {
        // two queries - first to get all groups which are a member of this group
        // second query gets all members of each group in the first query
        Set<Integer> epeopleIDs = new HashSet<Integer>();
        
        // Get all groups which are a member of this group
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                "SELECT * FROM group2groupcache WHERE parent_id= ? ",
                g.getID());
        
        Set<Integer> groupIDs = new HashSet<Integer>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            int childID = row.getIntColumn("child_id");

            groupIDs.add(new Integer(childID));
        }
        
        tri.close();

        // now we have all the groups (including this one)
        // it is time to find all the EPeople who belong to those groups
        // and filter out all duplicates

        Object[] parameters = new Object[groupIDs.size()+1];
        int idx = 0;
        Iterator i = groupIDs.iterator();

        // don't forget to add the current group to this query!
        parameters[idx++] = new Integer(g.getID());
        String epersonQuery = "eperson_group_id= ? ";
        if (i.hasNext())
            epersonQuery += " OR ";
        
        while (i.hasNext())
        {
            int groupID = ((Integer) i.next()).intValue();
            parameters[idx++] = new Integer(groupID);
            
            epersonQuery += "eperson_group_id= ? ";
            if (i.hasNext())
                epersonQuery += " OR ";
        }

        //get all the EPerson IDs
        // Note: even through the query is dynamicaly built all data is seperated
        // into the parameters array.
        tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                "SELECT * FROM epersongroup2eperson WHERE " + epersonQuery,
                parameters);

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            int epersonID = row.getIntColumn("eperson_id");
   
            epeopleIDs.add(new Integer(epersonID));
        }
        
        tri.close();

        return epeopleIDs;
    }

    private static boolean epersonInGroup(Context c, int groupID, EPerson e)
            throws SQLException
    {
        Set<Integer> groupIDs = Group.allMemberGroupIDs(c, e);

    @Deprecated
    public static boolean isMember(Context context, int groupID)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        return dao.currentUserInGroup(groupID);
    }

    @Deprecated
    Group(Context context, org.dspace.storage.rdbms.TableRow row)
    {
        this(context, row.getIntColumn("eperson_group_id"));
    }

    @Deprecated
    public static Group create(Context context) throws AuthorizeException
    {
        log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        log.info("Called Group.create()");
        log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.create();

        return group;
    }

    public void update() throws AuthorizeException
    {
        // FIXME: authorizations

        myContext.addEvent(new Event(Event.DELETE, Constants.GROUP, getID(), getName()));

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(myContext, getID());

        // Remove any group memberships first
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_group_id= ? ",
                getID());

        // remove any group2groupcache entries
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2groupcache WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // Now remove any group2group assignments
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2group WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // don't forget the new table
        deleteEpersonGroup2WorkspaceItem();

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        epeople.clear();

        log.info(LogManager.getHeader(myContext, "delete_group", "group_id="
                + getID()));
    }

    @Deprecated
    public void delete() throws AuthorizeException
    {
        dao.delete(this.getID());
    }

    @Deprecated
    public static Group find(Context context, int id)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(id);

        return group;
    }

    @Deprecated
    public static Group[] findAll(Context context, int sortField)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.getGroups(sortField);

        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * FIXME: Assumes the group name is unique. I don't think this is enforced
     * anywhere. Even so, this should probably call search() anyway.
     */
    @Deprecated
    public static Group findByName(Context context, String name)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(name);

        if (modifiedMetadata)
        {
            myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }

        // Redo eperson mappings if they've changed
        if (epeopleChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(myContext,
                    "delete from epersongroup2eperson where eperson_group_id= ? ",
                    getID());

            // Add new mappings
            Iterator i = epeople.iterator();

            while (i.hasNext())
            {
                EPerson e = (EPerson) i.next();

                TableRow mappingRow = DatabaseManager.create(myContext,
                        "epersongroup2eperson");
                mappingRow.setColumn("eperson_id", e.getID());
                mappingRow.setColumn("eperson_group_id", getID());
                DatabaseManager.update(myContext, mappingRow);
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
            Iterator i = groups.iterator();

            while (i.hasNext())
            {
                Group g = (Group) i.next();

                TableRow mappingRow = DatabaseManager.create(myContext,
                        "group2group");
                mappingRow.setColumn("parent_id", getID());
                mappingRow.setColumn("child_id", g.getID());
                DatabaseManager.update(myContext, mappingRow);
            }

            // groups changed, now change group cache
            rethinkGroupCache();

            groupsChanged = false;
        }

        log.info(LogManager.getHeader(myContext, "update_group", "group_id="
                + getID()));
    }

    @Deprecated
    public static EPerson[] allMembers(Context context, Group group)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.getAllEPeople(group);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    @Deprecated
    public static Group[] allMemberGroups(Context context, EPerson eperson)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.getGroups(eperson);

        return (Group[]) groups.toArray(new Group[0]);
    }

    @Deprecated
    public static Group[] search(Context context, String query)
	{
	    return search(context, query, -1, -1);
	}

        Map<Integer,Set<Integer>> parents = new HashMap<Integer,Set<Integer>>();

        while (tri.hasNext())
        {
            TableRow row = (TableRow) tri.next();

            Integer parentID = new Integer(row.getIntColumn("parent_id"));
            Integer childID = new Integer(row.getIntColumn("child_id"));

            // if parent doesn't have an entry, create one
            if (!parents.containsKey(parentID))
            {
                Set<Integer> children = new HashSet<Integer>();

                // add child id to the list
                children.add(childID);
                parents.put(parentID, children);
            }
            else
            {
                // parent has an entry, now add the child to the parent's record
                // of children
                Set<Integer> children =  parents.get(parentID);
                children.add(childID);
            }
        }
        
        tri.close();

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash

        Iterator i = parents.keySet().iterator();

        while (i.hasNext())
        {
            Integer parentID = (Integer) i.next();

            Set<Integer> myChildren = getChildren(parents, parentID);

            Iterator j = myChildren.iterator();

            while (j.hasNext())
            {
                // child of a parent
                Integer childID = (Integer) j.next();

                ((Set<Integer>) parents.get(parentID)).add(childID);
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

            Set<Integer> children =  parents.get(parent);
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
    private Set<Integer> getChildren(Map<Integer,Set<Integer>> parents, Integer parent)
    {
        Set<Integer> myChildren = new HashSet<Integer>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
            return myChildren;

        // got this far, so we must have children
        Set<Integer> children =  parents.get(parent);

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
