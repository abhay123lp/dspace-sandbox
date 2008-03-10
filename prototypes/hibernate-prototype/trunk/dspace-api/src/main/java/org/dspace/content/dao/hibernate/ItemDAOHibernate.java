package org.dspace.content.dao.hibernate;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class ItemDAOHibernate extends ItemDAO {
	public ItemDAOHibernate(Context context) {
		super(context);
	}
	 
	/* returns all in-archive, not-withdrawn items */
	public List<Item> getItems(EntityManager em) {
	   	Query q = em.createQuery("SELECT OBJECT(i) " +
	   			                 "FROM Item i " +
	   			                 "WHERE i.withdrawn = false AND i.inArchive = true");	   	
		List<Item> items = q.getResultList();
		return items;
	}
	
	public List<Item> getWithdrawnItems(Collection collection, EntityManager em) {
        Query q = em.createQuery("SELECT i " +
        		                 "FROM Collection collection, IN (collection.items) AS i " +
        		                 "WHERE i.withdrawn = true AND collection = :collection");
        q.setParameter("collection", collection);
        List<Item> items = q.getResultList();
        return items;	    
	}
	
	public List<Item> getItemsByMetadataValue(MetadataValue value, EntityManager em) {
        Query q = em.createQuery("SELECT i FROM Item i, IN (i.metadata) AS m WHERE m = :metadatavalue");
        q.setParameter("metadatavalue", value);
        List<Item> items = q.getResultList();
        return items;   	    
	}
	
	//TODO completare
	public List<Item> findItemsByMetadataValue(MetadataValue value, Date startDate, Date endDate, Context context) {
	    EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT i FROM Item i, IN (i.metadata) AS m " +
        		"WHERE m = :metadatavalue " +
        		"AND ");
	    List<Item> items = q.getResultList();
	    return items;
	}
	
	public List<Item> findItemsBySubmitter(EPerson eperson, Context context) {
	    EntityManager em = context.getEntityManager();
	    Query q = em.createQuery("SELECT i FROM Item i " +
	    		"WHERE i.inArchive = true AND i.subitter = :submitter");
	    q.setParameter("submitter", eperson);
	    List<Item> items = q.getResultList();
	    return items;
	}
	
	public List<Item> findItemForHarvest(DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn, EntityManager em) 
            throws ParseException {
	    String query = "SELECT DISTINCT i ";
	    
	    if (scope != null)
        {
            if (scope.getType() == Constants.COLLECTION)
            {
                query += "FROM Collection collection. IN (collection.items) AS i ";
            }
            else if (scope.getType() == Constants.COMMUNITY)
            {
                query += "FROM CommunityMapping cm, IN (cm.item) AS i ";
            }
        } else {
            query += "FROM Item i ";
        }
	    
	    query += "WHERE ";
	    boolean whereStart = false;
	    boolean selfGenerated = false;	    
	    if (scope != null)
        {
            if (scope.getType() == Constants.COLLECTION)
            {
                query += "collection.id = :scopeID ";
                whereStart = true;
            }
            else if (scope.getType() == Constants.COMMUNITY)
            {
                query += "community.id = :scopeID ";
                whereStart = true;
            }
        }
	    
	    if (startDate != null)
        {
            if (!whereStart)
            {
                query = query + "AND ";
            }
            query += "i.last_modified >= :startDate ";
            whereStart = true;
        }

	    if (endDate != null)
        {
            if (!whereStart)
            {
                query += "AND ";
            }
            query += "i.last_modified <= :endDate ";
            whereStart = true;
            
            if (endDate.length() == 20)
            {
                endDate = endDate.substring(0, 19) + ".999Z";
                selfGenerated = true;
            }
        }
	    
	    if (!withdrawn)
        {
	        if (!whereStart)
            {
                query += "AND ";
            }
            query += "i.withdrawn = false ";
            whereStart = true;
        }
	    
	    if (!whereStart)
        {
            query = query + "AND ";
        }
        query = query + "in_archive = true ";
        query += "ORDER BY item_id";
	    
        
	    Query q = em.createQuery(query);
	    if (scope != null) {
            q.setParameter("scopeID", scope.getId());
	    }
        if (startDate != null) {
            q.setParameter("startDate", toTimestamp(startDate, false));
        }
        if (endDate != null) {
            q.setParameter("endDate", toTimestamp(endDate, selfGenerated));
        }
	    List<Item> result = q.getResultList();
	    return result;
	    
	}
	 /**
     * Convert a String to a java.sql.Timestamp object
     *
     * @param t The timestamp String
     * @param selfGenerated Is this a self generated timestamp (e.g. it has
     *                      .999 on the end)
     * @return The converted Timestamp
     * @throws ParseException
     */
    private static Timestamp toTimestamp(String t, boolean selfGenerated)
        throws ParseException
    {
        SimpleDateFormat df;

        // Choose the correct date format based on string length
        if (t.length() == 10)
        {
            df = new SimpleDateFormat("yyyy-MM-dd");
        }
        else if (t.length() == 20)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
        else if (selfGenerated)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        else {
            // Not self generated, and not in a guessable format
            throw new ParseException("", 0);
        }

        // Parse the date
        df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return new Timestamp(df.parse(t).getTime());
    }

    /**
     * Take the date object and convert it into a string of the form YYYY-MM-DD
     *
     * @param   date    the date to be converted
     *
     * @return          A string of the form YYYY-MM-DD
     */
    private static String unParseDate(Date date)
    {
        // Use SimpleDateFormat
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd");
        return sdf.format(date);
    }
	
}
