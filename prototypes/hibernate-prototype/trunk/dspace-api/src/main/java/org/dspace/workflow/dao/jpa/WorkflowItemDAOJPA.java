package org.dspace.workflow.dao.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.TaskListItem;
import org.dspace.workflow.WorkflowItem;

public class WorkflowItemDAOJPA //extends WorkflowItemDAO
{
    public List<TaskListItem> findTaskListItemByWorkflowId(int workflowId, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT tli FROM TaskListItem tli WHERE tli.workflowItem.id = :workflowId");
        q.setParameter("workflowId", workflowId);
        List<TaskListItem> tli = q.getResultList();
        return tli;
    }
    
    public List<WorkflowItem> findWorkflowItemByCollection(Collection collection, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkflowItem wi WHERE wi.collection = :collection");
        q.setParameter("collection", collection);
        List<WorkflowItem> wis = q.getResultList();
        return wis;
    }
    
    public List<WorkflowItem> findWorkflowItemsByOwner(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkflowItem wi WHERE wi.owner = :eperson GROUP BY wi.id");
        List<WorkflowItem> wis = q.getResultList();
        return wis;
    }
    
    public List<WorkflowItem> findWorkflowItemsBySubmitter(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkflowItem wi WHERE wi.item.submitter = :eperson ORDER BY wi.id");
        List<WorkflowItem> wis = q.getResultList();
        return wis;
    }
    
    public List<WorkflowItem> findAllWorkflowItem(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkspaceItem wi ORDER BY wi.id");
        List<WorkflowItem> wis = q.getResultList();
        return wis;
    }
    
    public List<TaskListItem> findTaskListItemByEPerson(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT tli FROM TaskListItem tli " +
        		"WHERE tli.eperson = :eperson AND tli.iD = tli.workflowitem.id");
        q.setParameter("eperson", eperson);
        List<TaskListItem> tlis = q.getResultList();
        return tlis;
    }
}
