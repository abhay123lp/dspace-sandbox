<%--
  - remote-repositories.jsp
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
  - Display list of remote repositories
  -
  - Attributes:
  -
  -	remote_repositories - List of RemoteRepository objects
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
	prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.federate.RemoteRepository" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%@ page import="org.apache.log4j.Logger" %>

<%
	List<RemoteRepository> repos =
		(List) request.getAttribute("remote_repositories");
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	// FIXME: This throws a NullPointerException if the Community doesn't exist
	Community harvestRoot = (Community) request.getAttribute("harvest_root");
%>

<dspace:layout
		titlekey="jsp.dspace-admin.remote-repositories.title"
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
	#settings {
		clear: both;
		float: none;
		margin-top: 20px;
		width: 95%;
	}
	#remote_repository_list th {
		white-space: nowrap;
	}
	#remote_repository_list td {
		font-size: 70%;
	}
	.remote_repository_form {
		clear: both;
		float: none;
		witdh: 95%;
		margin: 20px;
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
</style>

<div id="header">
	<h1><fmt:message key="jsp.dspace-admin.remote-repositories.title"/></h1>
	<dspace:popup page="/help/site-admin.html"><fmt:message key="jsp.help"/></dspace:popup>
</div>

<div class="remote_repository_form">
<form method="post">
<table id="remote_repository_list" class="miscTable">
	<tr>
		<th class="oddRowOddCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.repository_id"/>
			</strong>
		</th>
		<th class="oddRowEvenCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.name"/>
			</strong>
		</th>
		<th class="oddRowOddCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.admin_email"/>
			</strong>
		</th>
		<th class="oddRowEvenCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.active"/>
			</strong>
		</th>
		<th class="oddRowOddCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.public"/>
			</strong>
		</th>
		<th class="oddRowEvenCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.distance"/>
			</strong>
		</th>
		<th class="oddRowOddCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.date_added"/>
			</strong>
		</th>
		<th class="oddRowEvenCol">
			<strong>
				<fmt:message key="jsp.dspace-admin.remote-repositories.date_last_harvested"/>
			</strong>
		</th>
		<th class="oddRowOddCol">&nbsp;</th>
		<th class="oddRowEvenCol">&nbsp;</th>
	</tr>
<%
	String row = "";
	int i = 0;
	for (RemoteRepository r : repos)
	{
		row = (i++ % 2 == 0 ? "even" : "odd");
%>
	<tr>
		<input type="hidden" name="repository_ids[]" value="<%= r.getID() %>" />
		<td class="<%= row %>RowOddCol" align="center">
			<%= r.getID() %>
		</td>
		<td class="<%= row %>RowEvenCol">
			<%= r.getName() %>
		</td>
		<td class="<%= row %>RowOddCol">
			<a href="mailto:<%= r.getAdminEmail() %>"><%= r.getAdminEmail() %></a>
		</td>
		<td class="<%= row %>RowEvenCol" align="center">
			<input type="checkbox" name="is_active_<%= r.getID() %>"
				<%= r.isActive() ? " checked=\"checked\"" : "" %> />
		</td>
		<td class="<%= row %>RowOddCol" align="center">
			<input type="checkbox" name="is_public_<%= r.getID() %>"
				<%= r.isPublic() ? " checked=\"checked\"" : "" %> />
		</td>
		<td class="<%= row %>RowEvenCol" align="center">
			<%= r.getDistance() %>
		</td>
		<td class="<%= row %>RowOddCol">
			<%
				Date dateAdded = r.getDateAdded(); 
			%>
			<%= dateFormat.format(dateAdded) %>
		</td>
		<td class="<%= row %>RowEvenCol">
			<%
				Date dateHarvested = r.getDateLastHarvested(); 
			%>
			<%= dateFormat.format(dateHarvested) %>
		</td>
		<td class="<%= row %>RowEvenCol">
			<input type="submit" name="submit_harvest_<%= r.getID() %>"
				value="<fmt:message key="jsp.dspace-admin.remote-repositories.harvest_single" />" />
			
		</td>
		<td class="<%= row %>RowOddCol">
			<input type="submit" name="submit_show_details_<%= r.getID() %>"
				value="<fmt:message key="jsp.dspace-admin.remote-repositories.show_details" />" />
			
		</td>
	</tr>
<%
	}
%>
</table>

<div id="remote_repository_admin_buttons">
		<input type="submit" name="submit_update_status"
			value="<fmt:message key="jsp.dspace-admin.remote-repositories.save"/>" />
		<input type="submit" name="submit_harvest_active"
			value="<fmt:message key="jsp.dspace-admin.remote-repositories.harvest_active" />" />
		<input type="submit" name="submit_add_repository"
			value="<fmt:message key="jsp.dspace-admin.remote-repositories.add_remote_repository" />" />
		<input type="submit" name="submit_update_list"
			value="<fmt:message key="jsp.dspace-admin.remote-repositories.update_list"/>" />
</div>
</form>
</div>

<form method="post">
<div id="settings">
	<h1><fmt:message key="jsp.dspace-admin.remote-repositories.settings_title"/></h1>
	<div class="remote_repository_form_row">
		<span class="remote_repository_form_label">
			<fmt:message key="jsp.dspace-admin.remote-repositories.root_community" />:
		</span>
		<span class="remote_repository_form_input">
			<a href="<%= harvestRoot.getIdentifier().getURL().toString() %>"
				><%= harvestRoot.getMetadata("name") %></a>
		</span>
	</div>
</div>
</form>

</dspace:layout>
