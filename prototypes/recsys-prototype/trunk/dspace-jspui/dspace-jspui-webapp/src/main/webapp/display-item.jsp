<%--
  - display-item.jsp
  -
  - Version: $Revision: 2966 $
  -
  - Date: $Date: 2008-06-24 22:02:04 +0100 (Tue, 24 Jun 2008) $
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
  - Renders a whole HTML page for displaying item metadata.  Simply includes
  - the relevant item display component in a standard HTML page.
  -
  - Attributes:
  -    display.all - Boolean - if true, display full metadata record
  -    item        - the Item to display
  -    collections - Array of Collections this item appears in.  This must be
  -                  passed in for two reasons: 1) item.getCollections() could
  -                  fail, and we're already committed to JSP display, and
  -                  2) the item might be in the process of being submitted and
  -                  a mapping between the item and collection might not
  -                  appear yet.  If this is omitted, the item display won't
  -                  display any collections.
  -    admin_button - Boolean, show admin 'edit' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.license.CreativeCommons" %>
<%@ page import="org.dspace.uri.ExternalIdentifier" %>
<%@ page import="org.dspace.uri.IdentifierService" %>
<%@ page import="org.dspace.uri.ObjectIdentifier" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.recsys.researchContext.ResearchContext" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.dspace.app.recsys.recommendation.Item2ItemRecommendation" %>

<%
    // Attributes
    Boolean displayAllBoolean = (Boolean) request.getAttribute("display.all");
    boolean displayAll = (displayAllBoolean != null && displayAllBoolean.booleanValue());
    Boolean suggest = (Boolean)request.getAttribute("suggest.enable");
    boolean suggestLink = (suggest == null ? false : suggest.booleanValue());
    Item item = (Item) request.getAttribute("item");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    // get the workspace id if one has been passed
    Integer workspace_id = (Integer) request.getAttribute("workspace_id");

    // get the persistent identifier if the item has one yet
    // ExternalIdentifier identifier = item.getExternalIdentifier();
    List<ExternalIdentifier> eids = item.getExternalIdentifiers();
    ObjectIdentifier oid = item.getIdentifier();
    String uri = "";
    String citationLink = "";
    String link = IdentifierService.getURL(item).toString();

    // CC URL & RDF
    String cc_url = CreativeCommons.getLicenseURL(item);
    String cc_rdf = CreativeCommons.getLicenseRDF(item);

    // Full title needs to be put into a string to use as tag argument
    String cf = IdentifierService.getCanonicalForm(item);
    String title = "FIXME";
    DCValue[] titleValue = item.getMetadata("dc", "title", null, Item.ANY);
    if (titleValue.length != 0)
    {
        title = titleValue[0].value + " (" + cf + ")";
    }
    else
    {
        title = "Item " + cf;
    }
%>

<dspace:layout title="<%= title %>">

<%
    String displayStyle = (displayAll ? "full" : "");
%>
    <div style="float: left; width: 60%;">

        <dspace:item-preview item="<%= item %>" />
        <dspace:item item="<%= item %>" collections="<%= collections %>" style="<%= displayStyle %>" />

<%
        if (displayAll)
        {
%>

            <div align="center">
<%
                if (workspace_id != null)
                {
%>
                    <form method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
                        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
                        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text1"/>" />
                    </form>
<%
                }
                else
                {
%>
                    <form method="get" action="<%= link %>">
                        <input type="hidden" name="mode" value="simple"/>
                        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text1"/>" />
                    </form>
<%
                }
%>
            </div>
<%
        }
        else
        {
%>
            <div align="center">
<%
                if (workspace_id != null)
                {
%>
                    <form method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
                        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
                        <input type="submit" name="submit_full" value="<fmt:message key="jsp.display-item.text2"/>" />
                    </form>
<%
                }
                else
                {
%>
                    <form method="get" action="<%= link %>">
                        <input type="hidden" name="mode" value="full"/>
                        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text2"/>" />
                    </form>
<%
                }
                if (suggestLink)
                {
%>
                    <%-- FIXME: This really ought to be escaped --%>
                    <a href="<%= request.getContextPath() %>/suggest?uri=<%= uri %>" target="new_window">
                       <fmt:message key="jsp.display-item.suggest"/></a>
<%
                }
%>
            </div>
<%
        }
%>
    </div>

    <!-- Start of Quambo display-item.jsp modifications -->

    <div style="float:left; width: 30%;">

<%
        if (request.getAttribute("user") != null)
        {
%>
            <span style="margin-left: 10px; font-size: 12pt; font-weight: bold;">Item Tools</span>

            <div style="border:1px solid #BBBBBB; margin-top:4px;">
<%
                Iterator<ResearchContext> notBookmarked = ((List<ResearchContext>) request.getAttribute("not_bookmarked_in")).iterator();
                if (notBookmarked.hasNext())
                {
%>
                    <form style="margin:10px;" method="post" action="<%= request.getContextPath()%>/resource/uuid/<%= item.getIdentifier().getUUID()%>">
                        <span style="font-size: 12pt; font-weight: bold;">Bookmark into:</span>
                        <select name="context_id">
<%
                            while (notBookmarked.hasNext())
                            {
                                ResearchContext r = notBookmarked.next();
%>
                                <option value="<%= r.getID() %>"><%= r.getName() %></option>
<%
                            }
%>
                        </select>
                        <input type="hidden" name="bookmark_action" value="bookmark" />
                        <input type="submit" name="Bookmark-Flag" value="Bookmark"/>
                    </form>
<%
                }

                Iterator<ResearchContext> bookmarked = ((List<ResearchContext>) request.getAttribute("bookmarked_in")).iterator();
                if (bookmarked.hasNext())
                {
%>
                    <form style="margin:10px;" method="post" action="<%= request.getContextPath()%>/resource/uuid/<%= item.getIdentifier().getUUID()%>">
                        <span style="font-size: 12pt; font-weight: bold;">Unbookmark from:</span>
                        <select name="context_id">
<%
                            while (bookmarked.hasNext())
                            {
                                ResearchContext r = bookmarked.next();
%>
                                <option value="<%= r.getID() %>"><%= r.getName() %></option>
<%
                        }
%>
                        </select>
                        <input type="submit" name="Bookmark-Flag" value="Unbookmark">
                        <input type="hidden" name="bookmark_action" value="unbookmark" />
                    </form>
<%
                }
%>
            </div>
<%
        }
%>

<%
        List<Item2ItemRecommendation> relatedItems = (List<Item2ItemRecommendation>)request.getAttribute("relatedItems");
        if (relatedItems.size() > 0)
        {
%>
            <span style="margin-left: 10px; font-size: 12pt; font-weight: bold;">Similar Items</span>

            <div style="border: 1px solid #BBBBBB; margin-top:4px;">

                <ul style="list-style-type: none; margin-left: -35px; margin-right:5px;">
<%
                    int i = relatedItems.size() - 1;
                    int counter = 0;
                    while (i >= 0)
                    {
%>
                        <li>
                            <a title="Similarity: <%= relatedItems.get(i).getSimilarity()%>" href="<%= request.getContextPath() + "/resource/uuid/" + relatedItems.get(i).getItem2().getIdentifier().getUUID() %>" style="text-decoration:none;"><span style="font-size: 12pt;"><%=relatedItems.get(i).getItem2().getName()%></span></a>
                        </li>
<%
                        if (counter >= 4)
                            break;
                        else
                            counter++;

                        if (i > 0)
                        {
%>
                            <div style="margin-top:10px; margin-left:0px; border-bottom: 1px dotted #000000;"></div>
<%
                        }
                        i--;
                    }
%>
                </ul>
            </div>
<%
        }
%>
    </div>

    <!-- End of Quambo display-item.jsp modifications -->    

    <div style="clear:both;">

<%
    if (workspace_id != null)
    {
%>
        <div align="center">
           <form method="post" action="<%= request.getContextPath() %>/workspace">
                <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>"/>
                <input type="submit" name="submit_open" value="<fmt:message key="jsp.display-item.back_to_workspace"/>"/>
            </form>
        </div>
<%
    }
%>
    <%-- SFX Link --%>
<%
    if (ConfigurationManager.getProperty("sfx.server.url") != null)
    {
%>
    <p align="center">
        <a href="<dspace:sfxlink item="<%= item %>"/>"><img src="<%= request.getContextPath() %>/image/sfx-link.gif" border="0" alt="SFX Query" /></a>
    </p>
<%
    }
%>
    <%-- Create Commons Link --%>
<%
    if (cc_url != null)
    {
%>
    <p class="submitFormHelp"><fmt:message key="jsp.display-item.text3"/> <a href="<%= cc_url %>"><fmt:message key="jsp.display-item.license"/></a><br/>
    <a href="<%= cc_url %>"><img src="<%= request.getContextPath() %>/image/cc-somerights.gif" border="0" alt="Creative Commons" /></a>
    </p>
    <!--
    <%= cc_rdf %>
    -->
<%
    }
%>
    <p class="submitFormHelp"><fmt:message key="jsp.display-item.copyright"/></p>

    </div>    

<%
    if (admin_button)  // admin edit button
    {
%>
    <dspace:sidebar>
    <table class="miscTable" align="center">
      <tr>
	    <td class="evenRowEvenCol" colspan="2">
	     <table>
            <tr>
              <th id="t1" class="standard">
                 <strong><fmt:message key="jsp.admintools"/></strong>
              </th>
            </tr>
        <tr>
              <td headers="t1" class="standard" align="center">
                <form method="get" action="<%= request.getContextPath() %>/tools/edit-item">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="submit" name="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
            </td>
            <td class="evenRowEvenCol" align="center">
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                    <%--<input type="submit" name="submit" value="Edit...">--%>
                    <input type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.item"/>" />
                </form>
            </td>
        </tr>
        </table>
        </td></tr></table>
    </dspace:sidebar>
<%
    }
%>
</dspace:layout>
