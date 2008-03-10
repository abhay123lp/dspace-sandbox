package org.dspace.content.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.SupervisedItem;
import org.dspace.content.dao.SupervisedItemDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class SupervisedItemDAOJPA extends SupervisedItemDAO
{
    public SupervisedItemDAOJPA(Context context) {
        super(context);
    }

    //FIXME testare queste due query, sono sicuramente scritte male
    
    public List<SupervisedItem> findSupervisedItems(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkspaceItem wi " +
        		"WHERE wi.type = 'supervised'");
        List<SupervisedItem> si = q.getResultList();
        return si;
    }
    
    public List<SupervisedItem> findSupervisedItems(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT wi FROM WorkspaceItemLink wil, " +
        		"IN (wil.WorkspaceItem) AS wi, " +
        		"IN (wil.Group) AS g " +
        		"IN (g.members) AS m " +
        		"WHERE wi.type = 'supervised' " +
        		"AND m = :eperson");
        q.setParameter("eperson", eperson);
        List<SupervisedItem> si = q.getResultList();
        return si;
    }

}
