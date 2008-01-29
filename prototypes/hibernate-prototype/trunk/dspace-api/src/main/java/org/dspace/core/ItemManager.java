package org.dspace.core;

import java.io.IOException;
import java.io.InputStream;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.BitstreamFactory;
import org.dspace.content.factory.BundleFactory;

public class ItemManager {
	
	private static ApplicationService applicationService;
	
	/* Create methods */
	
	public Bundle createBundle(Item item, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		Bundle bundle = BundleFactory.getInstance(context);
		addBundle(item, bundle, context);
		return bundle;
	}
	
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
	
	public void addBundle(Item item, Bundle bundle, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		if(bundle.getItem()!=null) {
			removeBundle(item, bundle, context);
		}
		
		bundle.setItem(item);
		item.getBundles().add(bundle);		
	}
	
	public void addBitstream(Bundle bundle, Bitstream bitstream) {
		if(bundle==null) {
			throw new IllegalArgumentException(
            "A Bundle owner of the bitstream is needed");
		}
		if(bitstream.getBundle()!=null) {
			removeBitstream(bundle, bitstream);
		}
		bitstream.setBundle(bundle);
		bundle.getBitstreams().add(bitstream);
	}
	
	/* Remove methods */
	
	public void removeBundle(Item item, Bundle bundle, Context context) {
		if(item==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		item.getBundles().remove(bundle);
		//bundle is now orphan
		applicationService.deleteBundle(context, bundle);
	}
	
	public void removeBitstream(Bundle bundle, Bitstream bitstream) {
		if(bundle==null) {
			throw new IllegalArgumentException(
            "A Item owner of the bundle is needed");
		}
		bundle.getBitstreams().remove(bitstream);
		//no need to delete it using applicationservice
		bitstream.setDeleted(true);
	}
}
