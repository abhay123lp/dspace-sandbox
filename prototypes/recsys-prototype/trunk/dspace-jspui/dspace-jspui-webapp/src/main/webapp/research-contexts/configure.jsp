<%--
- configure.jsp
-
- Version: $Revision: 235 $
-
- Date: $Date: 2007-07-16 14:42:31 +0100 (Mon, 16 Jul 2007) $
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
  - Allow a user to configure a Research Context to suit their needs
  --%>

<%@ page import="org.dspace.app.recsys.researchContext.ResearchContext" %>
<%@ page import="org.dspace.app.webui.servlet.ResearchContextServlet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.dspace.app.recsys.researchContext.Essence" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.UUID" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<style type="text/css">
    div.main
    {
        border: 2px solid #C3D9FF;
        margin: 0px;
        margin-left: 10px;
        width: 75%;
        float: left;
    }
    div.side
    {
        border-style: none;
        width: 15%;
        float: left;
        margin: 0px 0px 0px -40px;
    }
    table.form
    {
        empty-cells: show;
        margin-left: 10px;
    }
    td.form
    {
        font-size: 12pt;
    }
    ul
    {
        list-style-type: none;
    }
    li.selected
    {
        font-size: 12pt;
        border: 2px solid #C3D9FF;
        border-left: 2px solid #FFFFFF;
        margin-bottom: 4px;
        margin-left: -2px;
        padding: 4px;
    }
    li.unselected {
        font-size: 12pt;
        padding: 4px;
        margin-bottom: 4px;
    }
    p
    {
        width: 50%;
    }
</style>

<%
    ResearchContext rc = (ResearchContext)request.getAttribute("rC");
%>

<dspace:layout locbar="link"
               parentlink="/research-contexts"
               parenttitlekey="jsp.quambo.research-contexts.title"
               titlekey="jsp.quambo.research-contexts.configure.title">

    <table width="100%" border="0">
        <tr>
            <td align="left">
                <h1>Configure '<%= rc.getName()%>'</h1>
            </td>
        </tr>
    </table>
    <div class="main">
        <h2>Change Settings</h2>
        <form method="post" action="<%= request.getContextPath()%>/research-contexts">
            <table class="form">
                <tr>
                    <td class="form">
                        Name:
                    </td>
                    <td align="left">
                        <input type="text" name="name" value="<%= rc.getName() %>"/>
                    </td>
                </tr>
                <tr><td></td></tr>
                <tr>
                    <td colspan="2" align="center">
                        <input type="submit" name="submit_configure" value="Save" />
                        <input type="submit" name="submit_cancel" value="Cancel" />
                        <input type="hidden" name="step" value="<%= ResearchContextServlet.CONFIGURE_RESEARCH_CONTEXT %>" />
                        <input type="hidden" name="id" value="<%= rc.getID() %>">
                    </td>
                </tr>
            </table>
        </form>
        <table style="width: 100%; margin-top: 10px;">
            <tr>
                <td style="height: 2px;" bgcolor="#C3D9FF">
                </td>
            </tr>
        </table>
        <h2>Manage Essence(s)</h2>
        <p>
            You can add or remove essence feeds from this Research Context. Note
            that you cannot remove the local essence feed because this models
            your bookmarking and unbookmarking interactions.
        </p>
        <form method="post" action="<%= request.getContextPath() %>/research-contexts">
            <table class="miscTable" style="margin-left: 10px;">
                <tr>
                    <th class="oddRowOddCol">Name</th>
                    <th class="oddRowEvenCol">Address</th>
                    <th class="oddRowOddCol">Weight</th>
                    <th class="oddRowEvenCol">Delete</th>
                    <th class="oddRowOddCol"></th>
                </tr>
<%
                Set<Essence> essences = (Set<Essence>)request.getAttribute("essences");
                String row = "even";
                for (Essence e: essences)
                {
%>
                    <tr>
                        <td class="<%= row %>RowOddCol"><%= e.getName() %></td>
                        <td class="<%= row %>RowEvenCol"><%= e.getUri() %></td>
                        <td class="<%= row %>RowOddCol" align="center"><%= e.getWeight() %></td>
                        <td class="<%= row %>RowEvenCol" align="center"><input type="checkbox" /></td>
<%
                        if (e.isLocalEssence())
                        {
%>                          <td class="<%= row%>RowOddCol">
                                Cannot be deleted
                            </td>
<%
                        }
                        else
                        {
%>
                            <td class="<%= row %>RowOddCol">
                                <input type="submit" name="submit_delete_essence" value="Delete" />
                                <input type="hidden" name="step" value="<%= ResearchContextServlet.MANAGE_ESSENCE %>" />
                                <input type="hidden" name="delete_essence" value="<%= e.getID()%>" />
                                <input type="hidden" name="id" value="<%= rc.getID() %>">
                            </td>
<%
                        }
%>
                    </tr>
<%
                    row = (row.equals("even") ? "odd" : "even" );
                }
%>
            </table>
            </form>
        <h3>Add New Essence Feed</h3>
        <p>
            You can add a new essence feed source to this Research Context.
            Adding a new essence feed source lets you blend the essence of many
            Research Contexts with each other. Who knows what might happen?
        </p>
        <form method="post" action="<%= request.getContextPath() %>/research-contexts">
            <table style="margin-left: 10px;">
                <tr>
                    <td>Essence URI:</td>
                    <td>
                        <input type="text" name="essence_uri" />
                    </td>
                </tr>
                <tr>
                    <td>Name: </td>
                    <td>
                        <input type="text" name="essence_name" />
                    </td>
                    <td>
                        <input type="submit" name="submit_add_essence" value="Add" />
                        <input type="hidden" name="step" value="<%= ResearchContextServlet.MANAGE_ESSENCE %>" />
                        <input type="hidden" name="id" value="<%= rc.getID() %>">
                    </td>
                </tr>
            </table>
        </form>
        <table style="width: 100%; margin-top: 10px;">
            <tr>
                <td style="height: 2px;" bgcolor="#C3D9FF">
                </td>
            </tr>
        </table>
        <h2>Delete Research Context</h2>
<%
        if (rc.isInitialResearchContext())
        {
%>
            <p>
                You cannot delete this Research Context because it is required by the system.
            </p>
<%
        }
        else
        {
%>
            <p>
                If you delete a Research Context, all bookmarked items, recommended
                items, and essences will be deleted too. This may affect users who
                are using your essence feed in their Research Context.
            </p>
            <p>
                <em>You cannot undo this action.</em>
            </p>
            <form method="post" action="<%= request.getContextPath() %>/research-contexts">
                <table class="form">
                    <tr>
                        <td class="form">
                            Delete:
                        </td>
                        <td>
                            <input type="checkbox" name="delete" value="delete" />
                        </td>
                        <td>
                            <input type="submit" name="submit_configure" value="Confirm" />
                            <input type="hidden" name="step" value="<%= ResearchContextServlet.CONFIGURE_RESEARCH_CONTEXT %>" />
                            <input type="hidden" name="id" value="<%= rc.getID() %>">
                        </td>
                    </tr>
                </table>
            </form>
<%
        }
%>
    </div>
    <div class="side">
        <ul>
            <li class="unselected">
                <a href="<%= request.getContextPath()%>/research-contexts/create">New Research Context</a>
            </li>
<%
            List<ResearchContext> rCs =
              (List<ResearchContext>) request.getAttribute("researchContexts");

            Iterator<ResearchContext> iterator = rCs.iterator();

            UUID uuid = UUID.fromString(request.getAttribute("uuid").toString());
            while (iterator.hasNext())
            {
                ResearchContext r = iterator.next();
                String uri = request.getContextPath() +
                             "/research-contexts?uuid=" +
                             r.getUUID();

                if (r.getUUID().equals(uuid))
                {
                    String configureURI = request.getContextPath() +
                             "/research-contexts/configure" +
                             "?uuid=" + r.getUUID();
%>
                    <li class="selected">
                      <a href="<%=uri%>"><%= r.getName() %></a>
                      <br />
                      <a style="font-size: 10pt;" href="<%= configureURI %>">Configure</a>
                    </li>
<%              }
                else
                {
%>
                    <li class="unselected">
                      <a href="<%= uri %>"><%= r.getName() %></a>
                    </li>
<%
                }
            }
%>
        </ul>
    </div>
</dspace:layout>