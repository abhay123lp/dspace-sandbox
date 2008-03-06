package org.dspace.content.dao.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.WorkspaceItemLink;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

public class WorkspaceItemDAOJPA extends WorkspaceItemDAO
{
    public WorkspaceItemDAOJPA(Context context) {
        super(context);
    }
    public WorkspaceItemLink findWorkspaceItemLink(Group group, InProgressSubmission ips, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wil FROM WorkspaceItemLink wil " +
                "WHERE wil.group = :group AND wil.workspaceItem = :workspaceItem");
        q.setParameter("group", group);
        q.setParameter("workspaceItem", (WorkspaceItem)ips);
        WorkspaceItemLink wil = (WorkspaceItemLink)q.getSingleResult();
        return wil;
    }
    
    public void deleteWorkspaceItemLink(Group group, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE FROM WorkspaceItemLink wil WHERE wil.group = :group");
        q.setParameter("group", group);
        q.executeUpdate();
    }
}
