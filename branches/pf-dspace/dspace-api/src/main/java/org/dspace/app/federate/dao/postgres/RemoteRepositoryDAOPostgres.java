/**
 * RemoteRepositoryDAOPostgres.java
 *
 * Version: $Id$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.federate.dao.postgres;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.app.federate.OAIRepository;
import org.dspace.app.federate.RemoteRepository;
import org.dspace.app.federate.dao.RemoteRepositoryDAO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * @author James Rutherford
 */
public class RemoteRepositoryDAOPostgres extends RemoteRepositoryDAO
{
	private Logger log = Logger.getLogger(RemoteRepositoryDAOPostgres.class);
	private Context context;

	public RemoteRepositoryDAOPostgres(Context context)
	{
		this.context = context;
	}

	/**
	 * Create a new RemoteRepository with some sensible default values.
	 */
	public RemoteRepository create()
	{
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "remoterepository");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("repository_id");
            RemoteRepository rr = new RemoteRepository();
            rr.setID(id);
            rr.setIdentifier(new ObjectIdentifier(uuid));

            return super.create(rr);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

		
	}

	/**
	 * Create a new RemoteRepository based on the given OAIRepository object.
	 */
	public RemoteRepository create(OAIRepository oair)
	{
		RemoteRepository rr = this.retrieve(oair.getBaseURL());
		if (rr == null)
		{
			Date date = new Date();
			rr = new RemoteRepository();
			rr.setName(oair.getName());
			rr.setBaseURL(oair.getBaseURL());
			rr.setAdminEmail(oair.getAdminEmail());
			rr.setActive(false);
			rr.setPublic(false);
			rr.setAlive(true);
			rr.setDateAdded(date);
			rr.setDateLastSeen(date);
			rr.setDateLastHarvested(new Date(0));
			rr.setMetadataFormats(oair.getMetadataFormats());
			rr.setFailedImports(new ArrayList<String>());
		}
		else
		{
			rr.update(oair);
		}

		return rr;
	}

	/**
	 * Get a specific RemoteRepository given an ID.
	 * 
	 * @param id The id of the repository.
	 */
	public RemoteRepository retrieve(int id)
	{
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "remoterepository", id);

            if (row == null)
            {
                log.warn("remote repository " + id + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
	}

    public RemoteRepository retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "remoterepository", "uuid", uuid);

            if (row == null)
            {
                log.warn("remote repository " + uuid + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
	 * Get a specific RemoteRepository given a URL corresponding to the
	 * OAI request URL of the repository.
	 * 
	 * @param url The OAI request URL.
	 */
	public RemoteRepository retrieve(URL url)
	{
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "remoterepository", "base_url", url.toString());

            if (row == null)
            {
                log.warn("remote repository " + url + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
	}

    private RemoteRepository retrieve(TableRow row)
    {
        RemoteRepository rr = new RemoteRepository();
        populateFromTableRow(rr, row);

        return rr;
    }

    /**
	 * Save all changes to a RemoteRepository back to the database.
	 */
	public void update(RemoteRepository rr)
	{
		// First, we perform the updates on the associated Community object (if
		// it exists AND the repository is set active).
		try
		{
            TableRow row = DatabaseManager.find(context, "remoterepository",
                    rr.getID());

            if (row != null)
            {
                update(rr, row);
            }
            else
            {
                throw new RuntimeException("Didn't find remote repository " +
                        rr.getID());
            }
        }
        catch (SQLException sqle)
		{
			// FIXME: Do something
			log.error("SQLException: ", sqle);
		}
    }

    private void update(RemoteRepository rr, TableRow row)
    {
        try
        {
            if (rr.isActive() && (rr.getCommunity() == null))
			{
				// If we're activating this repository for the first time, we
				// create the community into which we are going to harvest
				// items.
				createCommunity(rr);
				context.commit();
			}
			else if (rr.getCommunity() != null)
			{
                CommunityDAO communityDAO =
                        CommunityDAOFactory.getInstance(context);

                Community community = rr.getCommunity();
				community.setMetadata("name", rr.getName());

				communityDAO.update(community);
				context.commit();
			}
		}
		catch (AuthorizeException ae)
		{
			// FIXME: Do something
			log.error("AuthorizeException: ", ae);
		}
		catch (SQLException sqle)
		{
			// FIXME: Do something
			log.error("SQLException: ", sqle);
		}

		try
		{
			row.setColumn("distance", rr.getDistance());
			URL baseURL = rr.getBaseURL();
			if (baseURL != null)
			{
				row.setColumn("base_url", rr.getBaseURL().toString());
			}
			row.setColumn("name", rr.getName());
			row.setColumn("admin_email", rr.getAdminEmail());
			row.setColumn("is_active", rr.isActive());
			row.setColumn("is_alive", rr.isAlive());
			row.setColumn("is_public", rr.isPublic());
			row.setColumn("date_added", rr.getDateAdded());
			row.setColumn("date_last_harvested", rr.getDateLastHarvested());
			row.setColumn("date_last_seen", rr.getDateLastSeen());
			Community community = rr.getCommunity();
			if (community != null)
			{
				row.setColumn("community_id", community.getID());
			}

			List prefixes = rr.getMetadataFormats();
			if (prefixes != null)
			{
				// Something altogether different
			}

			DatabaseManager.update(context, row);
			context.commit();

			// Now process the failed imports list
			List<String> failedImports = rr.getFailedImports();
			if (failedImports != null)
			{
				row = null;
				if (baseURL != null)
				{
					rr = retrieve(baseURL);
					int id = rr.getID();
					// Delete the old bad record list
					DatabaseManager.deleteByValue(
							context, "failedimports", "repository_id", id + "");

					for (String failedImport : failedImports)
					{
						row = DatabaseManager.create(context, "failedimports");

						row.setColumn("repository_id", id);
						row.setColumn("identifier", failedImport);
						DatabaseManager.update(context, row);
					}
				}
			}
		}
		catch (SQLException sqle)
		{
			// FIXME: Do something
			log.error("SQLException: ", sqle);
		}
	}

	/**
	 * Delete a RemoteRepository from the database.
	 *
	 * @param id The database ID of the repository.
	 */
	public void delete(int id)
	{
		try
		{
			DatabaseManager.deleteByValue(
					context, "failedimports", "repository_id", id + "");
			DatabaseManager.delete(context, "remoterepository", id);
			context.commit();

		}
		catch (SQLException e)
		{
			log.error("A problem was encountered when trying to delete the"
					+ " remote repository with id " + id);
		}
	}

    /**
	 * Returns a List containing all RemoteRepositories.
	 */
	public List<RemoteRepository> getRemoteRepositories()
	{
		String sql = 
			"SELECT repository_id FROM remoterepository " +
			"ORDER BY repository_id";

		return getRemoteRepositories(sql);
	}

	/**
	 * Returns a List of all RemoteRepositories that are marked as 'active'.
	 */
	public List<RemoteRepository> getActiveRemoteRepositories()
	{
		String sql = 
			"SELECT repository_id FROM remoterepository " +
            "WHERE is_active = '1' " +
			"ORDER BY repository_id";

		return getRemoteRepositories(sql);
	}

	/**
	 * Returns a List of all RemoteRepositories that are marked as 'public'.
	 */
	public List<RemoteRepository> getPublicRemoteRepositories()
	{
		String sql = 
			"SELECT repository_id FROM remoterepository " +
            "WHERE is_public = '1' " +
			"ORDER BY repository_id";

		return getRemoteRepositories(sql);
	}

	/**
	 *
	 */
	private List<RemoteRepository> getRemoteRepositories(String sql)
	{
		List<RemoteRepository> list = new ArrayList<RemoteRepository>();

        try
		{
			TableRowIterator tri =
                    DatabaseManager.queryTable(context, "remoterepository", sql);

			for (TableRow row : tri.toList())
			{
                int id = row.getIntColumn("repository_id");
                list.add(retrieve(id));
			}
		}
		catch (SQLException sqle)
		{
			throw new RuntimeException(sqle);
		}

        return list;
	}

	////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////

	/**
	 * Utility function for creating the community that will be associated with
	 * this repository.
	 */
	private void createCommunity(RemoteRepository rr) throws AuthorizeException
	{
        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);
        Community root = communityDAO.retrieve(
                ConfigurationManager.getIntProperty("harvest.root"));

		Community community = root.createSubcommunity();
		community.setMetadata("name", rr.getName());
		communityDAO.update(community);

		rr.setCommunity(community);
	}
	
	/**
	 * 
	 */
	private void populateFromTableRow(RemoteRepository rr, TableRow row)
	{
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));

        rr.setID(row.getIntColumn("repository_id"));
        rr.setIdentifier(new ObjectIdentifier(uuid));

        if (!row.isColumnNull("distance"))
		{
			rr.setDistance(row.getIntColumn("distance"));
		}
		if (!row.isColumnNull("base_url"))
		{
			String baseURL = "";
			try
			{
				baseURL = row.getStringColumn("base_url");
				rr.setBaseURL(new URL(baseURL));
			}
			catch (MalformedURLException mue)
			{
				// FIXME: Do something
				log.error("MalformedURLException thrown while setting URL:" +
						baseURL, mue);
			}
		}
		if (!row.isColumnNull("name"))
		{
			rr.setName(row.getStringColumn("name"));
		}
		if (!row.isColumnNull("admin_email"))
		{
			rr.setAdminEmail(row.getStringColumn("admin_email"));
		}
		if (!row.isColumnNull("is_active"))
		{
			rr.setActive(row.getBooleanColumn("is_active"));
		}
		if (!row.isColumnNull("is_alive"))
		{
			rr.setAlive(row.getBooleanColumn("is_alive"));
		}
		if (!row.isColumnNull("is_public"))
		{
			rr.setPublic(row.getBooleanColumn("is_public"));
		}
		if (!row.isColumnNull("date_added"))
		{
			rr.setDateAdded(row.getDateColumn("date_added"));
		}
		if (!row.isColumnNull("date_last_harvested"))
		{
			rr.setDateLastHarvested(row.getDateColumn("date_last_harvested"));
		}
		if (!row.isColumnNull("date_last_seen"))
		{
			rr.setDateLastSeen(row.getDateColumn("date_last_seen"));
		}
		if (!row.isColumnNull("community_id"))
		{
            rr.setCommunity(Community.find(
                        context, row.getIntColumn("community_id")));
		}
        /*
        if (!row.isColumnNull("metadataformats"))
		{
			// Something else altogether

		}
		*/

		List<String> failedImports = new ArrayList<String>();
		TableRowIterator tri = null;
		try
		{
			tri = DatabaseManager.queryTable(context, "failedimports",
				"SELECT identifier FROM failedimports WHERE repository_id=" +
				rr.getID());

			for (TableRow r :tri.toList())
			{
				failedImports.add(r.getStringColumn("identifier"));
			}
			Collections.sort(failedImports);
		}
		catch (SQLException sqle)
		{
			throw new RuntimeException(sqle);
		}
		rr.setFailedImports(failedImports);
	}

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
}