/*
 * Collection.java
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Browse;
import org.dspace.core.ArchiveManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.content.dao.CollectionDAO;        // Naughty!
import org.dspace.content.dao.CollectionDAOFactory; // Naughty!
import org.dspace.content.dao.CommunityDAO;         // Naughty!
import org.dspace.content.dao.CommunityDAOFactory;  // Naughty!
import org.dspace.content.dao.ItemDAO;              // Naughty!
import org.dspace.content.dao.ItemDAOFactory;       // Naughty!
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.workflow.WorkflowItem;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 *
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class Collection extends DSpaceObject
{
    private static Logger log = Logger.getLogger(Collection.class);

    private Context context;
    private CollectionDAO dao;
    private ItemDAO itemDAO;
    private CommunityDAO communityDAO;

    private int id;
    private String identifier;
    private List<PersistentIdentifier> identifiers;
    private String license;
    private Bitstream logo;
    private Item templateItem;

    private Map<String, String> metadata;

    // FIXME: Maybe we should be smart about this and only store the IDs. OTOH,
    // Groups aren't that heavyweight, and cacheing them may be a good thing
    // for performance. A proxy implementation that retrieved them on demand
    // and *then* cached them could be a good idea though.
    private Group[] workflowGroups;
    private Group submitters;
    private Group admins;

    public Collection(Context context, int id)
    {
        this.id = id;
        this.context = context;
        this.dao = CollectionDAOFactory.getInstance(context);
        this.itemDAO = ItemDAOFactory.getInstance(context);
        this.communityDAO = CommunityDAOFactory.getInstance(context);

        this.identifiers = new ArrayList<PersistentIdentifier>();
        this.metadata = new TreeMap<String, String>();
        this.workflowGroups = new Group[3];
    }

    public int getID()
    {
        return id;
    }

    public void setID(int id)
    {
        this.id = id;
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

    public Group getSubmitters()
    {
        return submitters;
    }

    public void setSubmitters(Group submitters)
    {
        this.submitters = submitters;
    }

    public Group createSubmitters() throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        try
        {
            if (submitters == null)
            {
                submitters = Group.create(context); // I'd rather do it with DAOs
                submitters.setName("COLLECTION_" + getID() + "_SUBMIT");
                submitters.update();
            }

            setSubmitters(submitters);

            AuthorizeManager.addPolicy(context, this, Constants.ADD, submitters);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        return submitters;
    }

    /**
     * Get the in_archive items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     */
    public ItemIterator getItems()
    {
        List<Item> allItems = itemDAO.getItemsByCollection(this);
        List<Integer> items = new ArrayList<Integer>();

        Iterator<Item> iterator = allItems.iterator();

        while (iterator.hasNext())
        {
            Item item = iterator.next();
            if (item.isArchived())
            {
                items.add(item.getID());
            }
        }

        return new ItemIterator(this.context, items);
    }

    /**
     * Get all the items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     */
    public ItemIterator getAllItems()
    {
        List<Integer> items = new ArrayList<Integer>();

        for (Item item : itemDAO.getItemsByCollection(this))
        {
            items.add(item.getID());
        }

        return new ItemIterator(this.context, items);
    }

    public String getMetadata(String field)
    {
        // FIXME: This is a little naughty, but in technically, the license is
        // actually metadata.
        if ("license".equals(field))
        {
            return getLicense();
        }
        return metadata.get(field);
    }

    public void setMetadata(String field, String value)
    {
        // FIXME: This is a little naughty, but in technically, the license is
        // actually metadata.
        if ("license".equals(field))
        {
            setLicense(value);
            return;
        }

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

    /**
     * Get the logo for the collection. <code>null</code> is return if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    /**
     * Give the collection a logo. Passing in <code>null</code> removes any
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
     * @throws AuthorizeException
     * @throws IOException
     */
    public Bitstream setLogo(InputStream is) throws AuthorizeException,
            IOException
    {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                context, this, Constants.DELETE)))
        {
            canEdit();
        }

        try
        {
            // First, delete any existing logo
            if (logo != null)
            {
                log.info(LogManager.getHeader(context, "remove_logo",
                        "collection_id=" + getID()));
                logo.delete();
                logo = null;
            }

            if (is == null)
            {
                log.info(LogManager.getHeader(context, "remove_logo",
                        "collection_id=" + getID()));
                logo = null;
            }
            else
            {
                logo = Bitstream.create(context, is);

                // now create policy for logo bitstream
                // to match our READ policy
                List policies = AuthorizeManager.getPoliciesActionFilter(
                        context, this, Constants.READ);
                AuthorizeManager.addPolicies(context, policies, logo);

                log.info(LogManager.getHeader(context, "set_logo",
                        "collection_id=" + getID() +
                        ",logo_bitstream_id=" + logo.getID()));
            }
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
     * Get the the workflow group corresponding to a particular workflow step.
     * This returns <code>null</code> if there is no group associated with
     * this collection for the given step.
     *
     * @param step
     *            the workflow step (1-3)
     *
     * @return the group of reviewers or <code>null</code>
     */
    public Group getWorkflowGroup(int step)
    {
        return workflowGroups[step - 1];
    }

    public Group[] getWorkflowGroups()
    {
        return workflowGroups;
    }

    /**
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated
     * group for that workflow step; any existing group is NOT deleted.
     *
     * @param step
     *            the workflow step (1-3)
     * @param g
     *            the new workflow group, or <code>null</code>
     */
    public void setWorkflowGroup(int step, Group g)
    {
        workflowGroups[step - 1] = g;
    }

    /**
     * Create a workflow group for the given step if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that while the new group is created in the database, the association
     * between the group and the collection is not written until
     * <code>update</code> is called.
     *
     * @param step
     *            the step (1-3) of the workflow to create or get the group for
     *
     * @return the workflow group associated with this collection
     * @throws AuthorizeException
     */
    public Group createWorkflowGroup(int step) throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        if (workflowGroups[step - 1] == null)
        {
            Group g = null;
            try
            {
                g = Group.create(context);
                g.setName("COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
                g.update();
                setWorkflowGroup(step, g);

                AuthorizeManager.addPolicy(context, this, Constants.ADD, g);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
        return workflowGroups[step - 1];
    }

    public Group getAdministrators()
    {
        return admins;
    }

    public void setAdministrators(Group admins)
    {
        this.admins = admins;
    }

    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     *
     * @return the default group of editors associated with this collection
     * @throws AuthorizeException
     */
    // FIXME: Need to do this cleanly without interrogating the data layer
    // directly. We need DAOs for Groups too!
    public Group createAdministrators() throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        try
        {
            if (admins == null)
            {
                admins = Group.create(context);
                admins.setName("COLLECTION_" + getID() + "_ADMIN");
                admins.update();
            }

            AuthorizeManager.addPolicy(context, this,
                    Constants.COLLECTION_ADMIN, admins);

            // administrators also get ADD on the submitter group
            if (submitters != null)
            {
                AuthorizeManager.addPolicy(context, submitters, Constants.ADD,
                        admins);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        // register this as the admin group
        this.admins = admins;

        return admins;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection. If the collection does not have a specific license, the
     * site-wide default is returned.
     *
     * @return the license for this collection
     */
    public String getLicense()
    {
        if ((license == null) || license.equals(""))
        {
            // Fallback to site-wide default
            license = ConfigurationManager.getDefaultSubmissionLicense();
        }

        return license;
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     *
     * @param license the license, or <code>null</code>
     */
    public void setLicense(String license)
    {
        this.license = license;
    }

    /**
     * Find out if the collection has a custom license
     *
     * @return <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense()
    {
        return ((license != null) && !license.equals(""));
    }

    public Item getTemplateItem()
    {
        return templateItem;
    }

    public void setTemplateItem(Item templateItem)
    {
        this.templateItem = templateItem;
    }

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @throws AuthorizeException
     */
    public void createTemplateItem() throws AuthorizeException
    {
        // Check authorisation
        canEdit();

        if (templateItem == null)
        {
            templateItem = itemDAO.create();

            log.info(LogManager.getHeader(context, "create_template_item",
                    "collection_id=" + getID() +
                    ",template_item_id=" + templateItem.getID()));
        }
    }

    /**
     * Remove the template item for this collection, if there is one. Note that
     * since this has to remove the old template item ID from the collection
     * record in the database, the colletion record will be changed, including
     * any other changes made; in other words, this method does an
     * <code>update</code>.
     *
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeTemplateItem() throws AuthorizeException, IOException
    {
        // Check authorisation
        canEdit();

        if (templateItem != null)
        {
            log.info(LogManager.getHeader(context, "remove_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + templateItem.getID()));

            itemDAO.delete(templateItem.getID());
            templateItem = null;
        }
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
        Community[] parents = getCommunities();

        for (int i = 0; i < parents.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(context, parents[i],
                    Constants.WRITE))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(context, parents[i],
                    Constants.ADD))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAnyOf(context, this, new int[] {
                Constants.WRITE, Constants.COLLECTION_ADMIN });
    }

    public int getType()
    {
        return Constants.COLLECTION;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    Collection(Context context, org.dspace.storage.rdbms.TableRow row)
    {
        this(context, row.getIntColumn("collection_id"));
    }

    @Deprecated
    static Collection create(Context context) throws AuthorizeException
    {
        return CollectionDAOFactory.getInstance(context).create();
    }

    @Deprecated
    public static Collection find(Context context, int id)
    {
        return CollectionDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    public static Collection[] findAll(Context context)
    {
        CollectionDAO dao = CollectionDAOFactory.getInstance(context);
        List<Collection> collections = dao.getCollections();

        return (Collection[]) collections.toArray(new Collection[0]);
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
    }

    @Deprecated
    void delete() throws AuthorizeException
    {
        dao.delete(this.getID());
    }

    @Deprecated
    public void addItem(Item item) throws AuthorizeException
    {
        ArchiveManager.move(context, item, null, this);
    }

    @Deprecated
    public void removeItem(Item item) throws AuthorizeException, IOException
    {
        ArchiveManager.move(context, item, this, null);
    }

    @Deprecated
    public Community[] getCommunities()
    {
        List<Community> communities = communityDAO.getParentCommunities(this);
        return (Community[]) communities.toArray(new Community[0]);
    }

    @Deprecated
    public static Collection[] findAuthorized(Context context, Community parent,
            int actionID)
    {
        CollectionDAO dao = CollectionDAOFactory.getInstance(context);
        List<Collection> collections =
            dao.getCollectionsByAuthority(parent, actionID);

        return (Collection[]) collections.toArray(new Collection[0]);
    }

    @Deprecated
    public int countItems()
    {
        return dao.itemCount(this);
    }
}
