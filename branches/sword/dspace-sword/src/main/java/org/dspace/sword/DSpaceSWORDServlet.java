package org.dspace.sword;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public class DSpaceSWORDServlet extends DSpaceServlet
{

	/* (non-Javadoc)
	 * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSGet(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException
	{
		this.doDSPost(context, request, response);
	}

	/* (non-Javadoc)
	 * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSPost(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException
	{
		DSpaceSWORDServer server = new DSpaceSWORDServer();
		server.setContext(context);
	}
	
}
