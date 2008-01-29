/*
 * ArchiveManager.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.CollectionFactory;
import org.dspace.content.factory.CommunityFactory;
import org.dspace.content.factory.ItemFactory;
import org.dspace.content.uri.ExternalIdentifier;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.search.DSIndexer;

public class ArchiveManager
{
    private static Logger log = Logger.getLogger(ArchiveManager.class);
    
    private static ApplicationService applicationService;

    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     * 
     * FIXME uguale al find dell'as?
     */
/*    public static void withdrawItem(Context context, Item item)
//        throws AuthorizeException, IOException
//    {
//        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
//
//        String timestamp = DCDate.getCurrent().toString();
//
//        // Build some provenance data while we're at it.
//        String collectionProv = "";
//        Collection[] colls = (Collection[])item.getCollections().toArray();
//
//        for (int i = 0; i < colls.length; i++)
//        {
//            collectionProv = collectionProv + colls[i].getMetadata("name")
//                    + " (ID: " + colls[i].getId() + ")\n";
//        }
//
//        // Check permission. User either has to have REMOVE on owning
//        // collection or be COLLECTION_EDITOR of owning collection
//        if (!AuthorizeManager.authorizeActionBoolean(context,
//                item.getOwningCollection(), Constants.COLLECTION_ADMIN)
//                && !AuthorizeManager.authorizeActionBoolean(context,
//                        item.getOwningCollection(), Constants.REMOVE))
//        {
//            throw new AuthorizeException("To withdraw item must be " +
//                    "COLLECTION_ADMIN or have REMOVE authorization on " +
//                    "owning Collection.");
//        }
//
//        item.setWithdrawn(true);
//        item.setArchived(false);
//
//        EPerson e = context.getCurrentUser();
//        try
//        {
//            // Add suitable provenance - includes user, date, collections +
//            // bitstream checksums
//            String prov = "Item withdrawn by " + e.getFullName() + " ("
//                    + e.getEmail() + ") on " + timestamp + "\n"
//                    + "Item was in collections:\n" + collectionProv
//                    + InstallItem.getBitstreamProvenanceMessage(item);
//            MetadataField mdf = applicationService.getMetadataField("description", "provenance", MetadataSchema.DC_SCHEMA, context);
//            item.addMetadata(mdf, "en", prov);
//
//            // Update item in DB
//            itemDAO.update(item);
//
//            // Remove from indicies
//            IndexBrowse ib = new IndexBrowse(context);
//            ib.itemRemoved(item);
//            DSIndexer.unIndexContent(context, item);
//        }
//        catch (BrowseException be)
//        {
//            throw new RuntimeException(be);
//        }
//
//        // and all of our authorization policies
//        
//        AuthorizeManager.removeAllPolicies(context, item);
//
//        log.info(LogManager.getHeader(context, "withdraw_item", "user="
//                + e.getEmail() + ",item_id=" + item.getId()));
//    }
//
//    
//    //Reinstate a withdrawn item.
//     
//    public static void reinstateItem(Context context, Item item)
//        throws AuthorizeException, IOException
//    {
//        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
//
//        String timestamp = DCDate.getCurrent().toString();
//
//        // Check permission. User must have ADD on all collections.
//        // Build some provenance data while we're at it.
//        String collectionProv = "";
//        Collection[] colls = (Collection[])item.getCollections().toArray();
//
//        for (int i = 0; i < colls.length; i++)
//        {
//            collectionProv = collectionProv + colls[i].getMetadata("name")
//                    + " (ID: " + colls[i].getId() + ")\n";
//            AuthorizeManager.authorizeAction(context, colls[i],
//                    Constants.ADD);
//        }
//
//        item.setWithdrawn(false);
//        item.setArchived(true);
//
//        // Add suitable provenance - includes user, date, collections +
//        // bitstream checksums
//        EPerson e = context.getCurrentUser();
//
//        String prov = "Item reinstated by " + e.getFullName() + " ("
//                + e.getEmail() + ") on " + timestamp + "\n"
//                + "Item was in collections:\n" + collectionProv
//                + InstallItem.getBitstreamProvenanceMessage(item);
//        
//        MetadataField mdf = applicationService.getMetadataField("description", "provenance", MetadataSchema.DC_SCHEMA, context);
//        item.addMetadata(mdf, "en", prov);
//
//        // Update item in DB
//        itemDAO.update(item);
//
//        // Add to indicies
//        // Remove - update() already performs this
//        // Browse.itemAdded(context, this);
//        DSIndexer.indexContent(context, item);
//
//        // authorization policies
//        if (colls.length > 0)
//        {
//            // 
//            // policies from first collection
//            // remove the item's policies and replace them with
//            // the defaults from the collection
//            item.inheritCollectionDefaultPolicies(colls[0]);
//        }
//
//        log.info(LogManager.getHeader(context, "reinstate_item", "user="
//                + e.getEmail() + ",item_id=" + item.getId()));
//    }
*/

    /**
     * Call with a null source to add to the destination; call with a null
     * destination to remove from the source; used in this way, move() can act
     * as an alias for add() and delete().
     *
     * WARNING: Communities, Collection, and Items that are orphaned after
     * being removed from a container will be *deleted*. It may be better to
     * leave them in the system with a means for re-associating them with other
     * containers, but that doesn't really fit with the strict hierarchical
     * nature of DSpace containers. ie: if you delete a Community, you expect
     * everything beneath it to get deleted as well, not just to be marked as
     * being orphaned.
     *
     * WARNING 2: This needs to include some sanity checks to make sure we
     * don't end up with circular parent-child relationships.
     */
/*    public static void move(Context context,
            DSpaceObject dso, DSpaceObject source, DSpaceObject dest)
        throws AuthorizeException
    {
        assert((dso instanceof Item) ||
               (dso instanceof Collection) ||
               (dso instanceof Community));
        assert((source != null) || (dest != null));

        logMove(dso, source, dest);

        if (dso instanceof Item)
        {
            if (dest != null)
            {
                addItemToCollection(context,
                        (Item) dso, (Collection) dest);
            }
            else
            {
                removeItemFromCollection(context,
                        (Item) dso, (Collection) source);
            }
        }
        else if (dso instanceof Collection)
        {
            if (dest != null)
            {
                addCollectionToCommunity(context,
                        (Collection) dso, (Community) dest);
            }
            else
            {
                removeCollectionFromCommunity(context,
                        (Collection) dso, (Community) source);
            }
        }
        else if (dso instanceof Community)
        {
            if (dest != null)
            {
                addCommunityToCommunity(context,
                        (Community) dso, (Community) dest);
            }
            else
            {
                removeCommunityFromCommunity(context,
                        (Community) dso, (Community) source);
            }
        }
    }
*/
    
    /* Create Methods */

    /* Creates a new community. If parent is null the community is a topcommunity */
    public Community createCommunity(Community parent, Context context) {
    	Community community = CommunityFactory.getInstance(context);
    	if(parent!=null) {
    		addCommunity(parent, community);
    		applicationService.save(context, Community.class, community);
    	} 
    	return community;
    }
    
    /* Creates a new collection. A collection must have a parent-community*/
    public Collection createCollection(Community parent, Context context) {
    	if(parent==null) {
    		throw new IllegalArgumentException(
            "A Collection must have a non-null parent Community");
    	}
    	Collection collection = CollectionFactory.getInstance(context);
    	addCollection(parent, collection);
    	return collection;
    }
    
    /* Creates a new item. An item must have an owning-collection */
    public Item createItem(Collection parent, Context context) {
    	if(parent==null) {
    		throw new IllegalArgumentException(
            "A Item must have a non-null owning Collection");
    	}
    	Item item = ItemFactory.getInstance(context);
    	item.setOwningCollection(parent);
    	addItem(parent, item);
    	
    	return item;
    }
    
     
    /* Add Methods */
    
    /* Adds a subcommunity to the specified parent community */
    public void addCommunity(Community parent, Community child) {
    	if(parent==null || child==null) {
    		throw new IllegalArgumentException(
            "Both parent and child must be not null");
    	}
    	child.getParentCommunities().add(parent);
    	parent.getSubCommunities().add(child);
    }
    
    /* Adds a collection to the specified community */
    public void addCollection(Community parent, Collection child) {
    	if(parent==null || child==null) {
    		throw new IllegalArgumentException(
            "Both parent and child must be not null");
    	}
    	child.getCommunities().add(parent);
    	parent.getCollections().add(child);
    }
    
    /* Adds an item to the specified collection */
    public void addItem(Collection parent, Item child) {
    	if(parent==null || child==null) {
    		throw new IllegalArgumentException(
            "Both parent and child must be not null");
    	}
    	child.getCollections().add(parent);
    	parent.getItems().add(child);
    }
    
    /* Remove methods */
    
    /* Removes a subcommunity from the specified community */
    public void removeCommunity(Community parent, Community child, Context context) {
    	if(child==null) {
    		throw new IllegalArgumentException(
            "Child cannot be null");
    	}
    	if(parent!=null) {	    	
	    	child.getParentCommunities().remove(parent);
	    	parent.getSubCommunities().remove(child);
	    	if(child.getParentCommunities().size()==0) { //orphan
	    		applicationService.deleteCommunity(context, child);
	    	}
    	} else { //top-community
    		applicationService.deleteCommunity(context, child);
    	}
    }
    
    public void removeCollection(Community parent, Collection child, Context context) {
    	if(parent==null || child==null) {
    		throw new IllegalArgumentException(
            "Both parent and child must be not null");
    	}
    	
    	parent.getCollections().remove(child);
    	child.getCommunities().remove(parent);
    	
    	//if the collection is orphan, remove it
    	if(child.getCommunities().size()==0) {
    		applicationService.delete(context, Collection.class, child);
    	}   	
    }
    
    public void removeItem(Collection parent, Item child, Context context) {
    	if(parent==null || child==null) {
    		throw new IllegalArgumentException(
            "Both parent and child must be not null");
    	}
    	
    	parent.getItems().remove(child);
    	child.getCollections().remove(parent);
    	//if the item is orphan, delete it
    	if(child.getCollections().size()==0){
    		applicationService.delete(context, Item.class, child);
    	}    	    	
    }
    
    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * Add an item to the collection. This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc. This has instant effect;
     * <code>update</code> need not be called.
     */
/*    private static void addItemToCollection(Context context,
            Item item, Collection collection)
        throws AuthorizeException
    {
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);

        collectionDAO.link(collection, item);
    }

    private static void removeItemFromCollection(Context context,
            Item item, Collection collection)
        throws AuthorizeException
    {
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);

        // Remove mapping
        collectionDAO.unlink(collection, item);
    }

    private static void addCollectionToCommunity(Context context,
            Collection child, Community parent)
        throws AuthorizeException
    {
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);

        communityDAO.link(parent, child);
    }

    private static void removeCollectionFromCommunity(Context context,
            Collection child, Community parent)
        throws AuthorizeException
    {
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);

        communityDAO.unlink(parent, child);
    }

    private static void addCommunityToCommunity(Context context,
            Community child, Community parent)
        throws AuthorizeException
    {
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);

        communityDAO.link(parent, child);
    }

    private static void removeCommunityFromCommunity(Context context,
            Community child, Community parent)
        throws AuthorizeException
    {
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);

        communityDAO.unlink(parent, child);
    }

    private static void logMove(DSpaceObject dso, DSpaceObject source,
            DSpaceObject dest)
    {
        String dsoStr = "";
        String sourceStr = "";
        String destStr = "";

        switch (dso.getType())
        {
            case Constants.ITEM:
                dsoStr = "Item ";
                break;
            case Constants.COLLECTION:
                dsoStr = "Collection ";
                break;
            case Constants.COMMUNITY:
                dsoStr = "Community ";
                break;
            default:
        }

        if (source != null)
        {
            switch (source.getType())
            {
                case Constants.ITEM:
                    sourceStr = "Item ";
                    break;
                case Constants.COLLECTION:
                    sourceStr = "Collection ";
                    break;
                case Constants.COMMUNITY:
                    sourceStr = "Community ";
                    break;
                default:
            }
        }

        if (dest != null)
        {
            switch (dest.getType())
            {
                case Constants.ITEM:
                    destStr = "Item ";
                    break;
                case Constants.COLLECTION:
                    destStr = "Collection ";
                    break;
                case Constants.COMMUNITY:
                    destStr = "Community ";
                    break;
                default:
            }
        }

        sourceStr = sourceStr + (source == null ? "null" : source.getId());
        destStr = destStr + (dest == null ? "null" : dest.getId());
        dsoStr = dsoStr + (dso == null ? "null" : dso.getId());

        log.warn("***************************************************");
        log.warn("Moving " + dsoStr + " from " + sourceStr + " to " + destStr);
        log.warn("***************************************************");
    }
*/
    /**
     * Using the CLI on ArchiveManager
	 *
     * Add {dspace.dir}/bin to your path or cd {dspace.dir}/bin
     * dsrun org.dspace.core.ArchiveManager [opts] 
	 *
	 * Options
     * -i [item id] Allows for the specification of an item by id
     * -u [email or id of eperson] Allows for the use of Authorization and specifying a user 
     * -p print the given item's (-i [id]) item_id, revision, previous_revision
     * -a print the same item data as -p for all items
     * -m print the given item's metadata
     * -z print the given item's persistent identifiers
     * -r create a new revision of the given item using the given user (-u [email or id]) 
     * 
     * Should be extenisble for other actions.
     */
    public static void main(String[] argv)
    {
        Context c = null;
        try
        {
            c = new Context();
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            //ItemDAO itemDAO = ItemDAOFactory.getInstance(c);
            GroupDAO groupDAO = GroupDAOFactory.getInstance(c);

            options.addOption("a", "all", false, "print all items");
            options.addOption("m", "metadata", false, "print item metadata");
            options.addOption("p", "print", false, "print item");
            options.addOption("u", "user", true, "eperson email address or id");
            options.addOption("i", "item_id", true, "id of the item");
            options.addOption("z", "identifiers", false, "print the presistent ids");
            options.addOption("g", "group", false, "print the group info");
            CommandLine line = parser.parse(options, argv);

            if (line.hasOption("a"))
            {
                printItems(applicationService.findAllItems(c));
            }
            else if (line.hasOption('g')) 
            {
                printGroups(groupDAO.getGroups(1));
            }
            else if (line.hasOption("m") && line.hasOption("i"))
            {
            	printItemMetadata(applicationService.get(c, Item.class, Integer.parseInt(line.getOptionValue("i"))));
            }
            else if (line.hasOption("p") && line.hasOption("i"))
            {
                int id = Integer.parseInt(line.getOptionValue("i"));
                System.out.println(applicationService.get(c, Item.class, id).toString());
            }
            else if (line.hasOption("z") && line.hasOption("i"))
            {
                System.out.println("id go");
                printExternalIdentifiers(applicationService.get(c, Item.class, Integer.parseInt(line.getOptionValue("i"))));
            }
            c.complete();
        }
        catch (Exception e)
        {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Prints out the list of items using item.toString()
     * @param items List<Item>
     */
    private static void printItems(List<Item> items)
    {
        for (Item i : items)
        {
            System.out.println(i.toString());
        }
    }
    
    /**
     * Prints out the list of groups using toString()
     * @param groups Group[]
     */
    private static void printGroups(List<Group> groups)
    {
        for (Group g : groups)
        {
            System.out.println("Group: " + g.toString());
        }
    }

    /**
     * Prints out the Item's metadata twice in two forms
     * 
     * @param item Item
     */
    private static void printItemMetadata(Item item)
    {
        System.out.println(item.getMetadata().toString());
        for (Object o : item.getMetadata())
        {
            System.out.println(o.toString());
        }
    }
    
    /**
     * Prints out all the persistent Identifiers for the given Item
     * 
     * @param item item
     */
    private static void printExternalIdentifiers(Item item)
    {
    	System.out.println("one pi: " + item.getExternalIdentifier().getCanonicalForm());
        System.out.println(item.getExternalIdentifiers().toString());
        for (ExternalIdentifier id : item.getExternalIdentifiers())
        {
            System.out.println(id.getCanonicalForm());
        }
    }
    
    public static void setApplicationService(ApplicationService applicationService) {
		ArchiveManager.applicationService = applicationService;
	}
}
