<%--
  - remote-repository-details.jsp
  -
  - Version: $Revision: 1.9 $
  -
  - Date: $Date: 2006/07/30 02:56:38 $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>


<%--
  - Display the full information for a remote repository, complete with a form
  - for editing the details.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.app.federate.RemoteRepository" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%
	RemoteRepository repo = (RemoteRepository) request.getAttribute("remote_repository");
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

	int id = repo.getID();
	Community community = repo.getCommunity();
	String name = repo.getName();
	URL url = repo.getBaseURL();
	String baseURL = "";
	if (url != null)
	{
		baseURL = url.toString();
	}
	boolean isActive = repo.isActive();
	boolean isPublic = repo.isPublic();
	int distance = repo.getDistance();
	String dateAdded = dateFormat.format(repo.getDateAdded());
	String dateLastHarvested = dateFormat.format(repo.getDateLastHarvested());
	String dateLastSeen = dateFormat.format(repo.getDateLastSeen());
	String adminEmail = repo.getAdminEmail();

	List failedImports = repo.getFailedImports();
%>

<dspace:layout
		titlekey="jsp.dspace-admin.remote-repository-details.title"
		navbar="admin"
		locbar="link"
		parenttitlekey="jsp.administer"
		parentlink="/dspace-admin"
		nocache="true">

<style type="text/css">
	#header {
		clear: both;
		float: none;
		width: 95%;
	}
	#header h1 {
		clear: none;
		float: left;
	}
	#header a {
		float: right;
	}
	.remote_repository_form {
		clear: both;
		float: none;
		margin: 20px;
		witdh: 95%;
	}
	.remote_repository_form_row {
		clear: both;
		float: none;
		margin: 5px;
		padding: 5px;
	}
	.remote_repository_form_label {
		float: left;
		font-weight: bold;
		text-align: right;
		width: 20%;
		min-width: 150px;
	}
	.remote_repository_form_input {
		padding-left: 5px;
		width: 80%;
	}
	#remote_repository_admin_buttons {
		margin: 20px;
	}
	#remote_repository_bad_records {
		margin: 20px;
	}
</style>

<div id="header">
	<h1><fmt:message key="jsp.dspace-admin.remote-repository-details.title"/></h1>
	<dspace:popup page="/help/site-admin.html"><fmt:message key="jsp.help"/></dspace:popup>
</div>

<form method="post">
	<input type="hidden" name="repository_id" value="<%= id %>" />

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.repository_id" />:
		</span>
		<span class="remote_repository_form_input">
			<%= id %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.name" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="text" size="30" name="name"
				value="<%= name %>" />
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.destination_community" />:
		</span>
		<span class="remote_repository_form_input">
<%
		if (community != null)
		{
%>
			<a href="<%= community.getIdentifier().getURL().toString() %>"
				><%= community.getMetadata("name") %></a>
<%
		}
		else
		{
%>
			<fmt:message key="jsp.dspace-admin.remote-repository-details.inactive" />
<%
		}
%>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.base_url" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="text" size="30" name="base_url"
				value="<%= baseURL %>" />
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.admin_email" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="text" size="30" name="admin_email"
				value="<%= adminEmail %>" />
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.active" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="checkbox" name="is_active"
				<%= isActive ? "checked=\"checked\"" : "" %> />
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.public" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="checkbox" name="is_public"
				<%= isPublic ? "checked=\"checked\"" : "" %> />
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.distance" />:
		</span>
		<span class="remote_repository_form_input">
			<%= distance %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.date_added" />:
		</span>
		<span class="remote_repository_form_input">
			<%= dateAdded %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.date_last_seen" />:
		</span>
		<span class="remote_repository_form_input">
			<%= dateLastSeen %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repository-details.date_last_harvested" />:
		</span>
		<span class="remote_repository_form_input">
			<input type="text" size="30" name="date_last_harvested"
				value="<%= dateLastHarvested %>" />
		</span>
	</div>

	<div id="remote_repository_admin_buttons">
			<input type="submit" name="submit_update_repository"
				value="<fmt:message key="jsp.dspace-admin.remote-repository-details.save"/>" />
			<input type="submit" name="submit_delete_repository"
				value="<fmt:message key="jsp.dspace-admin.remote-repository-details.delete" />" />
	</div>

	<div id="remote_repository_bad_records">
		<h1><fmt:message key="jsp.dspace-admin.remote-repository-details.bad_records_title"/></h1>
		<table class="miscTable">
			<tr>
				<th class="oddRowOddCol">
					<strong>
						<fmt:message key="jsp.dspace-admin.remote-repository-details.bad_records_internal_id" />
					</strong>
				</th>
				<th class="oddRowEvenCol">
					<strong>
						<fmt:message key="jsp.dspace-admin.remote-repository-details.bad_records_value" />
					</strong>
				</th>
			</tr>
<%
	String row = "";
	for (int i = 0; i < failedImports.size(); i++)
	{
		row = (i % 2 == 0 ? "even" : "odd");
		String r = (String)failedImports.get(i);
%>
			<tr>
				<td class="<%= row %>RowOddCol"><%= i + 1 %></td>
				<td class="<%= row %>RowEvenCol"><%= r %></td>
			</tr>
<%
	}
%>
		</table>
	</div>
</form>
<a href="<%= request.getContextPath() %>/dspace-admin/remote-repositories"
	><fmt:message key="jsp.dspace-admin.remote-repository-details.go_back" /></a>
</dspace:layout>

