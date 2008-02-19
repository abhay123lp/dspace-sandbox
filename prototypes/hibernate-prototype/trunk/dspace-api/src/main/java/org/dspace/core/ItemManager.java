package org.dspace.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.BundleFactory;
import org.dspace.eperson.EPerson;
import org.dspace.search.DSIndexer;

public class ItemManager
{

    /* Create methods */

    /* Creates a bundle in one item */
    public static Bundle createBundle(Item item, Context context)
    {
        if (item == null)
        {
            throw new IllegalArgumentException(
                    "A Item owner of the bundle is needed");
        }
        Bundle bundle = BundleFactory.getInstance(context);
        addBundle(item, bundle, context);
        ApplicationService.save(context, Bundle.class, bundle);
        return bundle;
    }
    //FIXME da deprecare? da cancellare?
    public static Bundle createBundle(Item item, String name, Context context)
    {
        if (item == null)
        {
            throw new IllegalArgumentException(
                    "A Item owner of the bundle is needed");
        }
        Bundle bundle = BundleFactory.getInstance(context);
        bundle.setName(name);
        addBundle(item, bundle, context);
        return bundle;
    }


    /* Creates a bitstream in a bundle */
    public static Bitstream createBitstream(Bundle bundle, InputStream is,
            Context context) throws IOException, AuthorizeException
    {
        if (bundle == null)
        {
            throw new IllegalArgumentException(
                    "A Bundle owner of the bitstream is needed");
        }
        Bitstream bitstream = BitstreamFactory.getInstance(context, is);
        System.out.println("---> " + bitstream.getId());
        addBitstream(bundle, bitstream);
        
        return bitstream;
    }
        

    /* Creates a bitstream inside the input item, in a bundle called "original" */
    public static Bitstream createSingleBitstream(Item item, InputStream is, String name, 
            Context context) throws IOException, AuthorizeException
    {
        
        Bitstream bitstream = BitstreamFactory.getInstance(context, is);
        Bundle bundle = ApplicationService.findBundleByName(item, name,
                context);
        if (bundle == null)
        {
            bundle = createBundle(item, context);
        }
        addBitstream(bundle, bitstream);
        return bitstream;
    }

    /* Add methods */

    /*
     * Adds a bundle to an item. If that bundle belongs to an another item, that
     * relationship is broken
     */
    public static void addBundle(Item item, Bundle bundle, Context context)
    {
        if (item == null)
        {
            throw new IllegalArgumentException(
                    "A Item owner of the bundle is needed");
        }

        // remove the bundle from any existing owner item
        if (bundle.getItem() != null)
        {
            bundle.getItem().getBundles().remove(bundle);
        }

        bundle.setItem(item);
        item.getBundles().add(bundle);
    }

    /*
     * Adds a bitstream to a bundle. If that bitstream belongs to an another
     * bundle, that relationship is broken
     */
    public static void addBitstream(Bundle bundle, Bitstream bitstream)
    {
        if (bundle == null)
        {
            throw new IllegalArgumentException(
                    "A Bundle owner of the bitstream is needed");
        }
        // remove the bitstream from any existing owner bundle
        if (bitstream.getBundle() != null)
        {
            bitstream.getBundle().getBitstreams().remove(bitstream);
        }
        bitstream.setBundle(bundle);
        bundle.getBitstreams().add(bitstream);
    }

    /* Remove methods */

    /* Removes all the bundles of an item */
    protected static void removeAllBundles(Item item, Context context)
    {
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            // removeBundle(item, bundle, context);
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams)
            {
                bitstream.setDeleted(true);
                bitstream.setBundle(null);
            }
            ApplicationService.delete(context, Bundle.class, bundle);
        }
        item.setBundles(null);
    }

    /* Removes and deletes a bundle from an item */
    public static void removeBundle(Item item, Bundle bundle, Context context)
    {
        if (item == null)
        {
            throw new IllegalArgumentException(
                    "A Item owner of the bundle is needed");
        }
        item.getBundles().remove(bundle);

        // bundle is now orphan, delete it and its bitstreams FIXME forse una
        // query è più efficiente?
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams)
        {
            bitstream.setDeleted(true);
            bitstream.setBundle(null);
        }

        ApplicationService.delete(context, Bundle.class, bundle);
    }

    /* Removes and deletes a bitstream from a bundle */
    public static void removeBitstream(Bundle bundle, Bitstream bitstream)
    {
        if (bundle == null)
        {
            throw new IllegalArgumentException(
                    "A Item owner of the bundle is needed");
        }
        bundle.getBitstreams().remove(bitstream);
        // no need to delete it using applicationservice
        bitstream.setDeleted(true);
    }

    public static void licenseGranted(Item item, String license, EPerson eperson, Context context)
            throws IOException, AuthorizeException
    {
        // Put together text to store
        String licenseText = "License granted by " + eperson.getFullName()
                + " (" + eperson.getEmail() + ") on "
                + DCDate.getCurrent().toString() + " (GMT):\n\n" + license;

        // Store text as a bitstream
        byte[] licenseBytes = licenseText.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(licenseBytes);
        //Bitstream b = createSingleBitstream(bais, "LICENSE");
        Bitstream b = createSingleBitstream(item, bais, "LICENSE", context);

        // Now set the format and name of the bitstream
        b.setName("license.txt");
        b.setSource("Written by org.dspace.content.Item");

        // Find the License format

        //BitstreamFormat bf = bitstreamFormatDAO.retrieveByShortDescription("License");
        BitstreamFormat bf = ApplicationService
                .findBitstreamFormatByShortDescription("License", context);

        b.setFormat(bf);

        //bitstreamDAO.update(b);
    }
    
    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     * 
     * FIXME uguale al find dell'as?
     */
    public static void withdrawItem(Context context, Item item)
        throws AuthorizeException, IOException
    {
        //ItemDAO itemDAO = ItemDAOFactory.getInstance(context);

        String timestamp = DCDate.getCurrent().toString();

        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = (Collection[])item.getCollections().toArray();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getId() + ")\n";
        }

        // Check permission. User either has to have REMOVE on owning
        // collection or be COLLECTION_EDITOR of owning collection
        if (!AuthorizeManager.authorizeActionBoolean(context,
                item.getOwningCollection(), Constants.COLLECTION_ADMIN)
                && !AuthorizeManager.authorizeActionBoolean(context,
                        item.getOwningCollection(), Constants.REMOVE))
        {
            throw new AuthorizeException("To withdraw item must be " +
                    "COLLECTION_ADMIN or have REMOVE authorization on " +
                    "owning Collection.");
        }

        item.setWithdrawn(true);
        item.setArchived(false);

        EPerson e = context.getCurrentUser();
        try
        {
            // Add suitable provenance - includes user, date, collections +
            // bitstream checksums
            String prov = "Item withdrawn by " + e.getFullName() + " ("
                    + e.getEmail() + ") on " + timestamp + "\n"
                    + "Item was in collections:\n" + collectionProv
                    + InstallItem.getBitstreamProvenanceMessage(item);
            MetadataField mdf = ApplicationService.getMetadataField("description", "provenance", MetadataSchema.DC_SCHEMA, context);
            item.addMetadata(mdf, "en", prov);

            // Update item in DB
            //itemDAO.update(item);

            // Remove from indicies
            IndexBrowse ib = new IndexBrowse(context);
            ib.itemRemoved(item);
            DSIndexer.unIndexContent(context, item);
        }
        catch (BrowseException be)
        {
            throw new RuntimeException(be);
        }

        // and all of our authorization policies
        
        AuthorizeManager.removeAllPolicies(context, item);

//        log.info(LogManager.getHeader(context, "withdraw_item", "user="
//                + e.getEmail() + ",item_id=" + item.getId()));
    }

    
    //Reinstate a withdrawn item.
     
    public static void reinstateItem(Context context, Item item)
        throws AuthorizeException, IOException
    {
        //ItemDAO itemDAO = ItemDAOFactory.getInstance(context);

        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = (Collection[])item.getCollections().toArray();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getId() + ")\n";
            AuthorizeManager.authorizeAction(context, colls[i],
                    Constants.ADD);
        }

        item.setWithdrawn(false);
        item.setArchived(true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = context.getCurrentUser();

        String prov = "Item reinstated by " + e.getFullName() + " ("
                + e.getEmail() + ") on " + timestamp + "\n"
                + "Item was in collections:\n" + collectionProv
                + InstallItem.getBitstreamProvenanceMessage(item);
        
        MetadataField mdf = ApplicationService.getMetadataField("description", "provenance", MetadataSchema.DC_SCHEMA, context);
        item.addMetadata(mdf, "en", prov);

        // Update item in DB
        //itemDAO.update(item);

        // Add to indicies
        // Remove - update() already performs this
        // Browse.itemAdded(context, this);
        DSIndexer.indexContent(context, item);

        // authorization policies
        if (colls.length > 0)
        {
            // 
            // policies from first collection
            // remove the item's policies and replace them with
            // the defaults from the collection
            item.inheritCollectionDefaultPolicies(colls[0]);
        }

//        log.info(LogManager.getHeader(context, "reinstate_item", "user="
//                + e.getEmail() + ",item_id=" + item.getId()));
    }

}
