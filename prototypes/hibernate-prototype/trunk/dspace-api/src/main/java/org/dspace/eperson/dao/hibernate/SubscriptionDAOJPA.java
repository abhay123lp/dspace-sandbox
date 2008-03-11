package org.dspace.eperson.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.dao.SubscriptionDAO;

public class SubscriptionDAOJPA extends SubscriptionDAO
{
    public Subscription findEPersonSubscriptionInCollection(EPerson eperson, Collection collection, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM Subscription s WHERE s.collection = :collection AND s.ePerson = :eperson");
        q.setParameter("eperson", eperson);
        q.setParameter("collection", collection);
        Subscription subscription = (Subscription)q.getSingleResult();
        return subscription;
    }
    
    public List<Subscription> findAllSubscriptions(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM Subscription s GROUP BY s.eperson.id");
        List<Subscription> subs = q.getResultList();
        return subs;
    }
    
    public List<Subscription> findSubscriptionsByEPerson(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM Subscription s WHERE s.ePerson = :eperson");
        q.setParameter("eperson", eperson);
        List<Subscription> subs = q.getResultList();
        return subs;
    }
    
    public void deleteAllSubscription(EPerson eperson, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("DELETE FROM Subscription s WHERE s.eperson = :eperson");
        q.setParameter("eperson", eperson);
        q.executeUpdate();
    }
    
    public Subscription getSubscriptionByUUID(UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT s FROM Subscription s WHERE s.uuid = :uuid");
        q.setParameter("uuid", uuid);
        Subscription s = (Subscription)q.getSingleResult();
        return s;
    }
}
