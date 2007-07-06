/*
 * CommunityDAOPostgres.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.content.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class CommunityDAOPostgres extends CommunityDAO
{
    public CommunityDAOPostgres(Context context)
    {
        if (context != null)
        {
            this.context = context;
            this.bitstreamDAO = BitstreamDAOFactory.getInstance(context);
            this.collectionDAO = CollectionDAOFactory.getInstance(context);
            this.identifierDAO =
                ExternalIdentifierDAOFactory.getInstance(context);
        }
    }

    @Override
    public Community create() throws AuthorizeException
    {
        try
        {
            UUID uuid = UUID.randomUUID();

            TableRow row = DatabaseManager.create(context, "community");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("community_id");
            
            return super.create(id, uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Community retrieve(int id)
    {
        Community community = super.retrieve(id);

        if (community != null)
        {
            return community;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "community", id);

            if (row == null)
            {
                return null;
            }

            community = new Community(context, id);
            populateCommunityFromTableRow(community, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(community);
            community.setExternalIdentifiers(identifiers);

            context.cache(community, id);

            return community;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Community retrieve(UUID uuid)
    {
        Community community = super.retrieve(uuid);

        if (community != null)
        {
            return community;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "community",
                    "uuid", uuid.toString());

            if (row == null)
            {
                return null;
            }

            int id = row.getIntColumn("community_id");
            community = new Community(context, id);
            populateCommunityFromTableRow(community, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(community);
            community.setExternalIdentifiers(identifiers);

            context.cache(community, id);

            return community;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Community community) throws AuthorizeException
    {
        super.update(community);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "community", community.getID());

            if (row != null)
            {
                update(community, row);
            }
            else
            {
                throw new RuntimeException("Didn't find community " +
                        community.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Community community, TableRow row)
        throws AuthorizeException
    {
        try
        {
            populateTableRowFromCommunity(community, row);

            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            // Delete community row
            DatabaseManager.delete(context, "community", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Community> getCommunities()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "community",
                    "SELECT community_id FROM community ORDER BY name");

            List<Community> communities = new ArrayList<Community>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("community_id");
                communities.add(retrieve(id));
            }

            return communities;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Community> getTopLevelCommunities()
    {
        try
        {
            // Get all communities that are not children
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "community",
                    "SELECT community_id FROM community " +
                    "WHERE NOT community_id IN " +
                    "(SELECT child_comm_id FROM community2community) " +
                    "ORDER BY name");

            List<Community> communities = new ArrayList<Community>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("community_id");
                communities.add(retrieve(id));
            }

            return communities;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Get the communities the given community or collection appears in. Note
     * that this only returns the immediate parents.
     */
    @Override
    public List<Community> getParentCommunities(DSpaceObject dso)
    {
        assert((dso instanceof Item) ||
               (dso instanceof Collection) ||
               (dso instanceof Community));

        try
        {
            TableRowIterator tri = null;
            if (dso instanceof Item)
            {
                 tri = DatabaseManager.queryTable(context,
                        "community",
                        "SELECT c.community_id " +
                        "FROM community c, community2item c2i " +
                        "WHERE c2i.community_id = c.community_id " +
                        "AND c2i.item_id = ? ",
                        dso.getID());
            }
            else if (dso instanceof Collection)
            {
                tri = DatabaseManager.queryTable(context, "community",
                        "SELECT c.community_id " +
                        "FROM community c, community2collection c2c " +
                        "WHERE c.community_id = c2c.community_id " +
                        "AND c2c.collection_id = ? ",
                        dso.getID());
            }
            else if (dso instanceof Community)
            {
                tri = DatabaseManager.queryTable(context, "community",
                        "SELECT c.community_id " +
                        "FROM community c, community2community c2c " +
                        "WHERE c2c.parent_comm_id = c.community_id " +
                        "AND c2c.child_comm_id = ? ",
                        dso.getID());
            }

            List<Community> parents = new ArrayList<Community>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("community_id");
                parents.add(retrieve(id));
            }

            return parents;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Community> getAllParentCommunities(DSpaceObject dso)
    {
        List<Community> parents = getParentCommunities(dso);
        for (Community parent : parents)
        {
            parents.addAll(getParentCommunities(parent));
        }
        return parents;
    }

    @Override
    public List<Community> getChildCommunities(Community community)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "community",
                    "SELECT c.community_id, c.name " +
                    "FROM community c, community2community c2c " +
                    "WHERE c2c.child_comm_id = c.community_id " +
                    "AND c2c.parent_comm_id = ? " +
                    "ORDER BY c.name",
                    community.getID());

            List<Community> communities = new ArrayList<Community>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("community_id");
                communities.add(retrieve(id));
            }

            return communities;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Straightforward utility method for counting the number of Items in the
     * given Community. There is probably a way to be smart about this. Also,
     * this strikes me as the kind of method that shouldn't really be in here.
     */
    @Override
    public int itemCount(Community community)
    {
    	int total = 0;

        for (Collection collection :
                collectionDAO.getChildCollections(community))
        {
        	total += collectionDAO.itemCount(collection);
        }

        for (Community child : getChildCommunities(community))
        {
        	total += itemCount(child);
        }

        return total;
    }

    /**
     * Create a database layer association between the given Community and
     * Collection.
     */
    @Override
    public void link(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        if (!linked(parent, child))
        {
            super.link(parent, child);

            try
            {
                if ((parent instanceof Community) &&
                    (child instanceof Collection))
                {
                    TableRow row =
                        DatabaseManager.create(context, "community2collection");

                    row.setColumn("community_id", parent.getID());
                    row.setColumn("collection_id", child.getID());

                    DatabaseManager.update(context, row);
                }
                else if ((parent instanceof Community) &&
                    (child instanceof Community))
                {
                    // Find out if mapping exists
                    TableRowIterator tri = DatabaseManager.queryTable(context,
                            "community2community",
                            "SELECT * FROM community2community " +
                            "WHERE parent_comm_id= ? "+
                            "AND child_comm_id= ? ",
                            parent.getID(), child.getID());

                    if (!tri.hasNext())
                    {
                        // No existing mapping, so add one
                        TableRow mappingRow = DatabaseManager.create(context,
                                "community2community");

                        mappingRow.setColumn("parent_comm_id", parent.getID());
                        mappingRow.setColumn("child_comm_id", child.getID());

                        DatabaseManager.update(context, mappingRow);
                    }
                }
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    /**
     * Remove any existing database layer association between the given Item
     * and Collection.
     */
    @Override
    public void unlink(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        if (linked(parent, child))
        {
            super.link(parent, child);

            try
            {
                if ((parent instanceof Community) &&
                    (child instanceof Collection))
                {
                    DatabaseManager.updateQuery(context,
                            "DELETE FROM community2collection " +
                            "WHERE community_id = ? AND collection_id = ? ",
                            parent.getID(), child.getID());
                }
                else if ((parent instanceof Community) &&
                    (child instanceof Community))
                {
                    DatabaseManager.updateQuery(context,
                            "DELETE FROM community2community " +
                            "WHERE parent_comm_id = ? AND child_comm_id = ? ",
                            parent.getID(), child.getID());
                }
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    /**
     * Determine whether or not there is an established link between the given
     * Community and Collection in the database.
     */
    private boolean linked(DSpaceObject parent, DSpaceObject child)
    {
        try
        {
            TableRowIterator tri = null;

            if ((parent instanceof Community) &&
                (child instanceof Collection))
            {
                tri = DatabaseManager.query(context,
                        "SELECT * FROM community2collection " +
                        "WHERE community_id = ? AND collection_id = ? ",
                        parent.getID(), child.getID());
            }
            else if ((parent instanceof Community) &&
                (child instanceof Community))
            {
                tri = DatabaseManager.query(context,
                        "SELECT * FROM community2community " +
                        "WHERE parent_comm_id = ? AND child_comm_id = ? ",
                        parent.getID(), child.getID());
            }
            else
            {
                throw new RuntimeException("Not allowed!");
            }

            return tri.hasNext();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateTableRowFromCommunity(Community community,
            TableRow row)
    {
        int id = community.getID();
        Bitstream logo = community.getLogo();

        if (logo == null)
        {
            row.setColumnNull("logo_bitstream_id");
        }
        else
        {
            row.setColumn("logo_bitstream_id", logo.getID());
        }

        // Now loop over all allowed metadata fields and set the value into the
        // TableRow.
        for (CommunityMetadataField field : CommunityMetadataField.values())
        {
            String value = community.getMetadata(field.toString());
            if (value == null)
            {
                row.setColumnNull(field.toString());
            }
            else
            {
                row.setColumn(field.toString(), value);
            }
        }
    }

    private void populateCommunityFromTableRow(Community c, TableRow row)
    {
        Bitstream logo = null;

        // Get the logo bitstream
        if (!row.isColumnNull("logo_bitstream_id"))
        {
            int id = row.getIntColumn("logo_bitstream_id");
            logo = bitstreamDAO.retrieve(id);
        }

        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        c.setIdentifier(new ObjectIdentifier(uuid));

        c.setLogoBitstream(logo);

        for (CommunityMetadataField field : CommunityMetadataField.values())
        {
            String value = row.getStringColumn(field.toString());
            if (value == null)
            {
                c.setMetadata(field.toString(), "");
            }
            else
            {
                c.setMetadata(field.toString(), value);
            }
        }
    }
}
