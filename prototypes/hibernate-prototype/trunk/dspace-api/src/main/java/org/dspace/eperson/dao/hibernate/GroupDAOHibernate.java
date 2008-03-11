package org.dspace.eperson.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
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
    
    public List<Group> findAllSupervisorGroup(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM WorkspaceItemLink wil, " +
        		"IN (wil.group) AS g ORDER BY g.name");
        List<Group> group = q.getResultList();
        return group;
    }
    
    public List<Group> findSupervisorGroup(WorkspaceItem workspaceItem, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM WorkspaceItemLink wil, IN (wil.group) AS g " +
        		"WHERE wil := :workspaceItem ORDER BY g.name");
        List<Group> group = q.getResultList();
        return group;
    }
    
    public List<Group> findGroups(String query, int offset, int limit, Context context) {
        EntityManager em = context.getEntityManager();
        String dbquery = "SELECT g FROM Group g " +
        		"WHERE g.name = :name " +
        		"OR g.id = :id";
        
        Query q = em.createQuery(dbquery);
        q.setParameter("name", query);
        q.setParameter("id", Integer.valueOf(query));
        if (offset >= 0 && limit > 0)
        {
              q.setMaxResults(limit);
              q.setFirstResult(offset);
        }
        List<Group> group = q.getResultList();
        return group;
    }
    
    public Group getGroupByUUID(UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT g FROM Group g WHERE g.uuid = :uuid");
        q.setParameter("uuid", uuid);
        Group g = (Group)q.getSingleResult();
        return g;
    }
    
}
