package org.dspace.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.BundleFactory;

public class ItemManager {
	
	private static ApplicationService applicationService;
	
	/* Create methods */
	
	/* Creates a bundle in one item */
	public Bundle createBundle(Item item, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		Bundle bundle = BundleFactory.getInstance(context);
		addBundle(item, bundle, context);
		return bundle;
	}
	
	/* Creates a bitstream in a bundle */
	public Bitstream createBitstream(Bundle bundle, InputStream is, Context context) 
	    throws IOException, AuthorizeException {
	    if(bundle==null) {
            throw new IllegalArgumentException(
            "A Bundle owner of the bitstream is needed");
        }	 
	    Bitstream bitstream = BitstreamFactory.getInstance(context, is);
	    addBitstream(bundle, bitstream);
	    return bitstream;
	}
	
	/* Creates a bitstream inside the input item, in a bundle called "original" */
	public Bitstream createSingleBitstream(Item item, InputStream is, Context context) 
		throws IOException, AuthorizeException {
		Bitstream bitstream = BitstreamFactory.getInstance(context, is);
		Bundle bundle = applicationService.findBundleByName(item, "original", context);
		if(bundle==null) {
			bundle = createBundle(item, context);
		}
		addBitstream(bundle, bitstream);
		return bitstream;
	}
	
	
	/* Add methods */
	
	/* Adds a bundle to an item. If that bundle belongs to an another item, that relationship is broken */
	public void addBundle(Item item, Bundle bundle, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		
		//remove the bundle from any existing owner item
		if(bundle.getItem()!=null) {
		    bundle.getItem().getBundles().remove(bundle);
		}
		
		bundle.setItem(item);
		item.getBundles().add(bundle);		
	}
	
	/* Adds a bitstream to a bundle. If that bitstream belongs to an another bundle, that relationship is broken */
	public void addBitstream(Bundle bundle, Bitstream bitstream) {
		if(bundle==null) {
			throw new IllegalArgumentException(
            "A Bundle owner of the bitstream is needed");
		}
		//remove the bitstream from any existing owner bundle
		if(bitstream.getBundle()!=null) {
		    bitstream.getBundle().getBitstreams().remove(bitstream);
		}
		bitstream.setBundle(bundle);
		bundle.getBitstreams().add(bitstream);
	}
	
	/* Remove methods */
	
	/* Removes all the bundles of an item */
	protected void removeAllBundles(Item item, Context context) {
	    List<Bundle> bundles = item.getBundles();
	    for(Bundle bundle : bundles) {
	        //removeBundle(item, bundle, context);
	        List<Bitstream> bitstreams = bundle.getBitstreams();
	        for(Bitstream bitstream : bitstreams) {
	            bitstream.setDeleted(true);
	            bitstream.setBundle(null);
	        }
	        applicationService.delete(context, Bundle.class, bundle);
	    }
	    item.setBundles(null);
	}
	
	/* Removes and deletes a bundle from an item */
	public void removeBundle(Item item, Bundle bundle, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		item.getBundles().remove(bundle);
		
		//bundle is now orphan, delete it and its bitstreams FIXME forse una query è più efficiente?
		List<Bitstream> bitstreams = bundle.getBitstreams();
		for(Bitstream bitstream : bitstreams) {
		    bitstream.setDeleted(true);
		    bitstream.setBundle(null);
		}
		
		applicationService.delete(context, Bundle.class, bundle);
	}
	
	/* Removes and deletes a bitstream from a bundle */
	public void removeBitstream(Bundle bundle, Bitstream bitstream) {
		if(bundle==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		bundle.getBitstreams().remove(bitstream);
		//no need to delete it using applicationservice
		bitstream.setDeleted(true);
	}
	
    public static void setApplicationService(ApplicationService applicationService)
    {
        ItemManager.applicationService = applicationService;
    }
	
}
