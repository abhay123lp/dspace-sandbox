/*
 * RemoteRepositoryAdminServlet.java
 *
 * Version: $Revision: 1.15 $
 *
 * Date: $Date: 2006/08/05 01:16:42 $
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
package org.dspace.app.webui.servlet.admin;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.federate.HarvestThread;
import org.dspace.app.federate.RemoteRepository;
import org.dspace.app.federate.dao.RemoteRepositoryDAO;
import org.dspace.app.federate.dao.postgres.RemoteRepositoryDAOPostgres;
import org.dspace.app.federate.OAIRepository;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;

/**
 * Servlet for dealing with remote repositories
 *
 * @author Talent
 * @author James Rutherford
 * @version $Revision: 1.15 $
 */
public class RemoteRepositoryAdminServlet extends DSpaceServlet
{
	/** log4j logger */
	private static Logger log = Logger.getLogger(RemoteRepositoryAdminServlet.class);

	private static PipedInputStream in;

	private static PipedOutputStream out;

	private RemoteRepositoryDAO dao;

	protected void doDSGet(Context c, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException
	{
		// Display a list of all the remote repositories
		dao = new RemoteRepositoryDAOPostgres(c);
		showRemoteRepositories(c, request, response);
	}

	protected void doDSPost(Context c, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException
	{
		dao = new RemoteRepositoryDAOPostgres(c);

		String button = UIUtil.getSubmitButton(request, "none");
		if (button.equals("submit_update_status"))
		{
			// Update active/public information
			String ids[] = request.getParameterValues("repository_ids[]");
			int id = -1;

			for (int i = 0; i < ids.length; i++)
			{
				id = Integer.parseInt(ids[i]);
				RemoteRepository rr = dao.retrieve(id);
				processRepositoryUpdate(request, button, rr);
			}
			
			showRemoteRepositories(c, request, response);

			c.complete();
		}
		else if (button.equals("submit_add_repository"))
		{
			log.info("adding new remote repository");
			// Add new remote repository
			RemoteRepository rr = dao.create();
			rr.setDistance(0);
			dao.update(rr);
            request.setAttribute("remote_repository", rr);

			showRepositoryAdminPage(c, request, response);

			c.complete();
		}
		else if (button.equals("submit_update_repository"))
		{
			// Update remote repository
			int id = Integer.parseInt(request.getParameter("repository_id"));
			RemoteRepository rr = dao.retrieve(id);
			processRepositoryUpdate(request, button, rr);

			log.info("updating remote repository with id = " + id);

			request.setAttribute("remote_repository", rr);
			request.setAttribute("remote_repository_dao", dao);
			showRepositoryAdminPage(c, request, response);

			c.complete();
		}
		else if (button.equals("submit_delete_repository"))
		{
			// Delete remote repository
			int id = Integer.parseInt(request.getParameter("repository_id"));
			RemoteRepository rr = dao.retrieve(id);

			request.setAttribute("remote_repository", rr);

			showRemovalConfirmation(c, request, response);

			c.complete();

		}
		else if (button.equals("submit_delete_repository_confirm"))
		{
			// Delete remote repository
			int id = Integer.parseInt(request.getParameter("repository_id"));

			log.info("removing remote repository with id = " + id);

			if (id > 0)
			{
				dao.delete(id);
			}
			else
			{
				log.info("attempted to delete non-existent repository " + id);
			}

			showRemoteRepositories(c, request, response);

			c.complete();

		}
		else if (button.startsWith("submit_show_details_"))
		{
			// Show details of remote repository
			int id = Integer.parseInt(button.substring("submit_show_details_".length()));
			RemoteRepository rr = dao.retrieve(id);

			request.setAttribute("remote_repository", rr);

			showRepositoryAdminPage(c, request, response);

			c.complete();

		}
		else if (button.equals("submit_harvest_active"))
		{
			// TODO: Why is this necessary?
			holdUpConsoleOutput();

			List<RemoteRepository> repositories = dao.getActiveRemoteRepositories();
			int ids[] = new int[repositories.size()], x = 0;

			for (RemoteRepository rr : repositories)
			{
				ids[x] = rr.getID();
			}

			HarvestThread hb = new HarvestThread(c.getCurrentUser());
			hb.setRepositories(ids);
			hb.setDate(new Date());
			hb.start();

			request.getSession().setAttribute("harvest.thread", hb);
			JSPManager.showJSP(request, response, "/dspace-admin/harvest.jsp");
			c.complete();
		}
		else if (button.startsWith("submit_harvest_"))
		{
			int[] id = { Integer.parseInt(button.substring("submit_harvest_".length())) };
			
			// TODO: Why is this necessary?
			holdUpConsoleOutput();

			HarvestThread hb = new HarvestThread(c.getCurrentUser());
			hb.setRepositories(id);
			hb.setDate(new Date());
			hb.start();

			request.getSession().setAttribute("harvest.thread", hb);
			JSPManager.showJSP(request, response, "/dspace-admin/harvest.jsp");
			c.complete();
		}
		else if (button.equals("submit_stop_harvest"))
		{
			HarvestThread ht = (HarvestThread)request.getSession().getAttribute("harvest.thread");
			if (ht != null)
			{
				ht.stopHarvest();
			}

			showRemoteRepositories(c, request, response);
			c.complete();
		}
		else if (button.equals("submit_update_list"))
		{
			processListUpdate(c);
			showRemoteRepositories(c, request, response);
			c.complete();
		}
		else
		{
			showRemoteRepositories(c, request, response);
			c.complete();
		}
	}

	/**
	 * Show the full info page for a repository. This is also the page used for
	 * editing and deleting repositories.
	 *
	 * @param c
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	private void showRepositoryAdminPage(Context c, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException
	{
		JSPManager.showJSP(request, response,
				"/dspace-admin/remote-repository-details.jsp");
	}

	/**
	 * Show the "confirm deletion" page.
	 *
	 * @param c
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	private void showRemovalConfirmation(Context c, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException
	{
		JSPManager.showJSP(request, response,
				"/dspace-admin/confirm-delete-remote-repository.jsp");
	}

	/**
	 * Show all remote repositories.
	 *
	 * @todo Deal gracefully with the situation where the root community has
	 * not been created and the user visits the `list' page. Just a message
	 * saying that it hasn't yet been done should suffice.
	 *
	 * @param c
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	private void showRemoteRepositories(Context c, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException
	{
		List<RemoteRepository> repos = dao.getRemoteRepositories();
		int id = ConfigurationManager.getIntProperty("harvest.root");
		Community root = Community.find(c, id);

		request.setAttribute("remote_repositories", repos);
		request.setAttribute("harvest_root", root);

		JSPManager.showJSP(request, response,
				"/dspace-admin/remote-repositories.jsp");
	}

	private void holdUpConsoleOutput() throws IOException
	{
		in = new PipedInputStream();
		out = new PipedOutputStream();

		out.connect(in);

		PrintStream  ps = new PrintStream(out);

		System.setOut(ps);
		System.setErr(ps);
	}

	/**
	 * Given a remote repository, this function will update it according to
	 * parameters passed in the form.
	 *
	 * @param request
	 * @param rr The RemoteRepository object to update
	 */
	private void processRepositoryUpdate(HttpServletRequest request,
			String button, RemoteRepository rr)
		throws ServletException, SQLException, AuthorizeException, IOException
	{
		// The "active" and "public" properties require slightly special
		// treatment, since they are modifiable from the repository list, as
		// well as from the individual repository administration pages.
		String suffix = "";
		if (button.equals("submit_update_status"))
		{
			suffix = "_" + rr.getID();
		}
		rr.setActive((request.getParameter("is_active" + suffix) != null)
				&& request.getParameter("is_active" + suffix).equals("on"));
		rr.setPublic((request.getParameter("is_public" + suffix) != null)
				&& request.getParameter("is_public" + suffix).equals("on"));

		if (request.getParameter("name") != null)
		{
			rr.setName(request.getParameter("name"));
		}
		if (request.getParameter("base_url") != null)
		{
			try
			{
				rr.setBaseURL(new URL(request.getParameter("base_url")));
			}
			catch (MalformedURLException mue) { }
		}
		if (request.getParameter("admin_email") != null)
		{
			rr.setAdminEmail(request.getParameter("admin_email"));
		}

		if (request.getParameter("date_last_harvested") != null)
		{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try
			{
				rr.setDateLastHarvested(format.parse(request.getParameter("date_last_harvested")));
			}
			catch (Exception e)
			{
				throw new ServletException(e);
			}
		}

		dao.update(rr);
	}

	/**
	 * Update our list of remote repositories by asking each of the
	 * repositories in our current list what repositories they know about.
	 */
	private void processListUpdate(Context c) throws ServletException
	{
		try
		{
			List<RemoteRepository> remoteRepositories = dao.getRemoteRepositories();
			int repoCount = remoteRepositories.size();

			for (RemoteRepository rr : remoteRepositories)
			{
				OAIRepository oair = null;
				try
				{
					oair = new OAIRepository(rr.getBaseURL());
					rr.update(oair);
					dao.update(rr);
				}
				catch (IOException ioe)
				{
					rr.setAlive(false);
					continue;
				}
				catch (JDOMException jdome)
				{
					rr.setAlive(false);
					continue;
				}

				try
				{
					List<URL> friends = oair.getFriends();
					int distance = rr.getDistance() + 1;

					log.info("Asking " + oair.getBaseURL() + " for friends list");
					processOAIFriendsList(c, friends, distance);
				}
				catch (Exception e) { }
			}

			remoteRepositories = dao.getRemoteRepositories();
			if (repoCount < remoteRepositories.size())
			{
				processListUpdate(c);
			}
		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}

	/**
	 * Iterate through the list of OAI baseURLs and take the appropriate
	 * action, depending on what we find.
	 */
	private void processOAIFriendsList(Context c, List<URL> friends, int distance)
		throws SQLException, Exception
	{
		// The friends list is just a List of URLs (Strings). Currently, the
		// only means we have to distinguish between remote repositories is
		// their harvest URLs. Thus, we can deal with any changes in repository
		// information, but a URL change will require manual intervention.
		for (URL harvestURL : friends)
		{
			URL ourURL =
				new URL(ConfigurationManager.getProperty("dspace.url.oai"));
			RemoteRepository candidate = dao.retrieve(harvestURL);
			OAIRepository oaiFriend = null;

			try
			{
				oaiFriend = new OAIRepository(harvestURL);

				if (candidate != null)
				{
					log.info("found an existing friend: " +
							harvestURL.toString());

					// The candidate repository exists in our local database,
					// so we run through and make sure we have up-to-date
					// information. The only thing we don't update is the
					// distance, since that is only set when we discover the
					// repository (it makes no sense to change it later).
					candidate.update(oaiFriend);
					dao.update(candidate);
				}
				else if (!harvestURL.equals(ourURL))
				{
					log.info("found a new friend: " + harvestURL.toString());

					// If the repository isn't in our local database, and
					// doesn't represent "us", then we consider it a new
					// friend, and add it to our local list.
					RemoteRepository rr =
						dao.create(oaiFriend);

					// The distance metric we apply to remote repositories
					// increments for each level of recursion. This isn't an
					// altogether reliable mechanism, since we may discover the
					// same repository at multiple "distances" depending on who
					// it is that exposes it to us first.
					rr.setDistance(distance);

					dao.update(rr);
				}
				else
				{
					log.info("found self in friends list.");
				}
			}
			catch (Exception e) {}
		}
	}
}
