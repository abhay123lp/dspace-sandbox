package org.dspace.eperson.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.WorkspaceItemLink;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;

public class GroupDAOHibernate extends GroupDAO
{
    public GroupDAOHibernate(Context context) {
        super(context);
    }
    
    public Group findGroupByName(Context context, String name) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM Group g WHERE g.name = :name");
        q.setParameter("name", name);
        Group group = (Group)q.getSingleResult();
        return group;
    }
    
    public List<Group> findAllGroupsSortedById(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM Group g ORDER BY g.id");
        List<Group> group = q.getResultList();
        return group;
    }
    
    public List<Group> findAllGroupsSortedByName(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM Group g ORDER BY g.name");
        List<Group> group = q.getResultList();
        return group;
    }
    
    
}
