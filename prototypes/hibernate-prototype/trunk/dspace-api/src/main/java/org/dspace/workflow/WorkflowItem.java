/*
 * WorkflowItem.java
 *
 * Version: $Revision: 2417 $
 *
 * Date: $Date: 2007-12-10 18:00:07 +0100 (lun, 10 dic 2007) $
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
package org.dspace.workflow;

import java.io.IOException;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.dao.WorkflowItemDAO;
import org.dspace.workflow.dao.WorkflowItemDAOFactory;

/**
 * Class representing an item going through the workflow process in DSpace
 * 
 * @author Robert Tansley
 * @version $Revision: 2417 $
 */
@Entity
public class WorkflowItem implements InProgressSubmission
{
    /** log4j category */
    private static Logger log = Logger.getLogger(WorkflowItem.class);

    private Context context;
    //private WorkflowItemDAO dao;

    private Item item;

    /** The collection the item is being submitted to */
    private Collection collection;

    /** EPerson owning the current state */
    private EPerson owner;

    private int id;
    private ObjectIdentifier oid;
    private int state;
    private boolean multipleFilesOwner; //multipleFiles
    private boolean multipleTitlesOwner; //multipleTitles
    private boolean publishedBefore;

    public WorkflowItem(Context context, int id)
    {
        this.context = context;
        this.id = id;

        //dao = WorkflowItemDAOFactory.getInstance(context);

        context.cache(this, id);
    }
    
    protected WorkflowItem() {}
    
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name="workflow_id")
    public int getId()
    {
        return id;
    }

    @Transient //FIXME controllare questo aspetto, discorso degli identificatori
    public ObjectIdentifier getIdentifier()
    {
        return oid;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    /**
     * get owner of WorkflowItem
     * 
     * @return EPerson owner
     */
    @ManyToOne
    @JoinColumn(name="owner")
    public EPerson getOwner()
    {
        return owner;
    }

    public void setOwner(EPerson owner)
    {
        this.owner = owner;
    }

    /**
     * Get state of WorkflowItem, as defined in <code>WorkflowManager</code>.
     */
    @Column(name="state")
    public int getState()
    {
        return state;
    }

    /**
     * Set the state of WorkflowItem.
     * 
     * @param state new state (from <code>WorkflowManager</code>)
     */
    public void setState(int state)
    {
        this.state = state;
    }

    // InProgressSubmission methods
    @OneToOne
    public Item getItem()
    {
        return item;
    }
    
    public void setItem(Item item)
    {
        this.item = item;
    }

    @ManyToOne
    public Collection getCollection()
    {
        return collection;
    }
    
    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    @Transient
    public EPerson getSubmitter()
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return multipleFilesOwner;
    }

    public void setMultipleFiles(boolean multipleFiles)
    {
        this.multipleFilesOwner = multipleFiles;
    }

    public boolean hasMultipleTitles()
    {
        return multipleTitlesOwner;
    }

    public void setMultipleTitles(boolean b)
    {
        this.multipleTitlesOwner = multipleTitlesOwner;
    }

    @Column(name="published_before")
    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    public void setPublishedBefore(boolean publishedBefore)
    {
        this.publishedBefore = publishedBefore;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public void update() throws IOException, AuthorizeException
    {
//        dao.update(this);
    }

    @Deprecated
    public void deleteWrapper() throws IOException, AuthorizeException
    {
//        dao.delete(getId());
    }

    @Deprecated
    public static WorkflowItem find(Context context, int id)
    {
//        WorkflowItemDAO dao = WorkflowItemDAOFactory.getInstance(context);
//        return dao.retrieve(id);
        return null;
    }

    @Deprecated
    public static WorkflowItem[] findAll(Context c)
    {
//        WorkflowItemDAO dao = WorkflowItemDAOFactory.getInstance(c);
//        List<WorkflowItem> wfItems = dao.getWorkflowItems();

//        return (WorkflowItem[]) wfItems.toArray(new WorkflowItem[0]);
        return null;
    }

    @Deprecated
    public static WorkflowItem[] findByEPerson(Context context, EPerson e)
    {
//        WorkflowItemDAO dao = WorkflowItemDAOFactory.getInstance(context);
//        List<WorkflowItem> wfItems = dao.getWorkflowItemsBySubmitter(e);

//        return (WorkflowItem[]) wfItems.toArray(new WorkflowItem[0]);
        return null;
    }

    @Deprecated
    public static WorkflowItem[] findByCollection(Context context, Collection c)
    {
//        WorkflowItemDAO dao = WorkflowItemDAOFactory.getInstance(context);
//        List<WorkflowItem> wfItems = dao.getWorkflowItems(c);

//        return (WorkflowItem[]) wfItems.toArray(new WorkflowItem[0]);
        return null;
    }

    @Column(name="multiple_files")
    public boolean isMultipleFilesOwner()
    {
        return multipleFilesOwner;
    }

    public void setMultipleFilesOwner(boolean multipleFilesOwner)
    {
        this.multipleFilesOwner = multipleFilesOwner;
    }

    @Column(name="multiple_titles")
    public boolean isMultipleTitlesOwner()
    {
        return multipleTitlesOwner;
    }

    public void setMultipleTitlesOwner(boolean multipleTitlesOwner)
    {
        this.multipleTitlesOwner = multipleTitlesOwner;
    }

    public void setId(int id)
    {
        this.id = id;
    }
}
