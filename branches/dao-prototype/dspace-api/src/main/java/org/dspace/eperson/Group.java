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
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.dao.EPersonDAO;           // Naughty!
import org.dspace.eperson.dao.EPersonDAOFactory;    // Naughty!
import org.dspace.eperson.dao.GroupDAO;             // Naughty!
import org.dspace.eperson.dao.GroupDAOFactory;      // Naughty!

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
    protected List<EPerson> epeople;
    protected List<Group> groups;

    public Group(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = GroupDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);

        epeople = new ArrayList<EPerson>();
        groups = new ArrayList<Group>();

        context.cache(this, id);
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

    public Group[] getMemberGroups()
    {
        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
    {
        return (EPerson[]) epeople.toArray(new EPerson[0]);
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
        dao.update(this);
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
            throws SQLException
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
            throws SQLException
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(name);

        return group;
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
