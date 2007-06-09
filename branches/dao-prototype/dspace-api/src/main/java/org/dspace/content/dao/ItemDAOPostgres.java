/*
 * ItemDAOPostgres.java
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
package org.dspace.content.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.proxy.ItemProxy;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.content.uri.PersistentIdentifier;
import org.dspace.content.uri.dao.PersistentIdentifierDAO;
import org.dspace.content.uri.dao.PersistentIdentifierDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 * @author Richard Jones
 */
public class ItemDAOPostgres extends ItemDAO
{
    private PersistentIdentifierDAO identifierDAO;

    /** query to obtain all the items from the database */
    private final String findAll = "SELECT * FROM item";
    
    /** query to check the existance of an item id */
    private final String getByID = "SELECT id FROM item WHERE item_id = ?";
        
    /** query to get the text value of a metadata element only (qualifier is NULL) */
    private final String getByMetadataElement =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id = ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? AND qualifier IS NULL " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";
    
    /** query to get the text value of a metadata element and qualifier */
    private final String getByMetadata =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id = ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? AND qualifier = ? " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";
    
    /** query to get the text value of a metadata element with the wildcard
     * qualifier (*) */
    private final String getByMetadataAnyQualifier =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id IN ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";


    public ItemDAOPostgres(Context context)
    {
        if (context != null)
        {
            this.context = context;
            this.bundleDAO = BundleDAOFactory.getInstance(context);
            this.identifierDAO =
                PersistentIdentifierDAOFactory.getInstance(context);
        }
    }

    @Override
    public Item create() throws AuthorizeException
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "item");
            row.setColumn("uuid", UUID.randomUUID().toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("item_id");

            return super.create(id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Item retrieve(int id)
    {
        Item item = super.retrieve(id);

        if (item != null)
        {
            return item;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "item", id);

            if (row == null)
            {
                log.warn("item " + id + " not found");
                return null;
            }

            item = new ItemProxy(context, id);
            populateItemFromTableRow(item, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<PersistentIdentifier> identifiers =
                identifierDAO.getPersistentIdentifiers(item);
            item.setPersistentIdentifiers(identifiers);

            context.cache(item, id);

            return item;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Item retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "item",
                    "uuid", uuid.toString());

            if (row == null)
            {
                log.warn("item " + uuid + " not found");
                return null;
            }

            int id = row.getIntColumn("item_id");
            Item item = new ItemProxy(context, id);
            populateItemFromTableRow(item, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<PersistentIdentifier> identifiers =
                identifierDAO.getPersistentIdentifiers(item);
            item.setPersistentIdentifiers(identifiers);

            context.cache(item, id);

            return item;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Item item) throws AuthorizeException
    {
        super.update(item);

        try
        {
            TableRow row = DatabaseManager.find(context, "item", item.getID());

            if (row != null)
            {
                update(item, row);
            }
            else
            {
                throw new RuntimeException("Didn't find item " + item.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Very thin adaptation of the old Item.update().
     */
    private void update(Item item, TableRow itemRow) throws AuthorizeException
    {
        try
        {
            // Fill out the TableRow and save it
            populateTableRowFromItem(item, itemRow);
            itemRow.setColumn("last_modified", new Date());
            DatabaseManager.update(context, itemRow);

            // Map counting number of values for each element/qualifier.
            // Keys are Strings: "element" or "element.qualifier"
            // Values are Integers indicating number of values written for a
            // element/qualifier
            Map elementCount = new HashMap();

            // FIXME: We need to be cunning about what we write to the database
//            if (dublinCoreChanged)
//            {
                // Remove existing metadata
                removeMetadataFromDatabase(item.getID());

                // Add in-memory metadata
                for (DCValue dcv : item.getMetadata())
                {
                    // Get the DC Type
                    int schemaID;
                    MetadataSchema schema =
                        MetadataSchema.find(context, dcv.schema);

                    if (schema == null)
                    {
                        schemaID = MetadataSchema.DC_SCHEMA_ID;
                    }
                    else
                    {
                        schemaID = schema.getSchemaID();
                    }

                    MetadataField field = MetadataField.findByElement(context,
                            schemaID, dcv.element, dcv.qualifier);

                    if (field == null)
                    {
                        // Bad DC field, log and throw exception
                        log.warn(LogManager.getHeader(context, "bad_dc",
                            "Bad DC field. SchemaID=" +
                            String.valueOf(schemaID) +
                            ", element: \"" + ((dcv.element == null)
                                ? "null" : dcv.element) +
                            "\" qualifier: \"" + ((dcv.qualifier == null)
                                ? "null" : dcv.qualifier) +
                            "\" value: \"" + ((dcv.value == null)
                                ? "null" : dcv.value) + "\""));

                        throw new SQLException("bad_dublin_core " +
                                "SchemaID=" + String.valueOf(schemaID) + ", " +
                                dcv.element + " " + dcv.qualifier);
                    }

                    // Work out the place number for ordering
                    int current = 0;

                    // Key into map is "element" or "element.qualifier"
                    String key = dcv.element +
                            ((dcv.qualifier == null)
                             ? "" : ("." + dcv.qualifier));

                    Integer currentInteger = (Integer) elementCount.get(key);

                    if (currentInteger != null)
                    {
                        current = currentInteger.intValue();
                    }

                    current++;
                    elementCount.put(key, new Integer(current));

                    // Write DCValue
                    MetadataValue metadata = new MetadataValue();
                    metadata.setItemId(item.getID());
                    metadata.setFieldId(field.getFieldID());
                    metadata.setValue(dcv.value);
                    metadata.setLanguage(dcv.language);
                    metadata.setPlace(current);
                    metadata.create(context);
                }

//                dublinCoreChanged = false;
//            }

            // Update browse indices
            Browse.itemChanged(context, item);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        super.delete(id);

        try
        {
            removeMetadataFromDatabase(id);

            DatabaseManager.delete(context, "item", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItems()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT item_id FROM item WHERE in_archive = '1'");

            List<Item> items = new ArrayList<Item>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("item_id");
                items.add(retrieve(id));
            }

            return items;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItemsByCollection(Collection collection)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT i.item_id " +
                    "FROM item i, collection2item c2i " +
                    "WHERE i.item_id = c2i.item_id "+ 
                    "AND c2i.collection_id = ? " + 
                    "AND i.in_archive = '1'",
                    collection.getID());

            List<Item> items = new ArrayList<Item>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("item_id");
                items.add(retrieve(id));
            }

            return items;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItemsBySubmitter(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT item_id FROM item " +
                    "WHERE in_archive = '1' " +
                    "AND submitter_id = ? ",
                    eperson.getID());

            List<Item> items = new ArrayList<Item>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("item_id");
                items.add(retrieve(id));
            }

            return items;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getParentItems(Bundle bundle)
    {
        try
        {
            // Get items
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT i.item_id FROM item i, item2bundle i2b " +
                    "WHERE i2b.item_id = i.item_id " +
                    "AND i2b.bundle_id = ? ",
                    bundle.getID());

            List<Item> items = new ArrayList<Item>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("item_id");
                items.add(retrieve(id));
            }

            return items;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        if (!linked(item, bundle))
        {
            super.link(item, bundle);

            try
            {
                TableRow row = DatabaseManager.create(context, "item2bundle");
                row.setColumn("item_id", item.getID());
                row.setColumn("bundle_id", bundle.getID());
                DatabaseManager.update(context, row);

                // If we're adding the Bundle to the Item, we bequeath our
                // policies unto it.
                AuthorizeManager.inheritPolicies(context, item, bundle);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    @Override
    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        if (linked(item, bundle))
        {
            super.unlink(item, bundle);

            try
            {
                // Remove bundle mappings from DB
                DatabaseManager.updateQuery(context,
                        "DELETE FROM item2bundle WHERE item_id= ? " +
                        "AND bundle_id= ? ",
                        item.getID(), bundle.getID());
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    private boolean linked(Item item, Bundle bundle)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT id FROM item2bundle " +
                    " WHERE item_id=" + item.getID() +
                    " AND bundle_id=" + bundle.getID());

            return tri.hasNext();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    //
    // Note: the methods below marked public should really be in the base Item
    // class (or the proxy), but until DAOs are adopted system-wide, we need to
    // take care of it here. For instance, getCollection() should really
    // interrogate CollectionDAO.retrieve(id), but that doesn't exist yet.
    ////////////////////////////////////////////////////////////////////

    private void populateTableRowFromItem(Item item, TableRow row)
    {
        EPerson submitter = item.getSubmitter();
        Collection owningCollection = item.getOwningCollection();

        row.setColumn("item_id", item.getID());
        row.setColumn("in_archive", item.isArchived());
        row.setColumn("withdrawn", item.isWithdrawn());
        row.setColumn("last_modified", item.getLastModified());

        if (submitter != null)
        {
            row.setColumn("submitter_id", submitter.getID());
        }

        if (owningCollection != null)
        {
            row.setColumn("owning_collection", owningCollection.getID());
        }
    }

    private void populateItemFromTableRow(Item item, TableRow row)
    {
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        int submitterId = row.getIntColumn("submitter_id");
        int owningCollectionId = row.getIntColumn("owning_collection");
        boolean inArchive = row.getBooleanColumn("in_archive");
        boolean withdrawn = row.getBooleanColumn("withdrawn");
        Date lastModified = row.getDateColumn("last_modified");

        item.setIdentifier(new ObjectIdentifier(uuid));
        item.setSubmitter(submitterId);
        item.setOwningCollectionId(owningCollectionId);
        item.setArchived(inArchive);
        item.setWithdrawn(withdrawn);
        item.setLastModified(lastModified);
    }

    @Override
    public void loadMetadata(Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "MetadataValue",
                    "SELECT * FROM MetadataValue " +
                    "WHERE item_id = ? " +
                    "ORDER BY metadata_field_id, place",
                    item.getID());

            List<DCValue> metadata = new ArrayList<DCValue>();

            for (TableRow row : tri.toList())
            {
                // Get the associated metadata field and schema information
                int fieldID = row.getIntColumn("metadata_field_id");
                MetadataField field = MetadataField.find(context, fieldID);

                if (field == null)
                {
                    log.error("Loading item - cannot found metadata field "
                            + fieldID);
                }
                else
                {
                    MetadataSchema schema = MetadataSchema.find(
                            context, field.getSchemaID());

                    // Make a DCValue object
                    DCValue dcv = new DCValue();
                    dcv.schema = schema.getName();
                    dcv.element = field.getElement();
                    dcv.qualifier = field.getQualifier();
                    dcv.language = row.getStringColumn("text_lang");
                    dcv.value = row.getStringColumn("text_value");

                    // Add it to the item
                    metadata.add(dcv);
                }
            }

            item.setMetadata(metadata);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
    
    /**
     * Perform a database query to obtain the string array of values
     * corresponding to the passed parameters. This is only really called from
     * 
     * <code>
     * getMetadata(schema, element, qualifier, lang);
     * </code>
     * 
     * which will obtain the value from cache if available first.
     * 
     * @param schema
     * @param element
     * @param qualifier
     * @param lang
     */
    @Override
    public List<DCValue> getMetadata(Item item, String schema, String element,
            String qualifier, String lang)
    {
        List<DCValue> metadata = new ArrayList<DCValue>();
        try
        {
            TableRowIterator tri;
            
            if (qualifier == null)
            {
                Object[] params = { item.getID(), element, schema };
                tri = DatabaseManager.query(context, getByMetadataElement,
                        params);
            }
            else if (Item.ANY.equals(qualifier))
            {
                Object[] params = { item.getID(), element, schema };
                tri = DatabaseManager.query(context, getByMetadataAnyQualifier,
                        params);
            }
            else
            {
                Object[] params = { item.getID(), element, qualifier, schema };
                tri = DatabaseManager.query(context, getByMetadata, params);
            }
            
            while (tri.hasNext())
            {
                TableRow tr = tri.next();
                DCValue dcv = new DCValue();
                dcv.schema = schema;
                dcv.element = element;
                dcv.qualifier = qualifier;
                dcv.language = lang;
                dcv.value = tr.getStringColumn("text_value");
                metadata.add(dcv);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
        return metadata;
    }

    private void removeMetadataFromDatabase(int itemId) throws SQLException
    {
        DatabaseManager.updateQuery(context,
                "DELETE FROM MetadataValue WHERE item_id= ? ",
                itemId);
    }
}
