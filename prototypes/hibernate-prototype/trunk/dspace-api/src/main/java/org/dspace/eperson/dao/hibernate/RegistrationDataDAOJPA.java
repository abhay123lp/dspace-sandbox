package org.dspace.eperson.dao.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;

public class RegistrationDataDAOJPA extends RegistrationDataDAO
{
    public void deleteRegistrationDataByToken(Context context, String token) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE FROM ResistrationData r WHERE r.token = :token");
        q.setParameter("token", token);
        q.executeUpdate();
    }
    
    public RegistrationData findRegistrationDataByToken(String token, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT r FROM RegistrationData r WHERE r.token = :token");
        q.setParameter("token", token);
        RegistrationData r = (RegistrationData)q.getSingleResult();
        return r;
    }
    
    public RegistrationData findRegistrationDataByEmail(String email, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT r FROM RegistrationData r WHERE r.email = :email");
        q.setParameter("email", email);
        RegistrationData r = (RegistrationData)q.getSingleResult();
        return r;
    }
}
