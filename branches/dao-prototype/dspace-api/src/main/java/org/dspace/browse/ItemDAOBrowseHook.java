package org.dspace.browse;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class ItemDAOBrowseHook extends ItemDAO
{
    private ItemDAO childDAO;

    public ItemDAOBrowseHook()
    {
    }

    public ItemDAOBrowseHook(Context context)
    {
        super(context);
    }

    public ItemDAO getChild()
    {
        return childDAO;
    }

    public void setChild(ItemDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Item create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Item retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Item retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Item item) throws AuthorizeException
    {
        childDAO.update(item);
    }

    public void delete(int id) throws AuthorizeException
    {
        // Remove from indices, if appropriate
        /** XXX FIXME
         ** NB Do not check to see if the item is archived - withdrawn /
         ** non-archived items may still be tracked in some browse tables
         ** for administrative purposes, and these need to be removed.
         **/
        // Remove from Browse indices
        try
        {
            IndexBrowse ib = new IndexBrowse(context);
            ib.itemRemoved(id);
            // FIXME: I think this is unnecessary because it is taken care
            // of by the search consumer, but I'm not sure.
            // DSIndexer.unIndexContent(context, item);
        }
        catch (BrowseException e)
        {
            throw new RuntimeException(e);
        }

        childDAO.delete(id);
    }

    public void decache(Item item)
    {
        childDAO.decache(item);
    }

    public List<Item> getItems()
    {
        return childDAO.getItems();
    }

    public List<Item> getItems(DSpaceObject scope,
                               String startDate, String endDate,
                               int offset, int limit,
                               boolean items, boolean collections,
                               boolean withdrawn)
            throws ParseException
    {
        return childDAO.getItems(scope, startDate, endDate, offset, limit,
                items, collections, withdrawn);
    }

    public List<Item> getItems(MetadataValue value)
    {
        return childDAO.getItems(value);
    }

    public List<Item> getItems(MetadataValue value, Date startDate, Date endDate)
    {
        return childDAO.getItems(value, startDate, endDate);
    }

    public List<Item> getItemsByCollection(Collection collection)
    {
        return childDAO.getItemsByCollection(collection);
    }

    public List<Item> getItemsBySubmitter(EPerson eperson)
    {
        return childDAO.getItemsBySubmitter(eperson);
    }

    public List<Item> getParentItems(Bundle bundle)
    {
        return childDAO.getParentItems(bundle);
    }

    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        childDAO.link(item, bundle);
    }

    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        childDAO.unlink(item, bundle);
    }

    public boolean linked(Item item, Bundle bundle)
    {
        return childDAO.linked(item, bundle);
    }

    public void loadMetadata(Item item)
    {
        childDAO.loadMetadata(item);
    }

    public List<DCValue> getMetadata(Item item, String schema, String element,
                                     String qualifier, String lang)
    {
        return childDAO.getMetadata(item, schema, element, qualifier, lang);
    }
}
