/*
 * MetadataValue.java
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.content.dao.MetadataSchemaDAOFactory;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.content.dao.MetadataValueDAOFactory;
import org.dspace.core.ApplicationService;
import org.dspace.core.Context;

/**
 * Class representing a metadata value. It represents a value of a given
 * <code>MetadataField</code> on an Item. (The Item can have many values of the
 * same field.) It contains element, qualifier, value and language. The field
 * (which names the schema, element, and qualifer), language, and a value.
 *
 * @author Martin Hald
 * @author James Rutherford
 * @see org.dspace.content.MetadataSchema, org.dspace.content.MetadataField
 */
@Entity
public class MetadataValue
{
    private static Logger log = Logger.getLogger(MetadataValue.class);

    private Context context;
//    private MetadataValueDAO dao;

    private int ID;

    /** The reference to the metadata field */
    private MetadataField metadataField;

    /** The reference to the DSpace item */
    //private int itemID;
    private Item item;

    /** The value of the field */
    private String value;

    /** The language of the field, may be <code>null</code> */
    private String language;

    /** The position of the record. */
    private int place;
    
    protected MetadataValue() {}
    
    public MetadataValue(Context context)
    {
        this.context = context;
//        this.ID = ID;

//        dao = MetadataValueDAOFactory.getInstance(context);
        place = 1;
    }

    /**
     * Constructor to create a value for a given field.
     *
     * @param metadataField inital value for field
     */
    public MetadataValue(MetadataField metadataField)
    {
        this.metadataField = metadataField;
    }

    /**
     * Get the field ID the metadata value represents.
     *
     * @return metadata field ID
     */
    @ManyToOne
    @JoinColumn(name="metadata_field_id")
    public MetadataField getMetadataField()
    {
        return metadataField;
    }

    /**
     * Set the field ID that the metadata value represents.
     *
     * @param fieldID new field ID
     */
    public void setFieldID(MetadataField metadataField)
    {
        this.metadataField = metadataField;
    }

    /**
     * Get the item ID.
     *
     * @return item ID
     * 
     * FIXME da deprecare
     */
/*    @Column(name="item_id")
    public int getItemID()
    {
        return itemID;
    }
*/
    /**
     * Set the item ID.
     *
     * @param itemID new item ID
     * 
     * FIXME da deprecare
     */
/*    public void setItemID(int itemID)
    {
        this.itemID = itemID;
    }
*/
    /**
     * Get the language (e.g. "en").
     *
     * @return language
     */
    @Column(name="text_lang")
    public String getLanguage()
    {
        return language;
    }

    /**
     * Set the language (e.g. "en").
     *
     * @param language new language
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Get the place ordering.
     *
     * @return place ordering
     */
    @Column(name="place")
    public int getPlace()
    {
        return place;
    }

    /**
     * Set the place ordering.
     *
     * @param place new place (relative order in series of values)
     */
    public void setPlace(int place)
    {
        this.place = place;
    }

    /**
     * Get the value ID.
     *
     * @return value ID
     */
    @Id
    public int getID()
    {
        return ID;
    }

    /**
     * Get the metadata value.
     *
     * @return metadata value
     */
    @Column(name="text_value")
    public String getValue()
    {
        return value;
    }

    /**
     * Set the metadata value
     *
     * @param value new metadata value
     */
    public void setValue(String value)
    {
        this.value = value;
    }
/*
    @Deprecated
    @Transient
    public static java.util.Collection findByField(Context context, int fieldID)
            throws AuthorizeException
    {
        MetadataValueDAO dao = MetadataValueDAOFactory.getInstance(context);
        return dao.getMetadataValues(fieldID);
    }
*/
//    @Deprecated
//    public void update(Context context) throws AuthorizeException
//    {
//        dao.update(this);
//    }
//
//    @Deprecated
//    public void delete(Context context) throws AuthorizeException
//    {
//        dao.delete(getID());
//    }
//
//    @Deprecated
//    public static MetadataValue find(Context context, int id)
//            throws AuthorizeException
//    {
//        MetadataValueDAO dao = MetadataValueDAOFactory.getInstance(context);
//        return dao.retrieve(id);
//    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    /**
     * FIXME: This assumes that we don't care about ID or placeID or itemID.
     *
     * @param other The DCValue object to compare with
     * @return Whether or not the two values are equal
     */
    
    public boolean equals(MetadataValue mdv)
    {
        MetadataFieldDAO mfDAO = MetadataFieldDAOFactory.getInstance(context);
        MetadataSchemaDAO msDAO = MetadataSchemaDAOFactory.getInstance(context);
        
        MetadataField mdvfield = mdv.getMetadataField();

        if ((mdvfield == null))
        {
            if (getMetadataField().getId() > 0)
            {
                return false;
            }
        }
        else
        {
            if (mdvfield.getId() != getMetadataField().getId())
            {
                return false;
            }
        }
        
        MetadataSchema mdvschema = mdvfield.getSchema();

        if (mdvschema == null)
        {
//            mdvschema = msDAO.retrieve(MetadataSchema.DC_SCHEMA_ID);
            mdvschema = ApplicationService.get(context, MetadataSchema.class, MetadataSchema.DC_SCHEMA_ID);
        }

        if (value == null)
        {
            if (mdv.getValue() != null)
            {
                return false;
            }
        }
        else
        {
            if (!value.equals(mdv.getValue()))
            {
                return false;
            }
        }

        if (language == null)
        {
            if (mdv.getLanguage() != null)
            {
                return false;
            }
        }
        else
        {
            if (!language.equals(mdv.getLanguage()))
            {
                return false;
            }
        }

        return true;
    }
 
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

	public void setID(int id) {
		ID = id;
	}
	@ManyToOne
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public void setMetadataField(MetadataField metadataField) {
		this.metadataField = metadataField;
	}
}
