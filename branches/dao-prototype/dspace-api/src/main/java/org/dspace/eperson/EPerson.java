/*
 * EPerson.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.EPersonDAO;           // Naughty!
import org.dspace.eperson.dao.EPersonDAOFactory;    // Naughty!
import org.dspace.history.HistoryManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing an e-person.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class EPerson extends DSpaceObject
{
    private static Logger log = Logger.getLogger(EPerson.class);

    private Context context;
    private EPersonDAO dao;

    private int id;
    private UUID uuid;
    
    /** See EPersonMetadataField. */
    private Map<EPersonMetadataField, String> metadata;

    private boolean selfRegistered;
    private boolean canLogin;
    private boolean requireCertificate;

    /** The e-mail field (for sorting) */
    public static final int EMAIL = 1;

    /** The last name (for sorting) */
    public static final int LASTNAME = 2;

    /** The e-mail field (for sorting) */
    public static final int ID = 3;

    /** The netid field (for sorting) */
    public static final int NETID = 4;

    /** The e-mail field (for sorting) */
    public static final int LANGUAGE = 5;

    public EPerson(Context context, int id)
    {
        this.id = id;
        this.dao = EPersonDAOFactory.getInstance(context);
        this.metadata = new HashMap<EPersonMetadataField, String>(8, 1);
    }

    public enum EPersonMetadataField
    {
        FIRSTNAME ("firstname"),
        LASTNAME ("lastname"),
        PASSWORD ("password"),
        EMAIL ("email"),
        PHONE ("phone"),
        NETID ("netid"),
        LANGUAGE ("language");

        private String name;

        private EPersonMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }

        public static EPersonMetadataField fromString(String name)
        {
            for (EPersonMetadataField f : values())
            {
                if (f.toString().equals(name))
                {
                    return f;
                }
            }

            throw new RuntimeException(":(");
        }
    }

//    EPerson(Context context, TableRow row)
    public EPerson(Context context, TableRow row)
    {
        this.context = context;

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_id"));
    }

    /**
     * Find the eperson by their email address
     *
     * @return EPerson
     */
    public static EPerson findByEmail(Context context, String email)
            throws SQLException, AuthorizeException
    {
        TableRow row = DatabaseManager.findByUnique(context, "eperson",
                "email", email);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the eperson by their netid
     *
     * @param context
     *            DSpace context
     * @param netid
     *            Network ID
     *
     * @return corresponding EPerson, or <code>null</code>
     */
    public static EPerson findByNetid(Context context, String netid)
            throws SQLException
    {
        if (netid == null)
            return null;

        TableRow row = DatabaseManager.findByUnique(context, "eperson",
                "netid", netid);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the epeople that match the search query across firstname, lastname or email
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }

    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     * This method also allows offsets and limits for pagination purposes.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query, int offset, int limit)
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
		String dbquery = "SELECT * FROM eperson WHERE eperson_id = ? OR " +
			"firstname ILIKE ? OR lastname ILIKE ? OR email ILIKE ? ORDER BY lastname, firstname ASC ";

		if (offset >= 0 && limit >0) {
			dbquery += "LIMIT " + limit + " OFFSET " + offset;
		}

        // When checking against the eperson-id, make sure the query can be
        // made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = new Integer(-1);
		}

		// Get all the epeople that match the query
		TableRowIterator rows =
            DatabaseManager.query(context, dbquery,
                    new Object[] {int_param, params, params, params});

		List epeopleRows = rows.toList();
		EPerson[] epeople = new EPerson[epeopleRows.size()];

		for (int i = 0; i < epeopleRows.size(); i++)
		{
		    TableRow row = (TableRow) epeopleRows.get(i);

		    // First check the cache
		    EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
		            .getIntColumn("eperson_id"));

		    if (fromCache != null)
		    {
		        epeople[i] = fromCache;
		    }
		    else
		    {
		        epeople[i] = new EPerson(context, row);
		    }
		}

		return epeople;
	}

     public String getLanguage()
     {
         return metadata.get(EPersonMetadataField.LANGUAGE);
     }

     public void setLanguage(String language)
     {
         if (language != null)
         {
             language = language.toLowerCase();
         }

         metadata.put(EPersonMetadataField.LANGUAGE, language);
     }

    public String getEmail()
    {
        return metadata.get(EPersonMetadataField.EMAIL);
    }

    public void setEmail(String email)
    {
        if (email != null)
        {
            email = email.toLowerCase();
        }

        metadata.put(EPersonMetadataField.EMAIL, email);
    }

    public String getNetid()
    {
        return metadata.get(EPersonMetadataField.NETID);
    }

    public void setNetid(String netid)
    {
        if (netid != null)
        {
            netid = netid.toLowerCase();
        }

        metadata.put(EPersonMetadataField.NETID, netid);
    }

    /**
     * Get the e-person's full name, combining first and last name in a
     * displayable string.
     *
     * @return their full name
     */
    public String getFullName()
    {
        String firstName = metadata.get(EPersonMetadataField.FIRSTNAME);
        String lastName = metadata.get(EPersonMetadataField.LASTNAME);

        if ((lastName == null) && (firstName == null))
        {
            return getEmail();
        }
        else if (firstName == null)
        {
            return lastName;
        }
        else
        {
            return (firstName + " " + lastName);
        }
    }

    public String getFirstName()
    {
        return metadata.get(EPersonMetadataField.FIRSTNAME);
    }

    public void setFirstName(String firstName)
    {
        metadata.put(EPersonMetadataField.FIRSTNAME, firstName);
    }

    public String getLastName()
    {
        return metadata.get(EPersonMetadataField.LASTNAME);
    }

    public void setLastName(String lastName)
    {
        metadata.put(EPersonMetadataField.LASTNAME, lastName);
    }

    public void setCanLogIn(boolean canLogin)
    {
        this.canLogin = canLogin;
    }

    public boolean canLogIn()
    {
        return canLogin;
    }

    public void setRequireCertificate(boolean requireCertificate)
    {
        this.requireCertificate = requireCertificate;
    }

    public boolean getRequireCertificate()
    {
        return requireCertificate;
    }

    public void setSelfRegistered(boolean selfRegistered)
    {
        this.selfRegistered = selfRegistered;
    }

    public boolean getSelfRegistered()
    {
        return selfRegistered;
    }

    public String getMetadata(EPersonMetadataField field)
    {
        return metadata.get(field);
    }

    public void setMetadata(EPersonMetadataField field, String value)
    {
        metadata.put(field, value);
    }

    @Deprecated
    public String getMetadata(String field)
    {
        return metadata.get(EPersonMetadataField.fromString(field));
    }

    @Deprecated
    public void setMetadata(String field, String value)
    {
        metadata.put(EPersonMetadataField.fromString(field), value);
    }

    public void setPassword(String password)
    {
        metadata.put(EPersonMetadataField.PASSWORD, Utils.getMD5(password));
    }

    public boolean checkPassword(String attempt)
    {
        String encoded = Utils.getMD5(attempt);

        return (encoded.equals(metadata.get(EPersonMetadataField.PASSWORD)));
    }

    /**
     * Update the EPerson
     */
    public void update() throws SQLException, AuthorizeException
    {
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.EPERSON;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public static EPerson[] findAll(Context context, int sortField)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.getEPeople(sortField);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    @Deprecated
    public static EPerson find(Context context, int id)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.retrieve(id);
    }

    @Deprecated
    public static EPerson create(Context context) throws AuthorizeException
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.create();
    }

    @Deprecated
    public void delete() throws AuthorizeException, EPersonDeletionException
    {
        dao.delete(getID());
    }
}
