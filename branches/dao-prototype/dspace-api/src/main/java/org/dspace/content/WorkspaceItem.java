/*
 * WorkspaceItem.java
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Class representing an item in the process of being submitted by a user.
 *
 * FIXME: this class could benefit from a proxy so the Collection and Item
 * aren't fully instantiated unless explicitly required. Could be wasted
 * effort, however, as the number of workspace items in memory at any given
 * time will typically be very low.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class WorkspaceItem implements InProgressSubmission
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    private Context context;
    private WorkspaceItemDAO dao;
    private ItemDAO itemDAO;
    private CollectionDAO collectionDAO;

    private int id;
    private ObjectIdentifier oid;
    private boolean hasMultipleFiles;
    private boolean hasMultipleTitles;
    private boolean publishedBefore;
    private int stageReached;

    private Item item;
    private Collection collection;

    public WorkspaceItem(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = WorkspaceItemDAOFactory.getInstance(context);
        itemDAO = ItemDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);

        context.cache(this, id);
    }
    
    public int getID()
    {
        return id;
    }

    public ObjectIdentifier getIdentifier()
    {
        return oid;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    public int getStageReached()
    {
        return stageReached;
    }

    public void setStageReached(int stageReached)
    {
        this.stageReached = stageReached;
    }

        // Create an item
        Item i = Item.create(c);
        i.setSubmitter(c.getCurrentUser());

        // Now create the policies for the submitter and workflow
        // users to modify item and contents
        // contents = bitstreams, bundles
        // FIXME: icky hardcoded workflow steps
        Group step1group = coll.getWorkflowGroup(1);
        Group step2group = coll.getWorkflowGroup(2);
        Group step3group = coll.getWorkflowGroup(3);

        EPerson e = c.getCurrentUser();

        // read permission
        AuthorizeManager.addPolicy(c, i, Constants.READ, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.READ, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.READ, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.READ, step3group);
        }

        // write permission
        AuthorizeManager.addPolicy(c, i, Constants.WRITE, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.WRITE, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.WRITE, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.WRITE, step3group);
        }

        // add permission
        AuthorizeManager.addPolicy(c, i, Constants.ADD, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.ADD, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.ADD, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.ADD, step3group);
        }

        // remove contents permission
        AuthorizeManager.addPolicy(c, i, Constants.REMOVE, e);

        if (step1group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step1group);
        }

        if (step2group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step2group);
        }

        if (step3group != null)
        {
            AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step3group);
        }

        // Copy template if appropriate
        Item templateItem = coll.getTemplateItem();

        if (template && (templateItem != null))
        {
            DCValue[] md = templateItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (int n = 0; n < md.length; n++)
            {
                i.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
                        md[n].value);
            }
        }

        i.update();

        // Create the workspace item row
        TableRow row = DatabaseManager.create(c, "workspaceitem");

        row.setColumn("item_id", i.getID());
        row.setColumn("collection_id", coll.getID());

        log.info(LogManager.getHeader(c, "create_workspace_item",
                "workspace_item_id=" + row.getIntColumn("workspace_item_id")
                        + "item_id=" + i.getID() + "collection_id="
                        + coll.getID()));

        DatabaseManager.update(c, row);

        WorkspaceItem wi = new WorkspaceItem(c, row);

        return wi;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Collection getCollection()
    {
        return collection;
    }

    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    public EPerson getSubmitter()
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return hasMultipleFiles;
    }

    public void setMultipleFiles(boolean hasMultipleFiles)
    {
        this.hasMultipleFiles = hasMultipleFiles;
    }

    public boolean hasMultipleTitles()
    {
        return hasMultipleTitles;
    }

    public void setMultipleTitles(boolean hasMultipleTitles)
    {
        this.hasMultipleTitles = hasMultipleTitles;
    }

    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    public void setPublishedBefore(boolean publishedBefore)
    {
        // Authorisation is checked by the item.update() method below

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public boolean equals(WorkspaceItem wsi)
    {
        if (getID() == wsi.getID())
        {
            return true;
        }

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the epersongroup2workspaceitem row first since it refers
        // to workspaceitem ID
        deleteEpersonGroup2WorkspaceItem();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);

        // Delete item
        item.delete();
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /** Deprecated by the introduction of DAOs */
    @Deprecated
    WorkspaceItem(Context context, org.dspace.storage.rdbms.TableRow row)
    {
        // Check authorisation. We check permissions on the enclosed item.
        AuthorizeManager.authorizeAction(ourContext, item, Constants.WRITE);

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //        deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);
    }

    @Deprecated
    public static WorkspaceItem create(Context context, Collection collection,
            boolean template)
        throws AuthorizeException, IOException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.create(collection, template);
    }

    @Deprecated
    public static WorkspaceItem find(Context context, int id)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.retrieve(id);
    }

    @Deprecated
    public void update() throws AuthorizeException, IOException
    {
        dao.update(this);
    }

    @Deprecated
    public void deleteWrapper() throws AuthorizeException, IOException
    {
        dao.delete(getID());
    }

    @Deprecated
    public void deleteAll() throws AuthorizeException,
            IOException
    {
        dao.deleteAll(getID());
    }

    @Deprecated
    public static WorkspaceItem[] findAll(Context context)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems();

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(ep);

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(c);

        return (WorkspaceItem[]) wsItems.toArray(new WorkspaceItem[0]);
    }
}
