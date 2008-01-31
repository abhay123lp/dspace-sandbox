package org.dspace.core;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;

import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.GenericDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.content.factory.BundleFactory;


public class ApplicationService {
	
	private static String persistentUnit = ConfigurationManager.getProperty("dspace.persistentUnit");
	
	private static GenericDAOFactory genericDAOFactory;
	
	private EntityManagerFactory emf;
	private EntityManager em; 
	
	public ApplicationService() {
		emf = Persistence.createEntityManagerFactory(persistentUnit);
		em = emf.createEntityManager();
	}
	
	/* CRUD operations */
	
	/*  
	 * Returns the object representing the entity with that id
	 */
	public <T> T get(Context context, Class<T> clazz, int id) {
		System.out.println(" ApplicationService: Get " + clazz.getCanonicalName());
		setupTransaction("Get");
		T result = em.find(clazz, id);
		return result;		
	}
	
	/*
	 * Updates an object in the db
	 */
	public <T> void update(Context context, Class<T> clazz, T object) {
		System.out.println(" ApplicationService: Update " + clazz.getCanonicalName());
		setupTransaction("Update");	
		em.merge(object);
	}
	
	/*
	 * Saves a new object into the db
	 */
	protected <T> void save (Context context, Class<T> clazz, T object) {
		System.out.println(" ApplicationService: Save " + clazz.getCanonicalName());
		setupTransaction("Save");
		em.persist(object);
	}
	
	/* 
	 * Removes an object from the db
	 */
	protected <T> void delete (Context context, Class<T> clazz, T object) {
		System.out.println(" ApplicationService: Delete: " + clazz.getCanonicalName());
		setupTransaction("Delete");
		em.remove(object);
	}

	/* Finder operations */	
	
	public MetadataField getMetadataField(String element, String qualifier,	String schema, Context context) {
		MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
		MetadataField mdf = mdfdao.retrieve(schema, element, qualifier);
		return mdf;		
	}
	
	public List<Community> findAllCommunities(Context context) {
		CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
		setupTransaction("findAllCommunities");
		List<Community> communities = cdao.getCommunities(em);
		complete();
		return communities;
	}
	
	public List<Collection> findAllCollections(Context context) {
		CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
		setupTransaction("findAllCollections");
		List<Collection> collections = cdao.getCollections(em);
		complete();
		return collections;
	}
	
	public List<Item> findAllItems(Context context) {
		ItemDAO idao = ItemDAOFactory.getInstance(context);
		setupTransaction("findAllItems");
		List<Item> items = idao.getItems(em);
		complete();
		return items;
	}
	
	public Bundle findBundleByName(Item item, String name, Context context) {
		BundleDAO bdao = BundleDAOFactory.getInstance(context);
		return bdao.findBundleByName(item, name, em);
	}
	
	
	/* Methods for transactions */
	
    /* Aborts the current transaction and closes the entitymanager */
    public void abort() {
    	em.getTransaction().rollback();
    	em.close();
    	System.out.println(" ApplicationService.abort: transazione in rollback e EntityManager chiuso");
    }
    
    
    /* Commits the current transaction without closing the EntityManager */
    public void commit() {
    	EntityTransaction tr = em.getTransaction();
    	try {
			if (tr.isActive()) {
				tr.commit();				
				System.out.println(" ApplicationService.commit: Chiusa una transazione");
			} else {
    			System.out.println(" ApplicationService.commit: Transazione attuale già chiusa");
    		}
    		
    	} finally {}
    }
    
    /* Commits the current transaction and closes the EntityManager */
    public void complete() {
    	EntityTransaction tr = em.getTransaction();
        try {  
        	if (tr.isActive()) {
				tr.commit();
				System.out.println(" ApplicationService.Complete: Chiusa una transazione");
			} else {
				tr.begin();
				tr.commit();
				System.out.println(" ApplicationService.Complete: Trovata transazione chiusa, aperta e chiusa una nuova transazione");
			}
        } 
        finally {
        	em.close();
        	System.out.println(" ApplicationService.Complete: Chiuso l'EntityManager");
        }
   	
    }
    
    /* Utility methods */
    
    /* if there isn't an active transaction, create it */
    private void setupTransaction(String method) {
		if(em.getTransaction()==null || !em.getTransaction().isActive()) {
			EntityTransaction tr = em.getTransaction();
			tr.begin();
			System.out.println(" ApplicationService." + method + ": Creata una nuova transazione");
		}
    }
}
