<%--
  - bookmarks.jsp
  -
  - Version: $Revision: $
  -
  - Date: $Date: $
  -
  - Copyright (c) 2007, Hewlett-Packard Company and Massachusetts
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

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.recsys.bookmark.Bookmark" %>
<%@ page import="org.dspace.app.recsys.researchContext.ResearchContext" %>
<%@ page import="java.util.UUID" %>
<%@ page import="java.util.Iterator" %>

<%
    List<Bookmark> bookmarks = (List<Bookmark>)request.getAttribute("bookmarks");
    String href = request.getServerName() + ":" + request.getServerPort() +
      request.getContextPath() + "/atom/";
    ResearchContext rC = (ResearchContext)request.getAttribute("researchContext");
%>

<style type="text/css">
    div.main {
        border: 2px solid #C3D9FF;
        margin: 0px;
        margin-left: 10px;
        width: 75%;
        float: left;
    }
    div.side {
        border-style: none;
        width: 15%;
        float: left;
        margin: 0px 0px 0px -40px;
    }
    ul {
        list-style-type: none;
    }
    li.selected {
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
</style>

<dspace:layout locbar="link"
               parentlink="/research-contexts"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.bookmarks.title">

    <div class="main">

        <div style="margin: 0px; margin-left:8px; margin-right:8px;
            font-size: 14pt; padding: 3px;
            background-color: rgb(232, 238, 247);">
                All Bookmarked Items
                <a href="
             <%= "http://" + href + "bookmarked_items/" + rC.getUUID() %>">
                    <img border="0" width="16" height="16"
                         src="research-contexts/image/atom_feed.gif">
                </a>
            </div>

<%
        if (bookmarks.size() > 0)
        {
%>
            <table class="miscTable" style="margin: 10px; width: 90%;">

                <tr>
                    <th class="oddRowOddCol">Title</th>
                    <th class="oddRowEvenCol">Created</th>
                </tr>
<%
            String row = "even";

            for (Bookmark b: bookmarks)
            {
%>
                <tr>
                     <td class="<%= row %>RowOddCol">
                          <a href="<%= b.getItem().getIdentifier().getUUID() %>"><%= b.getItem().getName() %></a>
                     </td>
                    <td class="<%= row%>RowEvenCol">
                        <%= b.getCreated().toString()%>
                    </td>
                </tr>
<%
                row = (row.equals("even") ? "odd" : "even" );
            }
%>
            </table>
<%
        }
        else
        {
%>
            <p>
                You have not bookmarked any items.
            </p>
<%
        }
        String returnURI = request.getContextPath() +
                     "/research-contexts?uuid=" + rC.getUUID();
%>
        <p align="right">
            <a href="<%= returnURI %>">
                Back to <%= rC.getName()%>
            </a>
        </p>
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