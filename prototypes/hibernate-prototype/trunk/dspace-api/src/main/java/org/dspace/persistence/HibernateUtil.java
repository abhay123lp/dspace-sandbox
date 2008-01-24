package org.dspace.persistence;


import org.hibernate.*;
import org.hibernate.cfg.*;

public class HibernateUtil {
	private static SessionFactory sessionFactory;
	static {
	  try {
	     sessionFactory=new Configuration().configure().buildSessionFactory();
	  } catch (Throwable ex) {
		     throw new ExceptionInInitializerError(ex);
	  }
	}
	
	public static SessionFactory getSessionFactory() {
	    return sessionFactory;
	}
	
	public static void shutdown() {
	    // Close caches and connection pools
	    getSessionFactory().close();
	}

}