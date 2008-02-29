    package org.dspace.core;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.jpa.ResourcePolicyDAOJPA;
import org.dspace.checker.BitstreamInfoDAOJPA;
import org.dspace.checker.ChecksumHistoryDAOJPA;
import org.dspace.checker.ChecksumResultDAOJPA;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.checker.ReporterDAOJPA;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
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
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.content.dao.MetadataSchemaDAOFactory;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.content.dao.MetadataValueDAOFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.EPerson.EPersonMetadataField;
import org.dspace.eperson.dao.hibernate.EPersonDAOHibernate;
import org.dspace.eperson.dao.hibernate.GroupDAOHibernate;
import org.dspace.eperson.dao.hibernate.RegistrationDataDAOJPA;
import org.dspace.eperson.dao.hibernate.SubscriptionDAOJPA;
import org.dspace.workflow.TaskListItem;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.dao.jpa.WorkflowItemDAOJPA;


public class ApplicationService {
    
    
    /* Generical CRUD operations */
    
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
    
    
    /* Delete methods */
    
    
    public static void deleteRegistrationDataByToken(Context context, String token) {
        RegistrationDataDAOJPA rddao = new RegistrationDataDAOJPA();
        rddao.deleteRegistrationDataByToken(context, token);
    }
    
    
    public static void deleteBitstreamInfo(int bitstreamId, Context context) {
        BitstreamInfoDAOJPA bidao = new BitstreamInfoDAOJPA();
        bidao.deleteBitstreamInfo(bitstreamId, context);        
    }
    
    public static void deleteHistoryForBitstreamInfo(int bitstreamId, Context context) {
        ChecksumHistoryDAOJPA chdao = new ChecksumHistoryDAOJPA();
        chdao.deleteHistoryForBitstreamInfo(bitstreamId, context);
    }
    
    public static int deleteHistoryByDateAndCode(Date retentionDate, String result, Context context) {
        ChecksumHistoryDAOJPA chdao = new ChecksumHistoryDAOJPA();
        return chdao.deleteHistoryByDateAndCode(retentionDate, result, context);
    }
  
 
   
    /* Finder operations */ 
    
    public static List<MetadataSchema> findAllMetadataSchema(Context context) {
        MetadataSchemaDAO mdsdao = MetadataSchemaDAOFactory.getInstance(context);
        return mdsdao.findAllMetadataSchema(context);
    }
    
    public static MetadataSchema findMetadataSchemaByName(String name, Context context) {
        MetadataSchemaDAO mdsdao = MetadataSchemaDAOFactory.getInstance(context);
        return mdsdao.findMetadataSchemaByName(name, context);
    }
    
    public static MetadataSchema findMetadataSchemaByNamespace(String namespace, Context context) {
        MetadataSchemaDAO mdsdao = MetadataSchemaDAOFactory.getInstance(context);
        return mdsdao.findMetadataSchemaByNamespace(namespace, context);
    }
    
    public static MetadataField findMetadataField(String element, String qualifier,  String schema, Context context) {
        MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
        return mdfdao.findMetadataField(schema, element, qualifier, context);             
    }
    
    public static MetadataField findMetadataField(int schemaId, String element, String qualifier, Context context) {
        MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
        return mdfdao.findMetadataField(schemaId, element, qualifier, context);           
    }
    
    public static List<MetadataField> findMetadataFields(MetadataSchema schema, Context context) {
        MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
        return mdfdao.findMetadataFields(schema, context);
    }
    
    public static List<MetadataField> findAllMetadataFields(Context context) {
        MetadataFieldDAO mdfdao =  MetadataFieldDAOFactory.getInstance(context);
        return mdfdao.findAllMetadataFields(context);
    }
    
    public static List<MetadataValue> findMetadataValues(MetadataField field, Context context) {
        MetadataValueDAO mdao = MetadataValueDAOFactory.getInstance(context);
        return mdao.getMetadataValues(field, context);
    }
    
    public static List<MetadataValue> findMetadataValues(MetadataField field, String value, Context context) {
        MetadataValueDAO mdao = MetadataValueDAOFactory.getInstance(context);
        return mdao.getMetadataValues(field, value, context);
    }
    
    public static List<MetadataValue> findMetadataValues(MetadataField field, String value, String language, Context context) {
        MetadataValueDAO mdao = MetadataValueDAOFactory.getInstance(context);
        return mdao.getMetadataValues(field, value, language, context);
    }
    
    public static List<MetadataValue> findMetadataValues(Item item, Context context) {
        MetadataValueDAO mdao = MetadataValueDAOFactory.getInstance(context);
        return mdao.getMetadataValues(item, context);
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
            boolean items, boolean collections, boolean withdrawn, Context context)
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
    
    
    public static List<Integer> findAllCommunityBitstreamsId(int communityId, Context context) {
        return null;
    }
    
    
    public static List<Integer> findAllCollectionBitstreamsId(int collectionId, Context context) {
        return null;
    }
    
    
    public static List<Integer> findAllItemBitstreamsId(int itemId, Context context) {
        return null;
    }
    //FIXME query sbagliata, da rifare    
    public static EPerson findEPersonByEmail(Context context, String email) {
        EPersonDAOHibernate edao = new EPersonDAOHibernate(context);
        return edao.findEPersonByEmail(context, email);
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
    
    
    public static Group findGroupByName(Context context, String name) {
        GroupDAOHibernate gdao = new GroupDAOHibernate(context);
        return gdao.findGroupByName(context, name);
    }
    
    //TODO implementare
    public static List<Group> findAllGroups(Context context) {
        return null;
    }
    
    
    
    public static List<Group> findAllGroupsSortedById(Context context) {
        GroupDAOHibernate gdao = new GroupDAOHibernate(context);
        return gdao.findAllGroupsSortedById(context);
    }
    
    
    public static List<Group> findAllGroupsSortedByName(Context context) {
        GroupDAOHibernate gdao = new GroupDAOHibernate(context);
        return gdao.findAllGroupsSortedByName(context);
    }
    
    //TODO implementare
    /**
     * Returns all groups the eperson belongs to, and 
     * all parent groups for groups eperson is a member of
     */  
    public static List<Group> findAllGroups(EPerson eperson, Context context) {
        return null;
    }    
    
    public static RegistrationData findRegistrationDataByToken(String token, Context context) {
        RegistrationDataDAOJPA rdao = new RegistrationDataDAOJPA();
        return rdao.findRegistrationDataByToken(token, context);
    }
    
    
    public static RegistrationData findRegistrationDataByEmail(String email, Context context) {
        RegistrationDataDAOJPA rdao = new RegistrationDataDAOJPA();
        return rdao.findRegistrationDataByEmail(email, context);
    }
    
    public static Subscription findEPersonSubscriptionInCollection(EPerson eperson, Collection collection, Context context) {
        SubscriptionDAOJPA sdao = new SubscriptionDAOJPA();
        return sdao.findEPersonSubscriptionInCollection(eperson, collection, context);
    }
    
    public static List<Subscription> findAllSubscriptions(Context context) {
        SubscriptionDAOJPA sdao = new SubscriptionDAOJPA();
        return sdao.findAllSubscriptions(context);
    }
   
    public static List<Subscription> findSubscriptionsByEPerson(EPerson eperson, Context context) {
        SubscriptionDAOJPA sdao = new SubscriptionDAOJPA();
        return sdao.findSubscriptionsByEPerson(eperson, context);
    }
    
    public static List<Bitstream> findUnknownBitstreams(Context context) {
        ReporterDAOJPA rdao = new ReporterDAOJPA();
        return rdao.findUnknownBitstreams(context);
    }
    
    
    public static int findOldestBitstreamId(Context context) {
        BitstreamInfoDAOJPA bdao = new BitstreamInfoDAOJPA();
        return bdao.findOldestBitstream(context);
    }
    
    public static int findOldestBitstream(Timestamp lessThanDate, Context context) {
        BitstreamInfoDAOJPA bdao = new BitstreamInfoDAOJPA();
        return bdao.findOldestBitstream(lessThanDate, context);
    }
        
    public static List<MostRecentChecksum> findMRCNotProcessedBitstreamsReport(Date startDate, Date endDate, Context context) {
        ReporterDAOJPA rdao = new ReporterDAOJPA();
        return rdao.findNotProcessedBitstreamsReport(startDate, endDate, context);
    }
    
    public static List<MostRecentChecksum> findMRCForBitstreamResultTypeReport(Date startDate, Date endDate, String resultCode, Context context) {
        ReporterDAOJPA rdao = new ReporterDAOJPA();
        return rdao.findMRCBitstreamResultTypeReport(startDate, endDate, resultCode, context);
    }
    
    public static MostRecentChecksum findMostRecentChecksumByBitstreamId(int bitstreamId, Context context) {
        BitstreamInfoDAOJPA bdao = new BitstreamInfoDAOJPA();
        return bdao.findMostRecentChecksumByBitstreamId(bitstreamId, context);        
    }
    
    
    public static String findChecksumCheckStrByCode(String code, Context context) {
        ChecksumResultDAOJPA crdao = new ChecksumResultDAOJPA();
        return crdao.findChecksumCheckStrByCode(code, context);
    }
    
    
    public static List<String> findAllCodes(Context context) {
        ChecksumResultDAOJPA crdao = new ChecksumResultDAOJPA();
        return crdao.findAllCodes(context);
    }
    
    public static List<TaskListItem> findTaskListItemByWorkflowId(int workflowId, Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findTaskListItemByWorkflowId(workflowId, context);        
    }
    
    //FIXME ricontrollare la query confrontandola con quella del dao originale
    public static List<TaskListItem> findTaskListItemByEPerson(EPerson eperson, Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findTaskListItemByEPerson(eperson, context);
    }
    
    public static List<WorkflowItem> findWorkflowItemByCollection(Collection collection, Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findWorkflowItemByCollection(collection, context);        
    }
    
    public static List<WorkflowItem> findWorkflowItemsByOwner(EPerson eperson, Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findWorkflowItemsByOwner(eperson, context);
    }
    
    public static List<WorkflowItem> findWorkflowItemsBySubmitter(EPerson eperson, Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findWorkflowItemsBySubmitter(eperson, context);
    }
    
    public static List<WorkflowItem> findAllWorkflowItem(Context context) {
        WorkflowItemDAOJPA wdao = new WorkflowItemDAOJPA();
        return wdao.findAllWorkflowItem(context);
    }
    
    public static List<ResourcePolicy> findPolicies(DSpaceObject dso, int actionID, Context context) {
        ResourcePolicyDAOJPA rdao = new ResourcePolicyDAOJPA();
        return rdao.getPolicies(dso, actionID, context);
    }
    
    public static List<ResourcePolicy> findPolicies(DSpaceObject dso, Context context) {
        ResourcePolicyDAOJPA rdao = new ResourcePolicyDAOJPA();
        return rdao.getPolicies(dso, context);
    }
    
    public static List<ResourcePolicy> findPolicies(Group group, Context context) {
        ResourcePolicyDAOJPA rdao = new ResourcePolicyDAOJPA();
        return rdao.getPolicies(group, context);
    }
    
    public static List<ResourcePolicy> findPolicies(DSpaceObject dso, Group group, Context context) {
        ResourcePolicyDAOJPA rdao = new ResourcePolicyDAOJPA();
        return rdao.getPolicies(dso, group, context);
    }
    
    /* Other methods */
    
    /** Returns the recorded number of items in one collection */
    public static int getCountItems(Collection collection, Context context) {
        CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
        Integer ccount = cdao.getCount(collection, context.getEntityManager());
        if(ccount==null) {
            // FIXME inserire un eccezione
            return -1;
        } else return ccount.intValue();
    }
    
    /** Returns the recorded number of items in one community */
    public static int getCountItems(Community community, Context context)
    {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        Integer ccount = cdao.getCount(community, context.getEntityManager());
        if(ccount==null) {
            // FIXME inserire un eccezione
            return -1;
        } else return ccount.intValue();
    }
    
    /** Calculates and return the number of item in one collection */
    public static Integer countItems(Collection collection, Context context) {
        CollectionDAO cdao = CollectionDAOFactory.getInstance(context);
        return cdao.count(collection, context.getEntityManager());        
    }
    
    /** Calculates and return the number of item in one community (no sub-communities, no collections)*/
    public static Integer countItems(Community community, Context context) {
        CommunityDAO cdao = CommunityDAOFactory.getInstance(context);
        return cdao.count(community, context.getEntityManager());        
    }
    
     
    public static void insertMissingChecksumBitstreams(Context context) {
        BitstreamInfoDAOJPA bdao = new BitstreamInfoDAOJPA();
        bdao.insertMissingChecksumBitstreams(context);
    }
    
    public static void insertMissingHistoryBitstream(Context context) {
        BitstreamInfoDAOJPA bdao = new BitstreamInfoDAOJPA();
        bdao.insertMissingHistoryBitstream(context);
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
