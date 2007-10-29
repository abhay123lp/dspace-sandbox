/*
 * IngestionWrapper.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2005/04/20 14:22:34 $
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

import java.sql.SQLException;
import java.net.URI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.authorize.AuthorizeException;

/**
 * An instance of this class is a placeholder used to collect
 * information about a archival DSpace Object (i.e.  Item, Collection,
 * Community) in the process of being ingested or built.  It is
 * necessary because a new archival object can only be instantiated when
 * its parent is known, and that (primary) parent cannot be changed
 * later.  This wrapper collects the parent and other instantiation
 * parameters before creating the new object.
 * <p>
 * Note that WorkspaceItem is now a subclass of IngestionWrapper, which
 * only wraps Items; it has more complete and meaningful implementations
 * of some of its methods.  IngestionWrapper is used to create
 * Collections and Communities.
 *
 * @author Larry Stone
 * @version $Revision: 1.12 $
 */
public class IngestionWrapper
       extends DSpaceObject
{
    /** owner or parent of wrapped object */
    protected DSpaceObject parent = null;

    /** under-construction object */
    protected DSpaceObject wrapped = null;

    /** authenticated user doing the submission */
    protected EPerson submitter = null;

    /** use template when creating new object?  (Item only, for now) */
    protected boolean template = false;

    /** pre-determined persistent ID for submission */
    protected String handle = null;

    /** withdraw flag for items only. */
    protected boolean withdrawn = false;

    private List<DSpaceObject> objectsToDelete = new ArrayList<DSpaceObject>();

    // our DSpace Object type.
    private int type = -1;

    /**
     * Construct a wrapper for a given object type.
     */
    protected IngestionWrapper(int wrappedType)
    {
        super();
        switch (wrappedType)
        {
            case Constants.ITEM:
                type = Constants.INGESTION_ITEM;
                break;
            case Constants.COLLECTION:
                type = Constants.INGESTION_COLLECTION;
                break;
            case Constants.COMMUNITY:
                type = Constants.INGESTION_COMMUNITY;
                break;
        }
    }

    /**
     * Static create method to be consistent with DSpaceObject convention.
     */
    public static IngestionWrapper create(int wrappedType)
    {
        return new IngestionWrapper(wrappedType);
    }

    /**
     * Instantiate the wrapped object to be ingested; only call this
     * once the parent (and perhaps other metadata) has been established.
     */
    public IngestionWrapper createWrappedObject(Context context)
        throws AuthorizeException, SQLException, IOException
    {
        switch (type)
        {
            case Constants.INGESTION_COLLECTION:
                wrapped = ((Community)parent).createCollection(handle);
                return this;

            case Constants.INGESTION_COMMUNITY:
                // top-level community?
                if (parent == null || parent.getType() == Constants.SITE)
                    wrapped = Community.create(null, context, handle);
                else
                    wrapped = ((Community)parent).createSubcommunity(handle);
                return this;

            case Constants.INGESTION_ITEM:
                if (submitter == null)
                    submitter = context.getCurrentUser();
                IngestionWrapper result = WorkspaceItem.create(context, (Collection)parent, template, submitter, handle);
                result.setWithdrawn(withdrawn);
                return result;
        }
        return null;
    }

    /**
     * Get the type of this object, found in Constants.
     * Note this is NOT the type of the wrapped object.
     *
     * @return type of the object
     */
    public int getType()
    {
        return type;
    }

    /**
     * Get the internal ID (database primary key) of this object,
     * which actually delegates to the wrapped object because this does
     * not exist in the database.  This is a poor placeholder for
     * the proper implementation in WorkspaceItem subclass.
     *
     * @return internal ID of object
     */
    public int getID()
    {
        return wrapped.getID();
    }

    /**
     * Get the Handle set in this wrapper.  May return <code>null</code>
     *
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public String getHandle()
    {
        return handle;
    }

    /**
     * Sets predetermined persistent ID (Handle) for new archival object.
     *
     * @param hdl handle under which new submission will be created.
     */
    public void setHandle(String hdl)
    {
        handle = hdl;
    }

    /**
     * @returns parent (owner) under which new submission will be created.
     */
    public DSpaceObject getParent()
    {
        return parent;
    }

    /**
     * Sets parent for new submission.
     *
     * @param parent (owner) under which new submission will be created.
     */
    public void setParent(DSpaceObject value)
    {
        parent = value;
    }

    /**
     * @returns submitter eperson who is responsible for submitting this
     *  object.
     */
    public EPerson getSubmitter()
        throws SQLException
    {
        return submitter;
    }

    /**
     * Sets the submitter, i.e. EPerson submitting this object.
     *
     * @param submitter eperson who is responsible for submitting this
     *  object.
     */
    public void setSubmitter(EPerson value)
    {
        submitter = value;
    }

    /**
     * Set choice of whether or not to use parent's template Item when
     * creating a new Item.
     */
    public void setTemplate(boolean value)
    {
        template = value;
    }

    /**
     * Set "isWithdrawn" state of an Item.  This is required when
     * e.g. re-ingesting an AIP which may have been in the withdrawn state.
     */
    public void setWithdrawn(boolean value)
    {
        withdrawn = value;
    }

    /**
     * Return state of wrapper's withdrawn flag.
     */
    public boolean getWithdrawn()
    {
        return withdrawn;
    }

    /**
     * @return the wrapped object.  may be null.
     */
    public DSpaceObject getWrappedObject()
    {
        return wrapped;
    }

    /**
     * Update wraped object's state in the database.
     */
    public void update()
        throws SQLException, AuthorizeException, IOException
    {
        if (wrapped != null)
        {
            switch (wrapped.getType())
            {
                case Constants.BITSTREAM:
                    ((Bitstream)wrapped).update();
                    break;
                case Constants.ITEM:
                    ((Item)wrapped).update();
                    break;
                case Constants.COLLECTION:
                    ((Collection)wrapped).update();
                    break;
                case Constants.COMMUNITY:
                    ((Community)wrapped).update();
                    break;
            }
        }
    }

    /**
     * Delete wrapped object, i.e. after failure in the ingestion process.
     * Should really only be needed when ingestion is spread across multiple
     * transactions, which is only possible with Item subclass; otherwise
     * the transaction can just be aborted and rolled back.
     */
    public void deleteAll() throws SQLException, AuthorizeException,
            IOException
    {
        if (wrapped != null)
        {
            switch (type)
            {
                case Constants.INGESTION_COLLECTION:
                    ((Community)parent).removeCollection((Collection)wrapped);
                    break;
             
                case Constants.INGESTION_COMMUNITY:
                    // parent may be Site if it's top-level
                    if (parent.getType() == Constants.COMMUNITY)
                        ((Community)parent).removeSubcommunity((Community)wrapped);
                    else
                        ((Community)wrapped).delete();
                    break;
             
                // should never execute this, since WorkspaceItem overrides deleteAll()
                case Constants.INGESTION_ITEM:
                    throw new IOException("What am I doing here?  This should be a WorkspaceItem.");
            }
        }
        wrapped = null;
        cleanup();
    }

    /**
     * delete the contents of this object.
     */
    public void delete() throws SQLException, AuthorizeException,
            IOException
    {
        deleteAll();
    }

    /**
     * Collect temporary objects (i.e. temporary Bitstreams constructed to hold
     * metadata or package streams) to be discarded after a successful
     * ingestion.  See cleanup()
     */
    public void addObjectToDelete(DSpaceObject dso)
    {
        objectsToDelete.add(dso);
    }

    /**
     * Delete the temporary objects tracked by addObjectToDelete().
     */
    public void cleanup()
        throws SQLException, AuthorizeException, IOException
    {
        for (DSpaceObject dso : objectsToDelete)
            deleteDSpaceObject(dso);
    }

    // compensation for lack of a generic delete() method on DSpaceObject.
    // (for which there are good reasons..)
    private void deleteDSpaceObject(DSpaceObject dso)
        throws SQLException, AuthorizeException, IOException
    {
        switch (dso.getType())
        {
            case Constants.BITSTREAM:
                ((Bitstream)dso).delete();
                break;
            case Constants.ITEM:
                ((Item)dso).delete();
                break;
            case Constants.COLLECTION:
                ((Collection)dso).delete();
                break;
            case Constants.COMMUNITY:
                ((Community)dso).delete();
                break;
        }
    }

    /**
     * no proper name, but this method is required by DSpaceObject.
     */
    public String getName()
    {
        return null;
    }
}
