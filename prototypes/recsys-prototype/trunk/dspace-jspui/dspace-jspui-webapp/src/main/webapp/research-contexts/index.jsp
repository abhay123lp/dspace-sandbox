<%--
  - main.jsp
  -
  - Version: $Revision: 9 $
  -
  - Date: $Date: 2007-05-25 21:32:15 +0100 (Fri, 25 May 2007) $
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
  Research Contexts Main Page
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.recsys.researchContext.ResearchContext" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.app.recsys.recommendation.Item2ResearchContextRecommendation" %>
<%@ page import="org.dspace.app.recsys.researchContext.KeyValue" %>
<%@ page import="java.util.*" %>

<style type="text/css">
    oddRowOddColumn {
        font-size: 12pt;
    }
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

<%
    ResearchContext rC = (ResearchContext)request.getAttribute("rC");
    UUID uuid = UUID.fromString(request.getAttribute("uuid").toString());
    String href = request.getServerName() + ":" + request.getServerPort() +
      request.getContextPath() + "/atom/";
%>

    <dspace:layout locbar="link"
                   parentlink="/research-contexts"
                   parenttitlekey="jsp.quambo.research-contexts.title"
                   titlekey="jsp.quambo.research-contexts.title"
                   nocache="true">

    <link rel="alternate" type="application/atom+xml" title="Recommended Items"
          href=<%= href + "recommended_items/" + rC.getUUID()%>/>
    <link rel="alternate" type="application/atom+xml" title="Bookmarked Items"
          href=<%= href + "bookmarked_items/" + rC.getUUID()%>/>
    <link rel="alternate" type="application/atom+xml" title="Essence"
          href=<%= href + "essence/" + rC.getUUID()%>/>

      <table width="100%" border="0">
        <tr>
            <td align="left">
                <h1>
                    <%= rC.getName() %>
                </h1>
            </td>
        </tr>
      </table>

    <div class="main">
    
            <div style="border-style:none; margin:0px;">

                <div style="margin: 0px;
                font-size: 14pt; padding: 3px;
                background-color: rgb(232, 238, 247);">
                    Recommended Items
                    <a href="
                 <%= "http://" + href + "recommended_items/" + uuid %>">
                        <img border="0" width="16" height="16"
                             src="research-contexts/image/atom_feed.gif">
                    </a>
                </div>

                <table style="width: 90%; margin-left:4px; margin-right: 8px;">
<%
                    Set<Item2ResearchContextRecommendation> r =
                      rC.getRecommendations();

                    if (r.size() > 0)
                    {
                        for (Item2ResearchContextRecommendation i: r)
                        {
%>
                            <tr>
                                 <td class="oddRowOddCol">
                                      <a title="Similarity: <%= i.getRelevance()
                                       %>"href="<%= request.getContextPath() +
                                       "/resource/uuid/" +
                                       i.getItem().getIdentifier().getUUID() %>
                                       "><%= i.getItem().getName() %></a>
                                 </td>
                            </tr>
<%
                        }

                            String uri = request.getContextPath() +
                            "/research-contexts/recommendations?uuid=" + uuid;
%>
                            <tr>
                                <td class="oddRowOddCol">
                                    <a href="<%= uri %>">
                                        Show all Recommended Items
                                    </a>
                                </td>
                            </tr>
<%
                    }
                    else
                    {
%>
                        <tr>
                            <td class="oddRowOddCol">
                                Quambo is not able to suggest any recommended
                                items to you at this time.
                            </td>
                        </tr>
<%
                    }
%>
                </table>

            </div>

            <div style="border-style: none; margin:0px;">

                <div style="margin: 0px;
                font-size: 14pt; padding: 3px;
                background-color: rgb(232, 238, 247);">
                    Bookmarked Items
                    <a href="
                 <%= "http://" + href + "bookmarked_items/" + uuid %>">
                        <img border="0" width="16" height="16"
                             src="research-contexts/image/atom_feed.gif">
                    </a>
                </div>

                <table style="width: 90%; margin-left:4px; margin-right: 8px;">
<%
                    List<Item> bookmarkedItems =
                      (List<Item>)request.getAttribute("bookmarkedItems");

                    if (bookmarkedItems.size() > 0)
                    {
                        int count = 0;

                        for (Item i: bookmarkedItems)
                        {
%>
                            <tr>
                                 <td class="oddRowOddCol">
                                      <a href="<%= request.getContextPath() +
                                      "/resource/uuid/" +
                                      i.getIdentifier().getUUID() %>">
                                          <%= i.getName() %></a>
                                 </td>
                            </tr>
<%
                            count ++;
                            if (count == 4)
                            {
                                break;
                            }
                        }
                            String bookmarkedItemsURI = request.getContextPath()
                            +"/research-contexts/bookmarks?uuid=" + uuid;
%>
                            <tr>
                                <td class="oddRowOddCol">
                                    <a href="<%= bookmarkedItemsURI %>">
                                        Show all Bookmarked Items
                                    </a>
                                </td>
                            </tr>
<%                    
                    }
                    else
                    {
%>
                        <tr>
                            <td class="oddRowOddCol" style="font-size:12pt;">
                                You have not bookmarked any items.
                            </td>
                        </tr>
<%
                    }
%>
                </table>

            </div>

            <div style="border-style: none; margin:0px;">

                <div style="margin: 0px;
                font-size: 14pt; padding: 3px;
                background-color: rgb(232, 238, 247);">
                    Essence
                    <a href="
                 <%= "http://" + href + "essence/" + rC.getUUID() %>">
                        <img border="0" width="16" height="16"
                             src="research-contexts/image/atom_feed.gif">
                    </a>
                </div>

                <div style="margin:8px; margin-top:0px;">
<%
                    Hashtable<KeyValue, Integer> essenceCloud =
                            rC.getEssenceCloud();

                    if (essenceCloud.size() > 0) {
%>
<%
                        Iterator<KeyValue> keyValues =
                                essenceCloud.keySet().iterator();

                        while (keyValues.hasNext())
                        {
                            KeyValue kv = keyValues.next();
                            String key = kv.getKey();
                            String uri = key.replace(" ", "+");
%>
                            <span
                               style="font-size:<%= essenceCloud.get(kv) %>pt;">
                                <a title="Weight: <%= kv.getValue() %>"
                                   style="color: #336699;"
                                   href="<%= request.getContextPath() %>
                                   /browse?type=subject&order=ASC&rpp=20&value=
                                   <%= uri %>"><%= key %></a>
                            </span>
<%
                        }
%>
<%
                    }
                    else
                    {
%>
                        <table>
                            <tr>
                                <td class="oddRowOddCol"
                                    style="margin-left:8px;">
                                    It has not been possible for Quambo to build
                                    a picture of your interests.
                                </td>
                            </tr>
                        </table>
<%
                    }
%>
              </div>

            </div>

    </div>

    <div class="side">
        <ul>            
            <li class="unselected">
                <a href="<%= request.getContextPath()%>
                /research-contexts/create">
                    New Research Context
                </a>
            </li>
<%
            List<ResearchContext> rCs =
                (List<ResearchContext>)request.getAttribute("researchContexts");

            Iterator<ResearchContext> iterator = rCs.iterator();

            while (iterator.hasNext())
            {
                rC = iterator.next();

                String uri = request.getContextPath() +
                             "/research-contexts?uuid=" +
                             rC.getUUID();

                if (rC.getUUID().equals(uuid))
                {
                    String configureURI = request.getContextPath() +
                             "/research-contexts/configure" +
                             "?uuid=" + rC.getUUID();
%>
                    <li class="selected">
                      <a href="<%=uri%>"><%= rC.getName() %></a>
                      <br />
                      <a style="font-size: 10pt;"
                         href="<%= configureURI %>">Configure</a>
                    </li>
<%              }
                else
                {
%>
                    <li class="unselected">
                      <a href="<%= uri %>"><%= rC.getName() %></a>
                    </li>
<%
                }
            }
%>
        </ul>
    </div>

</dspace:layout>
