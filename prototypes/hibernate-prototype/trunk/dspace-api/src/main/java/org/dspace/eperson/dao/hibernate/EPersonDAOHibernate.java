package org.dspace.eperson.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;

public class EPersonDAOHibernate extends EPersonDAO 
{
    public EPersonDAOHibernate(Context context) {
        super(context);
    }
    
    public EPerson findEPersonByEmail(Context context, String email) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT p FROM EPersonMetadata m, IN (m.eperson) AS p " +
        "WHERE m.field = email AND m.value = :value");
        q.setParameter("value", email);
        EPerson e = (EPerson)q.getSingleResult();
        return e;
    }
    
    public EPerson findEPersonByNetid(Context context, String netid) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT p FROM EPersonMetadata m, IN (m.eperson) AS p " +
        		"WHERE m.field = netid AND m.value = :value");
        q.setParameter("value", netid);
        EPerson e = (EPerson)q.getSingleResult();
        return e;
    }

    public List<EPerson> findAllEPeopleSorted(int sortField,
            Context context)
    {
        String s;

        switch (sortField)
        {
        case EPerson.ID:
            s = "eperson_id";
            break;
        case EPerson.EMAIL:
            s = "email";
            break;
        case EPerson.LANGUAGE:
            s = "language";
            break;
        case EPerson.NETID:
            s = "netid";
            break;
        case EPerson.LASTNAME:
        default:
            s = "lastname";
        }
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT p FROM EPerson p ORDER BY p.:s");
        q.setParameter("s", s);
        List<EPerson> epeople = q.getResultList();
        return epeople;
    }
    
    
    public List<EPerson> findEPeople(String query, int offset, int limit, Context context) {
        EntityManager em = context.getEntityManager();
        String dbquery = "SELECT p FROM EPerson p " +
            "WHERE p.id LIKE :id " +
            "OR p.firstname LIKE :fn " +
            "OR p.lastname LIKE :ln " +
            "OR p.email LIKE :email " +
            "ORDER BY p.lastname, p.firstname ASC";
                
        Query q = em.createQuery(dbquery);
        
        String params = "%" + query.toLowerCase() + "%";
        q.setParameter("id", Integer.valueOf(params));
        q.setParameter("fn", params);
        q.setParameter("ln", params);
        q.setParameter("email", params);
        if (offset >= 0 && limit > 0)
        {
              q.setMaxResults(limit);
              q.setFirstResult(offset);
        }
        
        List<EPerson> epeople = q.getResultList();
        return epeople;
    }
    
    public EPerson getEPersonByUUID(UUID uuid, Context context) {
        EntityManager em= context.getEntityManager();
        Query q = em.createQuery("SELECT ep FROM EPerson ep WHERE ep.uuid = :uuid");
        q.setParameter("uuid", uuid);
        EPerson ep = (EPerson)q.getSingleResult();
        return ep;
    }
}
