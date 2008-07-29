/*
 * RADIUServlet.java
 *
 * Version: $Revision: 0.5 $
 *
 * Date: $Date: 2007/08/06 11:34:00 $
 *                                                                                                                                                     
 * Copyright (c) 2007, IRICUP.  All rights reserved.                                                                                                            
 *                                                                                                                                                           
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.                                                                                        *
 * - Redistributions in binary form must reproduce the above copyright                                                                                  * notice, this list of conditions and the following disclaimer in the                                                                                  * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the IRICUP nor the names of their
 * contributors may be used to endorse or promote products derived from 
 * this software without specific prior written permission.
 *

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT                                                                                 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,                                                                                
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH 
 * DAMAGE. */


package org.dspace.app.webui.servlet;

import org.dspace.eperson.Group;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.authenticate.AuthenticationManager;
import java.net.InetAddress;
import net.sf.jradius.packet.attribute.RadiusAttribute;
import net.sf.jradius.packet.attribute.value.AttributeValue;
import net.sf.jradius.client.auth.RadiusAuthenticator;
import net.sf.jradius.client.RadiusClient;
import net.sf.jradius.dictionary.Attr_NASPort;
import net.sf.jradius.dictionary.Attr_NASPortType;
import net.sf.jradius.dictionary.Attr_ReplyMessage;
import net.sf.jradius.dictionary.Attr_UserName;
import net.sf.jradius.dictionary.Attr_UserPassword;
import net.sf.jradius.packet.AccessAccept;
import net.sf.jradius.packet.AccessRequest;
import net.sf.jradius.packet.RadiusRequest;
import net.sf.jradius.packet.RadiusResponse;
import net.sf.jradius.packet.attribute.AttributeFactory;
import net.sf.jradius.packet.attribute.AttributeList;

/**
 * RADIUS username and password authentication servlet using tinyradius. 
 *
 * @author  Marcelo Rodrigues (Reitoria da Universidade do Porto)
 * @version $Revision: 0.5 $
 */

public class RADIUSServlet extends DSpaceServlet 
{
    private static Logger log = Logger.getLogger(RADIUSServlet.class);
    
    protected void doDSGet(Context context,
			   HttpServletRequest request,
			   HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // check if Radius is enabled on the configuration file 
	boolean radius_enabled = ConfigurationManager.getBooleanProperty("radius.enable");

        if (radius_enabled)
	    {
		JSPManager.showJSP(request, response, "/login/radius.jsp");
		log.info(LogManager.getHeader(context, "[RADIUSServlet]", "Radius is enabled on the configuration file"));
	    }
        else
	    {
		log.info(LogManager.getHeader(context, "[RADIUSServlet]", "Radius is disabled on the configuration file. Redirecting to general Dspace authentication form"));
		JSPManager.showJSP(request, response, "/login/password.jsp");
	    }
    }
    
    protected void doDSPost(Context context,
			    HttpServletRequest request,
			    HttpServletResponse response) throws ServletException, 
								 IOException, SQLException, AuthorizeException
    {
	
	// Process email and password fields
        String netid = request.getParameter("login_netid");
        String password = request.getParameter("login_password");
     	EPerson eperson; 
	
	try {
	    
	    String radius_server = ConfigurationManager.getProperty("radius.server_ip");
	    String radius_key = ConfigurationManager.getProperty("radius.shared_key");
	    int radius_authport = ConfigurationManager.getIntProperty("radius.authport");
	    int radius_actport = ConfigurationManager.getIntProperty("radius.actport");
	    int radius_timeout = ConfigurationManager.getIntProperty("radius.timeout");
	    boolean RadiusDebug = ConfigurationManager.getBooleanProperty("radius.debug");
	    
	    	    
	    //reads radius configuration. In case of no configuration, sets default values
	    if (radius_authport==0) 
		{ 
		    radius_authport=1812; 
		}
	    
	    if (radius_actport==0)  
		{ 
		    radius_actport=1813; 
		}
	    
	    if (radius_timeout==0)  
		{ 
		    radius_timeout=6; 
		}
	    
	    if (RadiusDebug) 
		{
		    log.info(LogManager.getHeader(context, "[RADIUSServlet]RADIUS auth port", "" + radius_authport));        	
		    log.info(LogManager.getHeader(context, "[RADIUSServlet]RADIUS timeout", "" + radius_timeout));        	
		    log.info(LogManager.getHeader(context, "[RADIUSServlet]RADIUS server ip", "" + radius_server));        	
		}
	    
	    AttributeFactory.loadAttributeDictionary("net.sf.jradius.dictionary.AttributeDictionaryImpl");	    
	    
	    InetAddress host = InetAddress.getByName(radius_server);
	    RadiusClient rc = new RadiusClient(host, radius_key, radius_authport, radius_actport,radius_timeout);
	    
	    if (RadiusDebug) 
		{
		    log.info(LogManager.getHeader(context, "[RADIUSServlet]Connection OK", ""));        	
		}
	    
	    AttributeList attrs = new AttributeList();
	    attrs.add(new Attr_UserName(netid));
	    attrs.add(new Attr_NASPortType(Attr_NASPortType.IAPP));
	    attrs.add(new Attr_NASPort(new Long(1)));
	    
	    
	    RadiusResponse auth_answer=authenticate(rc,attrs,context,password);
	    if (auth_answer!=null)
		{
		    log.info(LogManager.getHeader(context, "[RADIUSServlet]RADIUS login", "Accepted"));        	
		    
		    /* check if the user already exists */
		    if (netid!=null) 
			{
			    eperson = EPerson.findByEmail(context, netid);
			    if (eperson!=null)
				{
				    /* checks if it needs to remove/add an group to this user */
				    AutoAddUserToGroup(true,eperson,context,auth_answer);
				    log.info(LogManager.getHeader(context,"[RADIUSServlet]Known USER"," - allowing login"));
				    Authenticate.loggedIn(context, request, eperson);
				    Authenticate.resumeInterruptedRequest(request, response);
				    return;
				}
			}
		    //If we reach this point, means that the user is not in Dspace DB... creating internal user in Dspace
		    context.setIgnoreAuthorization(true);
		    eperson = EPerson.create(context);
		    eperson.setEmail(netid);
		    eperson.setNetid(netid);
		    eperson.setCanLogIn(true);
		    AuthenticationManager.initEPerson(context, request, eperson);
		    
		    //checks if it needs to add an user to this group
		    AutoAddUserToGroup(false,eperson,context,auth_answer);
		    
		    eperson.update();
		    context.commit();
		    context.setIgnoreAuthorization(false); 
		    Authenticate.loggedIn(context, request, eperson);
		    Authenticate.resumeInterruptedRequest(request, response);
		    log.info(LogManager.getHeader(context,"[RADIUSServlet]New USER", " - created internal login"));
		    return;
		}
	}
	catch (Exception e) { e.printStackTrace(); e.getMessage(); }
	// incorrect login/password...
	log.info(LogManager.getHeader(context,"[RADIUSServlet]failed_login","netid=" + netid));
	JSPManager.showJSP(request, response, "/login/radius-incorrect.jsp");
    }



    /**                                                                                                                                                       * This method calls all authentication methods, starting from the most secure
     */
    
    public static RadiusResponse authenticate(RadiusClient rc,AttributeList attrs,Context context,String password)
    {
	//EAP-TTLS
	RadiusResponse eapttls=try_auth_method(rc,attrs,"eap-ttls:trustAll=true",context,password);
	if (eapttls!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling EAP-TTLS authentication"));
		return eapttls; 
	    }
	
	//MSCHAP
	RadiusResponse mschap=try_auth_method(rc,attrs,"mschap",context,password);
	if (mschap!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling MSCHAP authentication"));
		return mschap; 
	    }
	
	//MSCHAPV1
	RadiusResponse mschapv1=try_auth_method(rc,attrs,"mschapv1",context,password);
	if (mschapv1!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling MSCHAPV1 authentication"));
		return mschapv1; 
	    }
	
	//MSCHAPV2
	RadiusResponse mschapv2=try_auth_method(rc,attrs,"mschapv2",context,password);
	if (mschapv2!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling MSCHAPV2 authentication"));
		return mschapv2; 
	    }

	//CHAP
	RadiusResponse chap=try_auth_method(rc,attrs,"chap",context,password);
	if (chap!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling CHAP authentication"));
		return chap; 
	    }
	
	//EAP-MD5
	RadiusResponse eapmd5=try_auth_method(rc,attrs,"eap-md5",context,password);
	if (eapmd5!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling EAP-MD5 authentication"));
		return eapmd5; 
	    }
	
	//PAP
	RadiusResponse pap=try_auth_method(rc,attrs,"pap",context,password);
	if (pap!=null) 
	    {
		log.info(LogManager.getHeader(context,"[RADIUSServlet] Authentication:"," handling PAP authentication"));
		return pap; 
	    }
	
	
	//all methods failed
	return null;
    }
    
    
    /**                                                                                                                                                       * This method tests an authentication method that is passed as an argument
     */
    public static RadiusResponse try_auth_method(RadiusClient rc,AttributeList attrs,String prot,Context context,String password)
    {
	try
	    {
		RadiusRequest request = new AccessRequest(rc, attrs);
		request.addAttribute(new Attr_UserPassword(password));
		
		RadiusAuthenticator a =RadiusClient.getAuthProtocol(prot);
		RadiusResponse reply = rc.authenticate((AccessRequest)request, a, 5);
		
		boolean isAuthenticated = (reply instanceof AccessAccept);
		
		if (isAuthenticated) 
		    {
			//valid authentication
			return reply;
		    }
		
		return null;
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
		return null;
	    }
    }
    
    
    
    
    protected EPerson AutoAddUserToGroup(boolean known_user, EPerson eperson,Context context,RadiusResponse auth_answer)
    {
	
	//If auto add a user to a group is disabled, stop here 
        boolean AutoAddGroup=ConfigurationManager.getBooleanProperty("group.add.auto");
        if (AutoAddGroup==false) 
	    {
		return eperson;
	    }
	
        /*If the directive in config is configured to false, we wont update group information on existing users, every login */
        boolean UpdateKnownUsers=ConfigurationManager.getBooleanProperty("group.update_known_users");
        
	if (known_user && !UpdateKnownUsers) 
	    {
		return eperson;
	    }
	
	log.info(LogManager.getHeader(context, "[RADIUSServlet]AUTOADD", " option is enabled"));


	Object[] aList;
	aList = auth_answer.findAttributes(Attr_ReplyMessage.TYPE);
	if (aList == null)
	    {
		System.out.println("No reply-message defined..\n");
		return eperson;
	    }
	
	for (int i=0; i<aList.length; i++)
	    {
		RadiusAttribute test= (RadiusAttribute) aList[i];
		AttributeValue attributeValue = test.getValue();
		
		String entire_result=attributeValue.toString();
		String[] split_result = entire_result.split("_");
		
		if (split_result[0].equals("Group") )
		    {
			//means that we are adding/removing permission for a Group                                                            
			
			try {
			    String[] result1 = split_result[1].split(":");
			    String radiusgroupaction=result1[1];
			    String AutoAddGroupName=result1[0];
			    Group especial = Group.findByName(context,AutoAddGroupName);
			    
			    if (radiusgroupaction.equals("yes;"))
				//add this user to the group                                                                                  
				{
				    if (especial == null)
					{
					    log.warn(LogManager.getHeader(context,"[RADIUSServlet]ERROR: Could not find dspace group:",AutoAddGroupName));
					}
				    else
					{
					    if (!especial.isMember(eperson))
						{
						    log.warn(LogManager.getHeader(context,"[RADIUSServlet] Adding user to the group",AutoAddGroupName));
						    especial.addMember(eperson);
						    especial.update();
						    context.commit();
						}
					}
				}
			    else if (radiusgroupaction.equals("no;"))
				{
				    //remove user from this group                                                                             
				    log.warn(LogManager.getHeader(context,"[RADIUSServlet] Removing user from the group",AutoAddGroupName));
				    if (especial.isMember(eperson))
					{
					    especial.removeMember(eperson);
					    especial.update();
					    context.commit();
					}
				}
			    else
				{
				    log.info(LogManager.getHeader(context, "[RADIUSServlet]AUTOADD Group - action \"" + radiusgroupaction 
								  + "\" unknown. Valid actions are","\"yes\" or \"no\""));
				}
			}
			catch (Exception e) {
			    return eperson;
			}
		    }
		else
		    {
			log.info(LogManager.getHeader(context, "[RADIUSSERVLET]AUTOADD Group - This is not a valid string for AUTOADD.Probably and radius answer for another service?",entire_result));
		    }
	    }
        return eperson;
    }
}

