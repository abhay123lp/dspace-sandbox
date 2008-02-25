package org.dspace.eperson.dao.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;

public class RegistrationDataDAOJPA
{
    public void deleteRegistrationDataByToken(Context context, String token) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE FROM ResistrationData r WHERE r.token = :token");
        q.setParameter("token", token);
        q.executeUpdate();
    }
}
