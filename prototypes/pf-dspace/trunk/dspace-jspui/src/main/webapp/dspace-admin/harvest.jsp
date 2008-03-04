<%--
  - harvest.jsp
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

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.federate.HarvestThread"  %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.dspace-admin.harvest.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">
  
<table width=95%>
    <tr>
      <td align=left>
          <h1><fmt:message key="jsp.dspace-admin.harvest.title"/></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="/help/site-admin.html"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>
   
  <p align="center">
   <table border="1" width="37%" cellspacing="0" cellpadding="4" style="border-collapse: collapse" bgcolor="#FFFFEC" height="87">
	<tr>
  		<td style="font-size:12px;line-height:200%" align=center>Harvesting items, please wait...                                                                                           
  		<marquee style="border:1px solid #000000" direction="right" width="300" scrollamount="5"
			scrolldelay="10" bgcolor="#ECF2FF">
  			<table cellspacing="1" cellpadding="0">
 			  <tr height=8>
  				<td bgcolor=#3399FF width=8></td>
 				 <td></td>
 				 <td bgcolor=#3399FF width=8></td>
 				 <td></td>
 				 <td bgcolor=#3399FF width=8></td>
 				 <td></td>
  				<td bgcolor=#3399FF width=8></td>
 				 <td></td>
  			  </tr>
  			 </table>
  			</marquee>
  		 </td>
      </tr>
   </table>
   
   <form action="<%= request.getContextPath() %>/dspace-admin/remote-repositories" method="POST" >
        <input type="submit" name="submit_stop_harvest" value="Stop">
   </form>
  
  <p> 
   <a href="<%= request.getContextPath() %>/dspace-admin/remote-repositories"><fmt:message key="jsp.dspace-admin.harvest.back" /></a>
  </p>
</p>
                                                                                             
                                                                                                  
    <%-- <TABLE WIDTH="60%" ALIGN="CENTER"
            BORDER=1 CELLPADDING=0 CELLSPACING=2>
        <TR>
            <% int percent = 1000;%>
            <% for (int i = 10; i <= percent; i += 10) 
               { 
            %>
                  <TD WIDTH="10%" BGCOLOR="#000080">&nbsp;</TD>
            <% 
            	}
             %>
             <% for (int i = 100; i > percent; i -= 10) 
                { 
             %>
                   <TD WIDTH="10%">&nbsp;</TD>
             <% 
                } 
             %>
        </TR>
    </TABLE>
    
    <% HarvestThread task = (HarvestThread)request.getAttribute("harvest.thread"); %>
                                                                                                     
    <TABLE WIDTH="100%" BORDER=0 CELLPADDING=0 CELLSPACING=0>
        <TR>
            <TD ALIGN="CENTER">
               
                   Harvest is running, please wait...                 
                    
            </TD>
        </TR>
                                                                                                     
        <TR>
            <TD ALIGN="CENTER">
                <BR>
                <% if (task.isRunning()) 
                   { 
                %>
                    <FORM METHOD=POST>
                        <INPUT TYPE="SUBMIT" VALUE="Stop">
                    </FORM>
                <% } %> 
            </TD>
        </TR>
    </TABLE> --%>
 </dspace:layout>                                                                                                    
