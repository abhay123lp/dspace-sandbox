/*
 * MetadataSchemaDAO.java
 *
 * Version: $Revision: 427 $
 *
 * Date: $Date: 2007-08-07 17:32:39 +0100 (Tue, 07 Aug 2007) $
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

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.dao.CRUD;

public abstract class MetadataSchemaDAO extends ContentDAO
    implements CRUD<MetadataSchema>
{
    protected Logger log = Logger.getLogger(MetadataSchemaDAO.class);

    protected Context context;

    public MetadataSchemaDAO(Context context)
    {
        this.context = context;
    }

//    public MetadataSchema create() throws AuthorizeException
//    {
//        // Check authorisation: Only admins may create metadata schemas
//        if (!AuthorizeManager.isAdmin(context))
//        {
//            throw new AuthorizeException(
//                    "Only administrators may modify the metadata registry");
//        }
//
//        return null;
//    }

//    protected final MetadataSchema create(MetadataSchema schema)
//    {
//        log.info(LogManager.getHeader(context, "create_metadata_schema",
//                    "metadata_schema_id=" + schema.getId()));
//
//        return schema;
//    }
//
//    public MetadataSchema retrieve(int id)
//    {
//        return (MetadataSchema) context.fromCache(MetadataSchema.class, id);
//    }

    public MetadataSchema retrieve(UUID uuid)
    {
        return null;
    }

    /**
     * Get the schema object corresponding to this short name (eg: dc).
     */
//    public MetadataSchema retrieveByName(String namespace)
//    {
//        return null;
//    }
    
    public abstract MetadataSchema findMetadataSchemaByName(String name, Context context);
    public abstract MetadataSchema findMetadataSchemaByNamespace(String namespace, Context context);

    /**
     * Get the schema object corresponding to this namespace URI.
     */
//    public MetadataSchema retrieveByNamespace(String namespace)
//    {
//        return null;
//    }

//    public void update(MetadataSchema schema) throws AuthorizeException
//    {
//        // Check authorisation: Only admins may create metadata schemas
//        if (!AuthorizeManager.isAdmin(context))
//        {
//            throw new AuthorizeException(
//                    "Only administrators may modify the metadata registry");
//        }
//
//        int id = schema.getId();
//        String name = schema.getName();
//        String namespace = schema.getNamespace();
//
//        // Ensure the schema name is unique
//        if (!uniqueShortName(id, name))
//        {
//            throw new RuntimeException(
//                    new NonUniqueMetadataException("Please make the name " + name
//                    + " unique"));
//        }
//
//        // Ensure the schema namespace is unique
//        if (!uniqueNamespace(id, namespace))
//        {
//            throw new RuntimeException(
//                    new NonUniqueMetadataException("Please make the namespace "
//                        + namespace + " unique"));
//        }
//
//        log.info(LogManager.getHeader(context, "update_metadata_schema",
//                    "metadata_schema_id=" + id +
//                    "namespace=" + namespace +
//                    "name=" + name));
//    }

//    public void delete(int id) throws AuthorizeException
//    {
//        MetadataSchema schema = retrieve(id);
//        update(schema); // Sync in-memory object before removal
//
//        if (!AuthorizeManager.isAdmin(context))
//        {
//            throw new AuthorizeException(
//                    "Only administrators may modify the metadata registry");
//        }
//
//        // Ideally, we'd log the action after the operation had taken place,
//        // but it's not desperately important.
//        log.info(LogManager.getHeader(context, "delete_metadata_schema",
//                "metadata_schema_id=" + id));
//
//        MetadataFieldDAO dao = MetadataFieldDAOFactory.getInstance(context);
//        for (MetadataField field : dao.getMetadataFields(id))
//        {
//            dao.delete(field.getId());
//        }
//
//        context.removeCached(schema, id);
//    }

//    /**
//     * Return true if and only if the passed name appears within the allowed
//     * number of times in the current schema.
//     */
//    protected abstract boolean uniqueNamespace(int id, String namespace);

//    /**
//     * Return true if and only if the passed name is unique.
//     */
//    protected abstract boolean uniqueShortName(int id, String name);

//    public abstract List<MetadataSchema> getMetadataSchemas();
    public abstract List<MetadataSchema> findAllMetadataSchema(Context context);
}
