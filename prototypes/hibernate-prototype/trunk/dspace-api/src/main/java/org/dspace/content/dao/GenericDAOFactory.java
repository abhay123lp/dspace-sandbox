/* *
 * @author Daniele Ninfo
 */

package org.dspace.content.dao;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.storage.dao.CRUD;


public class GenericDAOFactory {
	
	public <T> CRUD<T> getInstance(Class<T> clazz, Context context) {
		if(clazz==Community.class) {
			return (CRUD<T>)CommunityDAOFactory.getInstance(context);
		} else if(clazz==Collection.class) {
			return (CRUD<T>)CollectionDAOFactory.getInstance(context);
		} else if(clazz==Item.class) {
			return (CRUD<T>)ItemDAOFactory.getInstance(context);
		} else if(clazz==Bundle.class) {
			return (CRUD<T>)BundleDAOFactory.getInstance(context);
		} else if(clazz==Bitstream.class) {
			return (CRUD<T>)BitstreamDAOFactory.getInstance(context);
		} else if(clazz==BitstreamFormat.class) {
			return (CRUD<T>)BitstreamFormatDAOFactory.getInstance(context);
		} else if(clazz==MetadataField.class) {
			return (CRUD<T>)MetadataFieldDAOFactory.getInstance(context);
		} else if(clazz==MetadataValue.class) {
			return (CRUD<T>)MetadataValueDAOFactory.getInstance(context);
		} else if(clazz==MetadataSchema.class) {
			return (CRUD<T>)MetadataSchemaDAOFactory.getInstance(context);
		} else if(clazz==SupervisedItem.class) {
			return (CRUD<T>)SupervisedItemDAOFactory.getInstance(context);
		} else if(clazz==WorkspaceItem.class) {
			return (CRUD<T>)WorkspaceItemDAOFactory.getInstance(context);
		}
		
		else return null;
		
	}
}
