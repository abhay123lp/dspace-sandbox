package org.dspace.eperson.dao.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.Query;

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
}
