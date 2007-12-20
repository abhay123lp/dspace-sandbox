package org.dspace.core;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.Site;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.GenericDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.storage.dao.CRUD;

public class ApplicationService {
	
	private static GenericDAOFactory genericDAOFactory;
	
	/*  
	 * Returns the object representing the entity with that id
	 */
	public <T> T get(Context context, Class<T> clazz, int id) {
		CRUD<T> crud = genericDAOFactory.getInstance(clazz, context);
		return crud.retrieve(id);
	}
	
	/*
	 * Saves or updates an object 
	 */
	public <T> void saveOrUpdate(Context context, Class<T> clazz, T object){}
	
	public MetadataField getMetadataField(String element, String qualifier,	String schema, Context context) {
		MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
		MetadataField mdf = mdfdao.retrieve(schema, element, qualifier);
		return mdf;		
	}
	
	public List<Community> findAllCommunities(Context context) {
		CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
		List<Community> communities = cdao.getCommunities();
		return communities;
	}
	
	public List<Collection> findAllCollections(Context context) {
		CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
		List<Collection> collections = cdao.getCollections();
		return collections;
	}
	
	public List<Item> findAllItems(Context context) {
		ItemDAO idao = ItemDAOFactory.getInstance(context);
		List<Item> collections = idao.getItems();
		return collections;
	}
	
}
