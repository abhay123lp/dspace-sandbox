package org.dspace.core;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;

import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.GenericDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;


public class ApplicationService {
	
	private static String persistentUnit = ConfigurationManager.getProperty("dspace.persistentUnit");
	
	private static GenericDAOFactory genericDAOFactory;
	
	private EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistentUnit);
	private EntityManager em; //= emf.createEntityManager();
	
	/* CRUD operations */
	
	/*  
	 * Returns the object representing the entity with that id
	 */
	public <T> T get(Context context, Class<T> clazz, int id) {
		System.out.println(" -------------------------- Get -------------------------- ");
		control("Get");
		//em = emf.createEntityManager();
		T result = em.find(clazz, id);
		//em.close();
		return result;		
	}
	
	/*
	 * Updates an object in the db
	 */
	public <T> void update(Context context, Class<T> clazz, T object) {
		System.out.println(" -------------------------- Update -------------------------- ");
		control("Update");	
//		em = emf.createEntityManager();
		object = em.merge(object);	
//		em.flush();
//		em.close();
	}
	
	/*
	 * Saves a new object into the db
	 */
	public <T> void save (Context context, Class<T> clazz, T object) {
		System.out.println(" -------------------------- Save -------------------------- ");
		control("Save");
//		em = emf.createEntityManager();
		em.persist(object);
//		em.flush();
//		em.close();
	}
	
	/* 
	 * Removes an object from the db
	 */
	public <T> void delete (Context context, Class<T> clazz, T object) {
		System.out.println(" -------------------------- Delete -------------------------- ");
		control("Delete");
//		em = emf.createEntityManager();
		em.remove(object);
//		em.close();
	}
	
	
	/* Finder operations */
	
	
	public MetadataField getMetadataField(String element, String qualifier,	String schema, Context context) {
		MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
		MetadataField mdf = mdfdao.retrieve(schema, element, qualifier);
		return mdf;		
	}
	
	public List<Community> findAllCommunities(Context context) {
		CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
		if(em==null || !em.isOpen()) em = emf.createEntityManager();
		if(em.getTransaction()==null || !em.getTransaction().isActive()) {
			EntityTransaction tr = em.getTransaction();
			tr.begin();
		}
		List<Community> communities = cdao.getCommunities(em);
		complete();
		return communities;
	}
	
	public List<Collection> findAllCollections(Context context) {
		CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
		if(em==null || !em.isOpen()) em = emf.createEntityManager();
		if(em.getTransaction()==null || !em.getTransaction().isActive()) {
			EntityTransaction tr = em.getTransaction();
			tr.begin();
		}
		List<Collection> collections = cdao.getCollections(em);
		complete();
		return collections;
	}
	
	public List<Item> findAllItems(Context context) {
		ItemDAO idao = ItemDAOFactory.getInstance(context);
		List<Item> collections = idao.getItems();
		return collections;
	}
	
	
	/* Methods for transactions */
	
	
	/* Starts a new transaction */
    public void startTransaction() {
    	if(em==null || !em.isOpen()) {
    		em = emf.createEntityManager();
    		System.out.println("StartTransaction: Creato un nuovo EntityManager");
    	}
    	em.getTransaction().begin();
    }

    
    /* Aborts the current transaction and closes the entitymanager */
    public void abort() {
    	em.getTransaction().rollback();
    	em.close();
    }
    
    
    /* Commits the current transaction without closing the EntityManager */
    public void commit() {
    	EntityTransaction tr = em.getTransaction();
    	try {
			if (tr.isActive()) {
				tr.commit();
				System.out.println("Commit: Chiusa una transazione");
			} else {
    			System.out.println("Commit: Transazione attuale già chiusa");
    		}
    		
    	} finally {}
    }
    
    /* Commits the current transaction and closes the EntityManager */
    public void complete() {
    	System.out.println(" -------------------------- Complete -------------------------- ");
        EntityTransaction tr = em.getTransaction();
        try {  
        	if (tr.isActive()) {
				tr.commit();
				System.out.println("Complete: Chiusa una transazione");
			} else {
				System.out.println("Complete: Transazione attuale già chiusa");
			}
        } 
        finally {
        	em.close();
        	System.out.println("Complete: Chiuso un EntityManager");
        }
   	
    }
    
    /* Utility methods */
    
    /* If em is null, create a new em, if there isn't an active transaction, create it */
    private void control(String method) {
		if(em==null || !em.isOpen()) {
			em = emf.createEntityManager();
			System.out.println(method + ": Creato un nuovo EntityManager");
		}
		if(em.getTransaction()==null || !em.getTransaction().isActive()) {
			EntityTransaction tr = em.getTransaction();
			tr.begin();
			System.out.println(method + ": Creata una nuova transazione");
		}
    }
   
///*********************************************************************************************/
//
//    public <T> T get(Context context, Class<T> clazz, int id) {
//		T result = em.find(clazz, id);
//		return result;		
//	}
//	public <T> void update(Context context, Class<T> clazz, T object) {
//		em.merge(object);	
//	}
//	public <T> void save (Context context, Class<T> clazz, T object) {
//		EntityTransaction tr = em.getTransaction();
//		em.persist(object);
//		tr.commit();
//	}
//	public <T> void delete (Context context, Class<T> clazz, T object) {
//		em.remove(object);
//	}
    
}
