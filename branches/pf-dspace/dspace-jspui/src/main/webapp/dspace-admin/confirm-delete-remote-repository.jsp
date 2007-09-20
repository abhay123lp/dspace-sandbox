<%--
  - confirm-delete-remote-repository.jsp
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
	URL url = repo.getHarvestURL();
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
		titlekey="jsp.dspace-admin.confirm-delete-remote-repository.title"
		navbar="admin"
		locbar="link"
		parenttitlekey="jsp.administer"
		parentlink="/dspace-admin"
		nocache="true">

<style type="text/css">
	.remote_repository_form {
		witdh: 95%;
		margin: 20px;
	}
	.remote_repository_form_row {
		clear: both;
		float: none;
		margin: 5px;
		padding: 5px;
	}
	.remote_repository_label {
		float: left;
		font-weight: bold;
		text-align: right;
		width: 30%;
	}
	.remote_repository_value {
		padding-left: 5px;
		width: 70%;
	}
	#remote_repository_admin_buttons {
		margin: 20px;
	}
	#remote_repository_bad_records {
		margin: 20px;
	}
</style>

<h1><fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.title"/></h1>

<dspace:popup page="/help/site-admin.html"><fmt:message key="jsp.help"/></dspace:popup>

<form method="post">
	<input type="hidden" name="repository_id" value="<%= id %>" />

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.repository_id" />:
		</span>
		<span class="remote_repository_value">
			<%= id %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.name" />:
		</span>
		<span class="remote_repository_value">
			<%= name %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.destination_community" />:
		</span>
		<span class="remote_repository_value">
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
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.inactive" />
<%
		}
%>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.base_url" />:
		</span>
		<span class="remote_repository_value">
			<%= baseURL %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.admin_email" />:
		</span>
		<span class="remote_repository_value">
			<%= adminEmail %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.active" />:
		</span>
		<span class="remote_repository_value">
			<%= isActive %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.public" />:
		</span>
		<span class="remote_repository_value">
			<%= isPublic %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.distance" />:
		</span>
		<span class="remote_repository_value">
			<%= distance %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.date_added" />:
		</span>
		<span class="remote_repository_value">
			<%= dateAdded %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.date_last_seen" />:
		</span>
		<span class="remote_repository_value">
			<%= dateLastSeen %>
		</span>
	</div>

	<div class="remote_repository_form_row">
		<span class="remote_repository_label">
			<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.date_last_harvested" />:
		</span>
		<span class="remote_repository_value">
			<%= dateLastHarvested %>
		</span>
	</div>

	<div id="remote_repository_admin_buttons">
			<input type="submit" name="submit_cancel"
				value="<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.cancel"/>" />
			<input type="submit" name="submit_delete_repository_confirm"
				value="<fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.delete" />" />
	</div>
</form>
<a href="<%= request.getContextPath() %>/dspace-admin/remote-repositories"
	><fmt:message key="jsp.dspace-admin.confirm-delete-remote-repository.go_back" /></a>
</dspace:layout>


