/*
 * CommunityDAO.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.content.uri.dao.ExternalIdentifierDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.search.DSIndexer;

/**
 * @author James Rutherford
 */
public abstract class CommunityDAO extends ContentDAO
{
    protected static Logger log = Logger.getLogger(CommunityDAO.class);

    protected Context context;
    protected BitstreamDAO bitstreamDAO;
    protected CollectionDAO collectionDAO;
    protected GroupDAO groupDAO;
    protected ExternalIdentifierDAO identifierDAO;

    /**
     * The allowed metadata fields for Communities are defined in the following
     * enum. This should make reading / writing all metadatafields a lot less
     * error-prone, not to mention concise and tidy!
     *
     * FIXME: Do we want this exposed anywhere else? Probably not...
     */
    protected enum CommunityMetadataField
    {
        NAME ("name"),
        SHORT_DESCRIPTION ("short_description"),
        INTRODUCTORY_TEXT ("introductory_text"),
        COPYRIGHT_TEXT ("copyright_text"),
        SIDE_BAR_TEXT ("side_bar_text");

        private String name;

        private CommunityMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public abstract Community create() throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final Community create(Community community)
        throws AuthorizeException
    {
        // Only administrators and adders can create communities
        if (!(AuthorizeManager.isAdmin(context)))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        // Create a default persistent identifier for this Community, and
        // add it to the in-memory Community object.
        ExternalIdentifier identifier = identifierDAO.create(community);
        community.addExternalIdentifier(identifier);

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = groupDAO.retrieve(0);

        ResourcePolicy policy = ResourcePolicy.create(context);
        policy.setResource(community);
        policy.setAction(Constants.READ);
        policy.setGroup(anonymousGroup);
        policy.update();

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + community.getID()) + ",uri=" +
                community.getExternalIdentifier().getCanonicalForm());

        update(community);

        return community;
    }

    public Community retrieve(int id)
    {
        return (Community) context.fromCache(Community.class, id);
    }

    public Community retrieve(UUID uuid)
    {
        return null;
    }

    public void update(Community community) throws AuthorizeException
    {
        // Check authorization
        community.canEdit();

        log.info(LogManager.getHeader(context, "update_community",
                "community_id=" + community.getID()));

        try
        {
            DSIndexer.reIndexContent(context, community);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        // FIXME: Do we need to iterate through child Communities /
        // Collecitons to update / re-index? Probably not.
    }

    public void delete(int id) throws AuthorizeException
    {
        try
        {
            Community community = retrieve(id);
            update(community); // Sync in-memory object before removal

            context.removeCached(community, id);

            // Check authorisation
            // FIXME: If this was a subcommunity, it is first removed from it's
            // parent.
            // This means the parentCommunity == null
            // But since this is also the case for top-level communities, we would
            // give everyone rights to remove the top-level communities.
            // The same problem occurs in removing the logo
            for (Community parent : getParentCommunities(community))
            {
                if (!AuthorizeManager.authorizeActionBoolean(context, parent,
                            Constants.REMOVE))
                {
                    AuthorizeManager.authorizeAction(context, community,
                            Constants.DELETE);
                }
            }

            // If not a top-level community, have parent remove me; this
            // will call delete() after removing the linkage
            // FIXME: Maybe it shouldn't though.
            // FIXME: This is totally broken.
            for (Community parent : getParentCommunities(community))
            {
                unlink(parent, community);
            }

            log.info(LogManager.getHeader(context, "delete_community",
                    "community_id=" + id));

            // Remove collections
            for (Collection child :
                    collectionDAO.getChildCollections(community))
            {
                unlink(community, child);
            }

            // Remove subcommunities
            for (Community child : getChildCommunities(community))
            {
                unlink(community, child);
            }

            // FIXME: This won't delete the logo. Needs more
            // bitstreamDAO.delete(logoId)
            community.setLogo(null);

            try
            {
                // remove from the search index
                DSIndexer.unIndexContent(context, community);

            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }

            // Remove all authorization policies
            AuthorizeManager.removeAllPolicies(context, community);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
    }

    public abstract List<Community> getCommunities();
    public abstract List<Community> getTopLevelCommunities();
    public abstract List<Community> getChildCommunities(Community community);

    public abstract List<Community> getParentCommunities(DSpaceObject dso);
    public abstract List<Community> getAllParentCommunities(DSpaceObject dso);

    public void link(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        assert(parent instanceof Community);
        assert((child instanceof Community) || (child instanceof Collection));

        if ((parent instanceof Community) &&
            (child instanceof Collection))
        {
            AuthorizeManager.authorizeAction(context,
                    (Community) parent, Constants.ADD);

            log.info(LogManager.getHeader(context, "add_collection",
                        "community_id=" + parent.getID() +
                        ",collection_id=" + child.getID()));
        }
        else if ((parent instanceof Community) &&
            (child instanceof Community))
        {
            AuthorizeManager.authorizeAction(context, parent,
                    Constants.ADD);

            log.info(LogManager.getHeader(context, "add_subcommunity",
                    "parent_comm_id=" + parent.getID() +
                    ",child_comm_id=" + child.getID()));
        }
    }

    public void unlink(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        assert(parent instanceof Community);
        assert((child instanceof Community) || (child instanceof Collection));

        if ((parent instanceof Community) &&
            (child instanceof Collection))
        {
            AuthorizeManager.authorizeAction(context, child,
                    Constants.REMOVE);

            log.info(LogManager.getHeader(context, "remove_collection",
                    "collection_id = " + parent.getID() +
                    ",item_id = " + child.getID()));
        }
        else if ((parent instanceof Community) &&
            (child instanceof Community))
        {
            AuthorizeManager.authorizeAction(context, child,
                    Constants.REMOVE);

            log.info(LogManager.getHeader(context,
                    "remove_subcommunity",
                    "parent_comm_id = " + parent.getID() +
                    ",child_comm_id = " + child.getID()));
        }
        else
        {
            throw new RuntimeException("Not allowed!");
        }
    }

    // Everything below this line is debatable & needs rethinking

    public abstract int itemCount(Community community);
}

