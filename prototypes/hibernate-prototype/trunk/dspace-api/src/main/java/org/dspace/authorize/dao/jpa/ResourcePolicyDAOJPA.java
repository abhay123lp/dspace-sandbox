package org.dspace.authorize.dao.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

public class ResourcePolicyDAOJPA
{
    public List<ResourcePolicy> getPolicies(DSpaceObject dso, int actionID, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT rp FROM ResourcePolicy rp " +
        		"WHERE resource_type_id = :type AND resource_id = :id AND action_id = :action ");
        q.setParameter("type", dso.getType());
        q.setParameter("id", dso.getId());
        q.setParameter("action", actionID);
        List<ResourcePolicy> rps = q.getResultList();
        return rps;
    }
    
    public List<ResourcePolicy> getPolicies(DSpaceObject dso, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT rp FROM ResourcePolicy rp " +
                "WHERE resource_type_id = :type AND resource_id = :id");
        q.setParameter("type", dso.getType());
        q.setParameter("id", dso.getId());        
        List<ResourcePolicy> rps = q.getResultList();
        return rps;
    }
    
    public List<ResourcePolicy> getPolicies(Group group, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT rp FROM ResourcePolicy rp " +
                "WHERE epersongroup_id = :epersongroup_id");
        q.setParameter("epersongroup_id", group.getId());
        List<ResourcePolicy> rps = q.getResultList();
        return rps;
    }
    
    public List<ResourcePolicy> getPolicies(DSpaceObject dso, Group group, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT rp FROM ResourcePolicy rp " +
                "WHERE resource_type_id = :type AND resource_id = :id AND epersongroup_id = :group_id");
        q.setParameter("type", dso.getType());
        q.setParameter("id", dso.getId());        
        q.setParameter("group_id", group.getId());
        List<ResourcePolicy> rps = q.getResultList();
        return rps;
    }
}
