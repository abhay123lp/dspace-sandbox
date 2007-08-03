/*
 * Community.java
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
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.content.dao.BitstreamDAO;         // Naughty!
import org.dspace.content.dao.BitstreamDAOFactory;  // Naughty!
import org.dspace.content.dao.CollectionDAO;        // Naughty!
import org.dspace.content.dao.CollectionDAOFactory; // Naughty!
import org.dspace.content.dao.CommunityDAO;         // Naughty!
import org.dspace.content.dao.CommunityDAOFactory;  // Naughty!
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 *
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class Community extends DSpaceObject
{
    private static Logger log = Logger.getLogger(Community.class);

    private Context context;
    private CommunityDAO dao;
    private BitstreamDAO bitstreamDAO;
    private CollectionDAO collectionDAO;

    private String identifier;
    private int logoID;
    private Bitstream logo;

    private Map<String, String> metadata;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Construct a community object from a database row.
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    Community(Context context, TableRow row) throws SQLException
    {
        this.id = id;
        this.context = context;

        dao = CommunityDAOFactory.getInstance(context);
        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);

        identifiers = new ArrayList<ExternalIdentifier>();
        metadata = new TreeMap<String, String>();

        context.cache(this, id);

        modified = modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Get a community from the database. Loads in the metadata
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the community
     * 
     * @return the community, or null if the ID is invalid.
     */
    public static Community find(Context context, int id) throws SQLException
    {
        // First check the cache
        Community fromCache = (Community) context
                .fromCache(Community.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "community", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_community",
                        "not_found,community_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_community",
                        "community_id=" + id));
            }

            return new Community(context, row);
        }
    }

    /**
     * Create a new community, with a new ID.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the newly created community
     */
    public static Community create(Community parent, Context context)
            throws SQLException, AuthorizeException
    {
        // Only administrators and adders can create communities
        if (!(AuthorizeManager.isAdmin(context) || AuthorizeManager
                .authorizeActionBoolean(context, parent, Constants.ADD)))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        TableRow row = DatabaseManager.create(context, "community");
        Community c = new Community(context, row);
        c.handle = HandleManager.createHandle(context, c);

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = Group.find(context, 0);

        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        context.addEvent(new Event(Event.CREATE, Constants.COMMUNITY, c.getID(), c.handle));

        // if creating a top-level Community, simulate an ADD event at the Site.
        if (parent == null)
            context.addEvent(new Event(Event.ADD, Constants.SITE, Site.SITE_ID, Constants.COMMUNITY, c.getID(), c.handle));

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + row.getIntColumn("community_id"))
                + ",handle=" + c.handle);

        return c;
    }

    /**
     * Get a list of all communities in the system. These are alphabetically
     * sorted by community name.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the communities in the system
     */
    public static Community[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.queryTable(context, "community",
                "SELECT * FROM community ORDER BY name");

        List<Community> communities = new ArrayList<Community>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) context.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                communities.add(fromCache);
            }
            else
            {
                communities.add(new Community(context, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get a list of all top-level communities in the system. These are
     * alphabetically sorted by community name. A top-level community is one
     * without a parent community.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the top-level communities in the system
     */
    public static Community[] findAllTop(Context context) throws SQLException
    {
        // get all communities that are not children
        TableRowIterator tri = DatabaseManager.queryTable(context, "community",
                "SELECT * FROM community WHERE NOT community_id IN "
                        + "(SELECT child_comm_id FROM community2community) "
                        + "ORDER BY name");

        List<Community> topCommunities = new ArrayList<Community>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) context.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                topCommunities.add(fromCache);
            }
            else
            {
                topCommunities.add(new Community(context, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[topCommunities.size()];
        communityArray = (Community[]) topCommunities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get the internal ID of this collection
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return communityRow.getIntColumn("community_id");
    }

    /**
     * @see org.dspace.content.DSpaceObject#getHandle()
     */
    public String getHandle()
    {
        if(handle == null) {
        	try {
				handle = HandleManager.findHandle(this.ourContext, this);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
        }
    	return handle;
    }

    /**
     * Get the value of a metadata field
     * 
     * @param field
     *            the name of the metadata field to get
     * 
     * @return the value of the metadata field
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String field)
    {
        return metadata.get(field);
    }

    public void setMetadata(String field, String value)
    {
        if ((field.trim()).equals("name") && (value.trim()).equals(""))
        {
            try
            {
                value = I18nUtil.getMessage(
                        "org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }
        metadata.put(field, value);
        modifiedMetadata = true;
        addDetails(field);
    }

    public String getName()
    {
        return getMetadata("name");
    }

    /**
     * Get the logo for the community. <code>null</code> is return if the
     * community does not have a logo.
     * 
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    /**
     * Give the community a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that
     * <code>update(/code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param  is   the stream to use as the new logo
     *
     * @return   the new logo bitstream, or <code>null</code> if there is no
     *           logo (<code>null</code> was passed in)
     */
    public Bitstream setLogo(InputStream is)
        throws AuthorizeException, IOException
    {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                context, this, Constants.DELETE)))
        {
            canEdit();
        }

        // First, delete any existing logo
        if (logo != null)
        {
            log.info(LogManager.getHeader(context, "remove_logo",
                    "community_id=" + getID()));

            logo.delete();
            logo = null;
        }

        if (is != null)
        {
            Bitstream newLogo = bitstreamDAO.store(is);
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
            List policies = AuthorizeManager.getPoliciesActionFilter(context,
                    this, Constants.READ);
            AuthorizeManager.addPolicies(context, policies, newLogo);

            log.info(LogManager.getHeader(context, "set_logo",
                    "community_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        modified = true;
        return logo;
    }

    public void setLogoBitstream(Bitstream logo)
    {
        // Check authorisation
        canEdit();

        log.info(LogManager.getHeader(ourContext, "update_community",
                "community_id=" + getID()));

        DatabaseManager.update(ourContext, communityRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.COMMUNITY, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     *
     * @return the new collection
     */
    public Collection createCollection() throws AuthorizeException
    {
        List<Collection> collections = new ArrayList<Collection>();

        Collection collection = collectionDAO.create();

        ArchiveManager.move(context, collection, null, this);

        return collection;
    }

    /**
     * Create a new sub-community within this community.
     *
     * @return the new community
     */
    public Community createSubcommunity() throws AuthorizeException
    {
        List<Community> subcommunities = new ArrayList<Community>();

        Community community = dao.create();

        ArchiveManager.move(context, community, null, this);

        return community;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public boolean canEditBoolean()
    {
        try
        {
            canEdit();

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    public void canEdit() throws AuthorizeException
    {
        List<Community> parents = dao.getParentCommunities(this);

        for (Community parent : parents)
        {
            if (AuthorizeManager.authorizeActionBoolean(context, parent,
                    Constants.WRITE))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(context, parent,
                    Constants.ADD))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);
    }

    public int getType()
    {
        List<Community> parentList = new ArrayList<Community>();
        Community parent = getParentCommunity();

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public int countItems()
    {
        return dao.itemCount(this);
    }

    @Deprecated
    Community(Context context, org.dspace.storage.rdbms.TableRow row)
    {
        this(context, row.getIntColumn("community_id"));
    }

    @Deprecated
    public static Community find(Context context, int id)
    {
        return CommunityDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    public static Community create(Community parent, Context context)
            throws AuthorizeException
    {
        Community community =
            CommunityDAOFactory.getInstance(context).create();

        if (parent != null)
        {
            // No existing mapping, so add one
            TableRow mappingRow = DatabaseManager.create(ourContext,
                    "community2collection");

            mappingRow.setColumn("community_id", getID());
            mappingRow.setColumn("collection_id", c.getID());

            ourContext.addEvent(new Event(Event.ADD, Constants.COMMUNITY, getID(), Constants.COLLECTION, c.getID(), c.getHandle()));

            DatabaseManager.update(ourContext, mappingRow);
        }

        return community;
    }

    @Deprecated
    public static Community[] findAll(Context context)
    {
        CommunityDAO dao = CommunityDAOFactory.getInstance(context);
        List<Community> communities = dao.getCommunities();

        return (Community[]) communities.toArray(new Community[0]);
    }

    @Deprecated
    public static Community[] findAllTop(Context context)
    {
        CommunityDAO dao = CommunityDAOFactory.getInstance(context);
        List<Community> communities = dao.getTopLevelCommunities();

        log.info(LogManager.getHeader(ourContext, "add_subcommunity",
                "parent_comm_id=" + getID() + ",child_comm_id=" + c.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                "community2community",
                "SELECT * FROM community2community WHERE parent_comm_id= ? "+
                "AND child_comm_id= ? ",getID(), c.getID());
        
        if (!tri.hasNext())
        {
            // No existing mapping, so add one
            TableRow mappingRow = DatabaseManager.create(ourContext,
                    "community2community");

            mappingRow.setColumn("parent_comm_id", getID());
            mappingRow.setColumn("child_comm_id", c.getID());

            ourContext.addEvent(new Event(Event.ADD, Constants.COMMUNITY, getID(), Constants.COMMUNITY, c.getID(), c.getHandle()));

            DatabaseManager.update(ourContext, mappingRow);
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    @Deprecated
    public Community getParentCommunity()
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_collection",
                "community_id=" + getID() + ",collection_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2collection WHERE community_id= ? "+
                "AND collection_id= ? ", getID(), c.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, getID(), Constants.COLLECTION, c.getID(), c.getHandle()));

        // Is the community an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM community2collection WHERE collection_id= ? ",
                c.getID());

        if (!tri.hasNext())
        {
            return null;
        }
        return parents.get(0);
    }

    @Deprecated
    public Community[] getAllParents()
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_subcommunity",
                "parent_comm_id=" + getID() + ",child_comm_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2community WHERE parent_comm_id= ? " +
                " AND child_comm_id= ? ", getID(),c.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, getID(), Constants.COMMUNITY, c.getID(), c.getHandle()));

        // Is the subcommunity an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM community2community WHERE child_comm_id= ? ",
                c.getID());

        if (!tri.hasNext())
        {
            //make the right to remove the sub explicit because the implicit
            // relation
            //has been removed. This only has to concern the currentUser
            // because
            //he started the removal process and he will end it too.
            //also add right to remove from the subcommunity to remove it's
            // children.
            AuthorizeManager.addPolicy(ourContext, c, Constants.DELETE,
                    ourContext.getCurrentUser());
            AuthorizeManager.addPolicy(ourContext, c, Constants.REMOVE,
                    ourContext.getCurrentUser());

            // Orphan; delete it
            c.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    @Deprecated
    public Collection[] getCollections()
    {
        // Check authorisation
        // FIXME: If this was a subcommunity, it is first removed from it's
        // parent.
        // This means the parentCommunity == null
        // But since this is also the case for top-level communities, we would
        // give everyone rights to remove the top-level communities.
        // The same problem occurs in removing the logo
        if (!AuthorizeManager.authorizeActionBoolean(ourContext,
                getParentCommunity(), Constants.REMOVE))
        {
            AuthorizeManager
                    .authorizeAction(ourContext, this, Constants.DELETE);
        }

        // If not a top-level community, have parent remove me; this
        // will call delete() after removing the linkage
        Community parent = getParentCommunity();

        if (parent != null)
        {
            parent.removeSubcommunity(this);

            return;
        }

        log.info(LogManager.getHeader(ourContext, "delete_community",
                "community_id=" + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.COMMUNITY, getID(), getHandle()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove collections
        Collection[] cols = getCollections();

        for (int i = 0; i < cols.length; i++)
        {
            removeCollection(cols[i]);
        }

        // Remove subcommunities
        Community[] comms = getSubcommunities();

        for (int j = 0; j < comms.length; j++)
        {
            removeSubcommunity(comms[j]);
        }

        // Remove the logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Delete community row
        DatabaseManager.delete(ourContext, communityRow);
    }

    @Deprecated
    public Community[] getSubcommunities()
    {
        List<Community> communities = dao.getChildCommunities(this);
        return (Community[]) communities.toArray(new Community[0]);
    }

    @Deprecated
    public void addCollection(Collection collection)
        throws AuthorizeException
    {
        ArchiveManager.move(context, collection, null, this);
    }

    @Deprecated
    public void removeCollection(Collection collection)
        throws AuthorizeException
    {
        ArchiveManager.move(context, collection, this, null);
    }

    @Deprecated
    public void addSubcommunity(Community community) throws AuthorizeException
    {
        ArchiveManager.move(context, community, null, this);
    }

    @Deprecated
    public void removeSubcommunity(Community community)
        throws AuthorizeException
    {
        ArchiveManager.move(context, community, this, null);
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
    }

    @Deprecated
    public void delete() throws AuthorizeException
    {
        dao.delete(this.getID());
    }
}
