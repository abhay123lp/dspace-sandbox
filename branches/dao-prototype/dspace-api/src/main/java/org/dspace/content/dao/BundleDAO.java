package org.dspace.content.dao;

import java.util.UUID;
import java.util.List;

import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public abstract class BundleDAO extends ContentDAO<BundleDAO>
        implements CRUD<Bundle>, Link<Bundle, Bitstream>
{
    protected Context context;

    public BundleDAO(Context context)
    {
        this.context = context;
    }

    public abstract BundleDAO getChild();

    public abstract void setChild(BundleDAO childDAO);

    public abstract Bundle create() throws AuthorizeException;

    public abstract Bundle retrieve(int id);

    public abstract Bundle retrieve(UUID uuid);

    public abstract void update(Bundle bundle) throws AuthorizeException;

    public abstract void delete(int id) throws AuthorizeException;

    public abstract List<Bundle> getBundles(Item item);

    public abstract List<Bundle> getBundles(Bitstream bitstream);

    public abstract void link(Bundle bundle, Bitstream bitstream)
            throws AuthorizeException;

    public abstract void unlink(Bundle bundle, Bitstream bitstream)
            throws AuthorizeException;

    public abstract boolean linked(Bundle bundle, Bitstream bitstream);
}
