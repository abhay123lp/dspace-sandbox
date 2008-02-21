    package org.dspace.core;

import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.dao.BitstreamFormatDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.content.factory.CollectionFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.EPerson.EPersonMetadataField;


public class ApplicationService {
    
    
    /* CRUD operations */
    
    /**
     * Returns the object representing the entity with that id
     */
    public static <T> T get(Context context, Class<T> clazz, int id) {
        System.out.println(" ApplicationService: get " + clazz.getCanonicalName());
        // the object must be in the persistence context, for coherence
        setupTransaction("get", context);  
        T result = context.getEntityManager().find(clazz, id);
        return result;      
    }
    
    /**
     * Updates an object in the db. Use this method only if there's no
     * transaction active: if there's one, only a context.commit or
     * context.complete is necessary
     */
    public static <T> void update(Context context, Class<T> clazz, T object) {
        System.out.println(" ApplicationService: update " + clazz.getCanonicalName());
        setupTransaction("update", context);    
        context.getEntityManager().merge(object);
    }
    
    /**
     * Saves a new object into the db. Use this method only if there's no
     * transaction active: if there's one, only a context.commit or
     * context.complete is necessary
     */
    public static <T> void save (Context context, Class<T> clazz, T object) {
        System.out.println(" ApplicationService: save " + clazz.getCanonicalName());
        setupTransaction("save", context);
        context.getEntityManager().persist(object);
    }
    
    /**
     * Removes an object from the db. This method is public and not protected
     * because other packages use it, but it should be NOT used explicitly, use
     * managers instead.
     */
    public static <T> void delete (Context context, Class<T> clazz, T object) {
        System.out.println(" ApplicationService: delete: " + clazz.getCanonicalName());
        setupTransaction("delete", context);
        context.getEntityManager().remove(object);
    }
    
    //TODO implementare
    public static void deleteRegistrationDataByToken(Context context, String token) {
        
    }

    /* Finder operations */ 
    
    /*
     * Returns a particular metadatafield
     */
    public static MetadataField getMetadataField(String element, String qualifier,  String schema, Context context) {
        MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
        MetadataField mdf = mdfdao.retrieve(schema, element, qualifier);
        return mdf;     
    }
    
    /*
     * Returns all communities
     */
    public static List<Community> findAllCommunities(Context context) {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        // setupTransaction("findAllCommunities", context);
        List<Community> communities = cdao.getCommunities(context.getEntityManager());      
        return communities;
    }
    
    /*
     * Returns all top-level communities
     */
    public static List<Community> findAllTopCommunities(Context context) {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        // setupTransaction("findAllTopCommunities", context);
        List<Community> topcommunities = cdao.getTopCommunities(context.getEntityManager());
        return topcommunities;
    }
    
    /*
     * Returns all the collections
     */
    public static List<Collection> findAllCollections(Context context) {
        CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
        // setupTransaction("findAllCollections", context);
        List<Collection> collections = cdao.getCollections(context.getEntityManager());     
        return collections;
    }
    
    /* Returns all item with in_archive=true and withdrawn=false */
    public static List<Item> findAllItems(Context context) {
        ItemDAO idao = ItemDAOFactory.getInstance(context);
        // setupTransaction("findAllItems", context);
        List<Item> items = idao.getItems(context.getEntityManager());
        return items;
    }
    
    /*
     * Returns all withdrawn items
     */
    public static List<Item> findWithdrawnItems(Context context, Collection collection) {
        ItemDAO idao = ItemDAOFactory.getInstance(context);
        // setupTransaction("findWithdrawnItems", context);
        List<Item> items = idao.getWithdrawnItems(collection, context.getEntityManager());
        return items;
    }
    
    /*
     * Returns all items with a particular metadatavalue
     */
    public static List<Item> findItemByMetadataValue(MetadataValue value, Context context) {
        ItemDAO idao = ItemDAOFactory.getInstance(context);
        // setupTransaction("findItemByMetadataValue", context);
        List<Item> items = idao.getItemsByMetadataValue(value, context.getEntityManager());
        return items;
    }
    
    public static Bundle findBundleByName(Item item, String name, Context context) {
        BundleDAO bdao = BundleDAOFactory.getInstance(context);
        return bdao.findBundleByName(item, name, context.getEntityManager());
    }
    
    //TODO implementare
    public static List<Item> findItemForHarvest(DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn)
            throws ParseException
    {
        return null;
    }
    

    public static List<Bitstream> findDeletedBitstream(Context context) {
        BitstreamDAO bdao = BitstreamDAOFactory.getInstance(context);
        List<Bitstream> bitstreams = bdao.getDeletedBitstreams(context.getEntityManager());
        return bitstreams;
    }
    
    
    public static BitstreamFormat findBitstreamFormatByShortDescription(String description, Context context) {
        BitstreamFormatDAO bdao = BitstreamFormatDAOFactory.getInstance(context);
        BitstreamFormat bf = bdao.getBitstreamFormatByShortDescription(description, context.getEntityManager());
        return bf;      
    }
        
    public static List<BitstreamFormat> findBitstreamFormatByInternal(boolean internal, Context context) {
        BitstreamFormatDAO bdao = BitstreamFormatDAOFactory.getInstance(context);
        List<BitstreamFormat> bfs = bdao.getBitstreamFormatByInternal(internal, context.getEntityManager());
        return bfs;
    }
    
    // TODO implementare
    public static EPerson findEPersonByEmail(Context context, String email) {
        return null;
    }
    
    // TODO implementare
    public static EPerson findEPersonByNetid(Context context, String netid) {
        return null;
    }
    
    public static EPerson findEPersonByEPersonMetadataField(Context context, EPersonMetadataField field, String value) {
        return null;
    }
    
    // FIXME //ritorna tutte le persone nel gruppo e nei suoi sottogruppi,
    // ricorsivamente
    // questo metodo sta nell'account manager, perchè?
// public static List<EPerson> findAllEPeople(Group group) {
// return null;
// }
    
    // TODO implemetare
    public static Group findGroupByName(Context context, String name) {
        return null;
    }
    
    //TODO implementare
    public static List<Group> findAllGroups(Context context) {
        return null;
    }
    
    
    //TODO implementare
    public static List<Group> findAllGroupsSortedById(Context context) {
        return null;
    }
    
    //TODO implementare
    public static List<Group> findAllGroupsSortedByName(Context context) {
        return null;
    }
    
    //TODO implementare
    /**
     * Returns all groups the eperson belongs to, and 
     * all parent groups for groups eperson is a member of
     */  
    public static List<Group> findAllGroups(EPerson eperson) {
        return null;
    }
    
    //TODO implementare
    public static RegistrationData findRegistrationDataByToken(String token, Context context) {
        return null;
    }
    
    //TODO
    public static RegistrationData findRegistrationDataByEmail(String email, Context context) {
        return null;
    }
    
    public static int getCountItems(Collection collection, Context context) {
        CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
        Integer ccount = cdao.getCount(collection, context.getEntityManager());
        if(ccount==null) {
            // FIXME inserire un eccezione
            return -1;
        } else return ccount.intValue();
    }
    
    public static int getCountItems(Community community, Context context)
    {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        Integer ccount = cdao.getCount(community, context.getEntityManager());
        if(ccount==null) {
            // FIXME inserire un eccezione
            return -1;
        } else return ccount.intValue();
    }
    
    public static Integer countItems(Collection collection, Context context) {
        CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
        return cdao.count(collection, context.getEntityManager());        
    }
    
    public static Integer countItems(Community community, Context context) {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        return cdao.count(community, context.getEntityManager());        
    }
    
    
    /* Methods for transactions */
    
    /* Aborts the current transaction and closes the entitymanager */
// public static void abort(Context context) {
// context.getEntityManager().getTransaction().rollback();
// context.getEntityManager().close();
// System.out.println(" ApplicationService.abort: transazione in rollback e
// EntityManager chiuso");
// }
    
    
    /* Commits the current transaction without closing the EntityManager */
// public static void commit(Context context) {
// EntityTransaction tr = context.getEntityManager().getTransaction();
// try {
// if (tr.isActive()) {
// tr.commit();
// System.out.println(" ApplicationService.commit: Chiusa una transazione");
// } else {
// System.out.println(" ApplicationService.commit: Transazione attuale giÃ 
// chiusa");
// }
//          
// } finally {}
// }
    
    /* Commits the current transaction and closes the EntityManager */
// public static void complete(Context context) {
// EntityTransaction tr = context.getEntityManager().getTransaction();
// try {
// if (tr.isActive()) {
// tr.commit();
// System.out.println(" ApplicationService.Complete: Chiusa una transazione");
// } else {
// tr.begin();
// tr.commit();
// System.out.println(" ApplicationService.Complete: Trovata transazione chiusa,
// aperta e chiusa una nuova transazione");
// }
// }
// finally {
// context.getEntityManager().close();
// System.out.println(" ApplicationService.Complete: Chiuso l'EntityManager");
// }
//      
// }
    
    /* Utility methods */
    
    /* if there isn't an active transaction, create it */
    private static void setupTransaction(String method, Context context) {
        EntityManager em = context.getEntityManager();
        if(em.getTransaction()==null || !em.getTransaction().isActive()) {
            EntityTransaction tr = em.getTransaction();
            tr.begin();
            System.out.println(" ApplicationService." + method + ": creata una nuova transazione");
        }
    }
}
