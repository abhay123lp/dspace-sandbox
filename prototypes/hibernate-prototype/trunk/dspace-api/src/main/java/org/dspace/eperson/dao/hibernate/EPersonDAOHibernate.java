package org.dspace.eperson.dao.hibernate;

import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;

public class EPersonDAOHibernate extends EPersonDAO 
{
    public EPersonDAOHibernate(Context context) {
        super(context);
    }
    
    //TODO implementare (??? nel daopostgres non c'è)
    public List<EPerson> search(String query){
        return null;
    }
    
    //TODO implementare
    public List<EPerson> search(String query, int offset, int limit) {
        return null;
    }
    
    //TODO implementare
    public List<EPerson> getEPeople(int sortField) {
        return null;
    }
    
    //TODO implementare
    public EPerson retrieve(UUID uuid) {
        return null;
    }
}
