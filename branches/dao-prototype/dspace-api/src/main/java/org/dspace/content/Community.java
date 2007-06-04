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
import java.sql.SQLException;
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
import org.dspace.content.dao.CollectionDAO;        // Naughty!
import org.dspace.content.dao.CollectionDAOFactory; // Naughty!
import org.dspace.content.dao.CommunityDAO;         // Naughty!
import org.dspace.content.dao.CommunityDAOFactory;  // Naughty!
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;

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
    private CollectionDAO collectionDAO;

    private int id;
    private String identifier;
    private List<PersistentIdentifier> identifiers;
    private int logoID;
    private Bitstream logo;

    private Map<String, String> metadata;

    public Community(Context context, int id)
    {
        this.id = id;
        this.context = context;
        this.dao = CommunityDAOFactory.getInstance(context);
        this.collectionDAO = CollectionDAOFactory.getInstance(context);

        this.identifiers = new ArrayList<PersistentIdentifier>();
        this.metadata = new TreeMap<String, String>();
    }

    public int getID()
    {
        return id;
    }

    public void setID(int id)
    {
        this.id = id;
    }

    public ObjectIdentifier getIdentifier()
    {
        return new ObjectIdentifier(context, this);
    }

    /**
     * For those cases where you only want one, and you don't care what sort.
     */
    public PersistentIdentifier getPersistentIdentifier()
    {
        if (identifiers.size() > 0)
        {
            return identifiers.get(0);
        }
        else
        {
            throw new RuntimeException(
                    "I don't have any persistent identifiers.\n" + this);
        }
    }

    public List<PersistentIdentifier> getPersistentIdentifiers()
    {
        return identifiers;
    }

    public void addPersistentIdentifier(PersistentIdentifier identifier)
    {
        this.identifiers.add(identifier);
    }

    public void setPersistentIdentifiers(List<PersistentIdentifier> identifiers)
    {
        this.identifiers = identifiers;
    }

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
                value = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }
        metadata.put(field, value);
    }

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
    public Bitstream setLogo(InputStream is) throws AuthorizeException
    {
        try
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
                Bitstream newLogo = Bitstream.create(context, is);
                logo = newLogo;

                // now create policy for logo bitstream
                // to match our READ policy
                List policies = AuthorizeManager.getPoliciesActionFilter(
                        context, this, Constants.READ);
                AuthorizeManager.addPolicies(context, policies, newLogo);

                log.info(LogManager.getHeader(context, "set_logo",
                        "community_id=" + getID() + "logo_bitstream_id="
                                + newLogo.getID()));
            }
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        return logo;
    }

    public void setLogoBitstream(Bitstream logo)
    {
        this.logo = logo;
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     *
     * @return the new collection
     */
    public Collection createCollection() throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

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
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

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
        return Constants.COMMUNITY;
    }

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
        return CommunityDAOFactory.getInstance(context).create();
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

        return (Community[]) communities.toArray(new Community[0]);
    }

    @Deprecated
    public Community getParentCommunity()
    {
        // FIXME: Oh so Bad and Wrong, but it's at least as good as the old
        // implementation, so we'll let it slide.
        List<Community> parents = dao.getParentCommunities(this);
        if (parents.size() == 0)
        {
            return null;
        }
        return parents.get(0);
    }

    @Deprecated
    public Community[] getAllParents()
    {
        List<Community> parents = dao.getAllParentCommunities(this);
        return (Community[]) parents.toArray(new Community[0]);
    }

    @Deprecated
    public Collection[] getCollections()
    {
        List<Collection> collections = collectionDAO.getChildCollections(this);
        return (Collection[]) collections.toArray(new Collection[0]);
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
