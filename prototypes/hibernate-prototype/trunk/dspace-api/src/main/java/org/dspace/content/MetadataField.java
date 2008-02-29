/*
 * MetadataField.java
 *
 * Version: $Revision: 1721 $
 *
 * Date: $Date: 2008-01-24 16:57:38 +0100 (gio, 24 gen 2008) $
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
package org.dspace.content;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.core.ApplicationService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * DSpace object that represents a metadata field, which is
 * defined by a combination of schema, element, and qualifier.  Every
 * metadata element belongs in a field.
 *
 * @author Martin Hald
 * @version $Revision: 1721 $
 * @see org.dspace.content.MetadataValue, org.dspace.content.MetadataSchema
 */
@Entity
@Table(name="metadatafieldregistry")
public class MetadataField
{
    private static Logger log = Logger.getLogger(MetadataField.class);

    private Context context;
//    private MetadataFieldDAO dao;

    private int id;
    private MetadataSchema schema;
    private String element;
    private String qualifier;
    private String scopeNote;

    protected MetadataField() {}
    
    public MetadataField(Context context)
    {
        this.context = context;
//        this.id = id;
    }

    /**
     * Get the element name.
     *
     * @return element name
     */
    @Column(name="element")
    public String getElement()
    {
        return element;
    }

    /**
     * Set the element name.
     *
     * @param element new value for element
     */
    public void setElement(String element)
    {
        this.element = element;
    }

    /**
     * Get the metadata field id.
     *
     * @return metadata field id
     */
    @Id
    public int getId()
    {
        return id;
    }

    /**
     * Get the qualifier.
     *
     * @return qualifier
     */
    @Column(name="qualifier")
    public String getQualifier()
    {
        return qualifier;
    }

    /**
     * Set the qualifier.
     *
     * @param qualifier new value for qualifier
     */
    public void setQualifier(String qualifier)
    {
        this.qualifier = qualifier;
    }

    /**
     * Get the schema record key.
     *
     * @return schema record key
     */
    @ManyToOne
    @JoinColumn(name="metadata_schema_id")
    public MetadataSchema getSchema()
    {
        return schema;
    }

    /**
     * Set the schema record key.
     *
     * @param schemaID new value for key
     */
    public void setSchema(MetadataSchema schema)
    {
        this.schema = schema;
    }

    /**
     * Get the scope note.
     *
     * @return scope note
     */
    @Column(name="scope_note")
    public String getScopeNote()
    {
        return scopeNote;
    }

    /**
     * Set the scope note.
     *
     * @param scopeNote new value for scope note
     */
    public void setScopeNote(String scopeNote)
    {
        this.scopeNote = scopeNote;
    }

//    @Deprecated
//    public static MetadataField findByElement(Context context, int schemaID,
//            String element, String qualifier)
//    {
//        return MetadataFieldDAOFactory.getInstance(context).retrieve(schemaID,
//                element, qualifier);
//    }

//    @Deprecated
//    public static List<MetadataField> findAll(Context context)
//    {
//        MetadataFieldDAO dao = MetadataFieldDAOFactory.getInstance(context);
//        List<MetadataField> fields = dao.getMetadataFields();
//
//        return fields;
//    }

    @Deprecated
    public static MetadataField[] findAllInSchema(Context context, int schemaID)
    {
        MetadataFieldDAO dao = MetadataFieldDAOFactory.getInstance(context);
//        List<MetadataField> fields = dao.getMetadataFields(schemaID);
        MetadataSchema schema = ApplicationService.get(context, MetadataSchema.class, schemaID);
        List<MetadataField> fields = ApplicationService.findMetadataFields(schema, context);

        return (MetadataField[]) fields.toArray(new MetadataField[0]);
    }

//    @Deprecated
//    public void update(Context context) throws AuthorizeException
//    {
//        dao.update(this);
//    }

//    @Deprecated
//    public void delete(Context context) throws AuthorizeException
//    {
//        dao.delete(getId());
//    }

    /**
     * Return the HTML FORM key for the given field.
     *
     * FIXME: This is so utterly horrid and wrong I can't quite find the words.
     * 
     * @param schema
     * @param element
     * @param qualifier
     * @return HTML FORM key
     */
    @Deprecated
    public static String formKey(String schema, String element, String qualifier)
    {
        if (qualifier == null)
        {
            return schema + "_" + element;
        }
        else
        {
            return schema + "_" + element + "_" + qualifier;
        }
    }

//    @Deprecated
//    public static MetadataField find(Context context, int id)
//    {
//        return MetadataFieldDAOFactory.getInstance(context).retrieve(id);
//    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

	public void setId(int id) {
		this.id = id;
	}
}
