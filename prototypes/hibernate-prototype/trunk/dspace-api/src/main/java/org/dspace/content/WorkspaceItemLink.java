package org.dspace.content;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.eperson.Group;

@Entity
@Table(name="epersongroup2workspaceitem")
public class WorkspaceItemLink
{
    private Group group;
    private WorkspaceItem workspaceItem;
    private int id;
    
    public WorkspaceItemLink() {}

    @ManyToOne
    @JoinColumn(name="eperson_group_id")
    public Group getGroup()
    {
        return group;
    }

    public void setGroup(Group group)
    {
        this.group = group;
    }

    @ManyToOne
    @JoinColumn(name="workspace_item_id")
    public WorkspaceItem getWorkspaceItem()
    {
        return workspaceItem;
    }

    public void setWorkspaceItem(WorkspaceItem workspaceItem)
    {
        this.workspaceItem = workspaceItem;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    
    
}
