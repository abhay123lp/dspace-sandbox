package org.dspace.checker;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.core.Context;

public class ChecksumResultDAOJPA
{
    public List<String> findAllCodes(Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT c.result_code FROM ChecksumCheckResults c");
        List<String> codes = q.getResultList();
        return codes;
    }
    
    public String findChecksumCheckStrByCode(String code, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("select c.result_description ChecksumCheckResults c where result_code = :code");
        q.setParameter("code", code);
        String resultcode = (String)q.getSingleResult();
        return resultcode;        
    }
}
