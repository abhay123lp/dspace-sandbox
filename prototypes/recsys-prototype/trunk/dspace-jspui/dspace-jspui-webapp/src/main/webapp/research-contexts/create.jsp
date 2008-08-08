<%--
- create.jsp
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
  - Allow a user to configure a Research Context to suit certain needs
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.dspace.app.webui.servlet.ResearchContextServlet" %>
<%@ page import="org.dspace.app.recsys.researchContext.ResearchContext" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>

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
    table.form {
        empty-cells: show;
        margin-left: 25%;
    }
    td {
        font-size: 12pt;
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
    p.text {
        margin-left: 25%;
        width: 50%;
        font-size: 12pt;
    }
</style>

<dspace:layout locbar="link"
               parentlink="/research-contexts"
               parenttitlekey="jsp.quambo.research-contexts.title"
               titlekey="jsp.quambo.research-contexts.create.title">

    <table width="100%" border="0">
        <tr>
            <td align="left">
                <h1>Create Research Context</h1>
            </td>
        </tr>
    </table>

    <div class="main">
        <p class="text">
            Creating a new Research Context means you can bookmark repository
            items into different 'buckets'. When you bookmark items based on
            what you perceive to be similar, the metadata extracted to generate
            recommendations should be focused.
        </p>
        <p class="text">
            You can seed the creation of a new Research Context with the address
            of a Research Context Essence Atom feed. If you don't have one,
            don't worry.
        </p>
        <form method="post"
              action="<%= request.getContextPath()%>/research-contexts/create" >
            <input type="hidden" name="step"
                value="<%= ResearchContextServlet.CREATE_RESEARCH_CONTEXT %>" />
            <table class="form" cellspacing="5">
                <tr>
                    <td>Context Name</td>
                    <td >
                        <input type="text" name="context_name" />
                    </td>
                </tr>
                <tr>
                    <td>Seed with Essence URL</td>
                    <td>
                        <input type="text" name="essenceURL" />
                    </td>
                </tr>
                <tr>
                    <td>Essence Name</td>
                    <td>
                        <input type="text" name="essenceName" />
                    </td>
                </tr>
                <tr>
                    <td colspan="2" align="center">
                        <input type="submit" name="submit_create"
                               value="Confirm" />
                        <input type="submit" name="submit_cancel"
                               value="Cancel" />
                    </td>
                </tr>
            </table>
        </form>
    </div>
    <div class="side">
        <ul>
            <li class="selected">
                <a href="<%= request.getContextPath()%>
                /research-contexts/create">New Research Context</a>
            </li>
            <%
                List<ResearchContext> rCs =
                        (List<ResearchContext>)request.
                                getAttribute("researchContexts");
                Iterator<ResearchContext> iterator = rCs.iterator();
                while (iterator.hasNext())
                {
                    ResearchContext r = iterator.next();
                    String uri = request.getContextPath() +
                            "/research-contexts?research_context_id=" +
                            r.getID();
            %>
            <li class="unselected">
                <a href="<%= uri %>"><%= r.getName() %></a>
            </li>
            <%
                }
            %>
        </ul>
    </div>
</dspace:layout>
