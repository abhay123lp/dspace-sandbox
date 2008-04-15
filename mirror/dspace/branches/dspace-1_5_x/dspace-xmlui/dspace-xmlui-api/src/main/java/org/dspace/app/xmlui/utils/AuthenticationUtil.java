/*
 * Authenticate.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/05 21:39:29 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Methods for authenticating the user. This is DSpace platform code, as opposed
 * to the site-specific authentication code, that resides in implementations of
 * the org.dspace.eperson.AuthenticationMethod interface.
 * 
 * @author Scott Phillips
 * @author Robert Tansley
 */

public class AuthenticationUtil
{
    private static final Logger log = Logger.getLogger(AuthenticationUtil.class);

    /**
     * Session attribute name for storing the return url where the user should
     * be redirected too once successfully authenticated.
     */
    public static final String REQUEST_INTERRUPTED = "dspace.request.interrupted";
    public static final String REQUEST_RESUME = "dspace.request.resume";
    
    /**
     * These store a message giving a reason for why the request is being interrupted.
     */
    public static final String REQUEST_INTERRUPTED_HEADER = "dspace.request.interrupted.header";
    public static final String REQUEST_INTERRUPTED_MESSAGE = "dspace.request.interrupted.message";
    public static final String REQUEST_INTERRUPTED_CHARACTERS = "dspace.request.interrupted.characters";

    
    /**
     * The IP address this user first logged in from, do not allow this session for
     * other IP addresses.
     */
    private static final String CURRENT_IP_ADDRESS = "dspace.user.ip";
    
    /**
     * The effective user id, typically this will never change. However if an administrator 
     * has assumed login as this user then they will differ.
     */
    private static final String EFFECTIVE_USER_ID = "dspace.user.effective";
    private static final String AUTHENTICATED_USER_ID = "dspace.user.authenticated";
    
    /**
     * Authenticate the current DSpace content based upon given authentication
     * credentials. The AuthenticationManager will consult the configured
     * authentication stack to determine the best method.
     * 
     * @param objectModel
     *            Cocoon's object model.
     * @param email
     *            The email credentials provided by the user.
     * @param password
     *            The password credentials provided by the user.
     * @param realm
     *            The realm credentials proveded by the user.
     * @return Return a current context with either the eperson attached if the
     *         authentication was successfull or or no eperson attached if the
     *         attempt failed.
     */
    public static Context Authenticate(Map objectModel, String email, String password, String realm) 
    throws SQLException
    {
        // Get the real HttpRequest
        HttpServletRequest request = (HttpServletRequest) objectModel
                .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        int implicitStatus = AuthenticationManager.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }
        else
        {
            // If implicit authentication failed, fall over to explicit.

            int explicitStatus = AuthenticationManager.authenticate(context,
                    email, password, realm, request);

            if (explicitStatus == AuthenticationMethod.SUCCESS)
            {
                // Logged in OK.
                log.info(LogManager
                        .getHeader(context, "login", "type=explicit"));
                AuthenticationUtil.logIn(context, request, context
                        .getCurrentUser());
            }
            else
            {
                log.info(LogManager.getHeader(context, "failed_login", "email="
                        + email + ", realm=" + realm + ", result="
                        + explicitStatus));
            }
        }

        return context;
    }

    /**
     * Preform implicite authentication. The authenticationManager will consult
     * the authentication stack for any methods that can implicitly authenticate
     * this session. If the attempt was successfull then the returned context
     * will have an eperson attached other wise the context will not have an
     * eperson attached.
     * 
     * @param objectModel
     *            Cocoon's object model.
     * @return This requests DSpace context.
     */
    public static Context AuthenticateImplicit(Map objectModel)
            throws SQLException
    {
        // Get the real HttpRequest
        final HttpServletRequest request = (HttpServletRequest) objectModel
                .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        int implicitStatus = AuthenticationManager.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }

        return context;
    }

    /**
     * Log the given user in as a real authenticated user. This should only be used after 
     * a user has presented their credintals and they have been validated. 
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param eperson
     *            the eperson logged in
     */
    private static void logIn(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
        if (eperson == null)
            return;
        
        HttpSession session = request.getSession();

        context.setCurrentUser(eperson);

        // Set any special groups - invoke the authentication mgr.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context,
                request);
        for (int groupID : groupIDs)
            context.setSpecialGroup(groupID);

        // and the remote IP address to compare against later requests
        // so we can detect session hijacking.
        session.setAttribute(CURRENT_IP_ADDRESS, request.getRemoteAddr());
        
        // Set both the effective and authenticated user to the same.
        session.setAttribute(EFFECTIVE_USER_ID, eperson.getID());
        session.setAttribute(AUTHENTICATED_USER_ID,eperson.getID());
        
    }

    /**
     * Check to see if there are any session attributes indicating a currently authenticated 
     * user. If there is then log this user in.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP Request
     */
    public static void resumeLogin(Context context, HttpServletRequest request)
            throws SQLException
    {
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            Integer id = (Integer) session.getAttribute(EFFECTIVE_USER_ID);

            if (id != null)
            {
                String address = (String)session.getAttribute(CURRENT_IP_ADDRESS);
                if (address != null && address.equals(request.getRemoteAddr()))
                {
                    EPerson eperson = EPerson.find(context, id);
                    context.setCurrentUser(eperson);

                    // Set any special groups - invoke the authentication mgr.
                    int[] groupIDs = AuthenticationManager.getSpecialGroups(context, request);
                    for (int groupID : groupIDs)
                        context.setSpecialGroup(groupID);
                }
                else
                {
                    // Possible hack attempt.
                }
            } // if id
        } // if session
    }

    /**
     * Assume the login as another user. Only site administrators may preform the action.
     * 
     * @param context
     * 		The current DSpace context loged in as a site administrator
     * @param request
     * 		The reall HTTP request.
     * @param loginAs
     * 		Whom to login as.
     */
    public static void loginAs(Context context, HttpServletRequest request, EPerson loginAs ) 
    throws SQLException, AuthorizeException
    {
    	// Only allow loginAs if the administrator has allowed it.
    	if (!ConfigurationManager.getBooleanProperty("xmlui.user.assumelogin", false))
    		return;
    	
    	// Only super administrators can login as someone else.
    	if (!AuthorizeManager.isAdmin(context))
    		throw new AuthorizeException("Only site administrators may assume login as another user.");
    		
    	// Just to be double be sure, make sure the administrator
    	// is the one who actualy authenticated themself.
	    HttpSession session = request.getSession(false);
	    Integer authenticatedID = (Integer) session.getAttribute(AUTHENTICATED_USER_ID); 
	    if (context.getCurrentUser().getID() != authenticatedID)
	    	throw new AuthorizeException("Only authenticated users whom are administrators may assume the login as another user.");
	    
	    // You may not assume the login of another super administrator
	    if (loginAs == null)
	    	return;
	    Group administrators = Group.find(context,1);
	    if (administrators.isMember(loginAs))
	    	throw new AuthorizeException("You may not assume the login as another super administrator.");
	    
	    // Success, allow the user to login as another user.
	    context.setCurrentUser(loginAs);
	
        // Set any special groups - invoke the authentication mgr.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context,request);
        for (int groupID : groupIDs)
            context.setSpecialGroup(groupID);
	    	        
        // Set both the effective and authenticated user to the same.
        session.setAttribute(EFFECTIVE_USER_ID, loginAs.getID());
    }
    
    
    /**
     * Log the user out.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     */
    public static void logOut(Context context, HttpServletRequest request) throws SQLException
    {
        HttpSession session = request.getSession();

        if (session.getAttribute(EFFECTIVE_USER_ID) != null &&
        	session.getAttribute(AUTHENTICATED_USER_ID) != null)
        {
    	    Integer effectiveID = (Integer) session.getAttribute(EFFECTIVE_USER_ID); 
    	    Integer authenticatedID = (Integer) session.getAttribute(AUTHENTICATED_USER_ID); 
    	    
    	    if (effectiveID != authenticatedID)
    	    {
    	    	// The user has login in as another user, instead of logging them out, 
    	    	// revert back to their previous login name.
    	    	
    	    	EPerson authenticatedUser = EPerson.find(context, authenticatedID);
    	    	context.setCurrentUser(authenticatedUser);
    	    	session.setAttribute(EFFECTIVE_USER_ID, authenticatedID);
    	    	return;
    	    }
        }
        
        // Otherwise, just log the person out as normal.
        context.setCurrentUser(null);
        session.removeAttribute(EFFECTIVE_USER_ID);
        session.removeAttribute(AUTHENTICATED_USER_ID);
        session.removeAttribute(CURRENT_IP_ADDRESS);
    }

    /**
     * Determine if the email can register them selfs or need to be
     * created by a site administrator first.
     * 
     * @param objectModel
     *          The Cocoon object model
     * @param email
     *          The email of the person to be registered.
     * @return true if the email can register, otherwise false.
     */
    public static boolean canSelfRegister(Map objectModel, String email) throws SQLException 
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        
        return AuthenticationManager.canSelfRegister(context,request,email);
    }
    
    /**
     * Determine if the EPerson (to be created or allready created) has the
     * ability to set their own password.
     * 
     * @param objectModel
     *              The Cocoon object model
     * @param email
     *              The email address of the EPerson.
     * @return
     */
    public static boolean allowSetPassword(Map objectModel, String email) throws SQLException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        
        return AuthenticationManager.allowSetPassword(context, request, email);
    }
    
    /**
     * Construct a new, mostly blank, eperson for the given email address. This should
     * only be called once the email address has been verified.
     * 
     * @param objectModel 
     *              The Cocoon object model.
     * @param email
     *              The email address of the new eperson.
     * @return A newly created EPerson object.
     */
    public static EPerson createNewEperson(Map objectModel, String email) throws 
        SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        
        Context context = ContextUtil.obtainContext(objectModel);
        
        // Need to create new eperson
        // FIXME: TEMPORARILY need to turn off authentication, as usually
        // only site admins can create e-people
        context.setIgnoreAuthorization(true);
        EPerson eperson = EPerson.create(context);
        eperson.setEmail(email);
        eperson.setCanLogIn(true);
        eperson.setSelfRegistered(true);
        eperson.update();
        context.setIgnoreAuthorization(false);
        
        // Give site auth a chance to set/override appropriate fields
        AuthenticationManager.initEPerson(context, request, eperson);
        
        return eperson;   
    }
    
    
    
    
    
    /**
     * Is there a currently interuppted request?
     * 
     * @param objectModel The Cocoon object Model
     */
    public static boolean isInterupptedRequest(Map objectModel) 
    {
    	final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
    	
    	Object interruptedObject = session.getAttribute(REQUEST_INTERRUPTED);
    	
    	if (interruptedObject instanceof RequestInfo)
    	{
    		// There is currently either an interrupted or yet-to-be resumed request.
    		return true;
    	}
    		
    	// There are not interupted requests.
    	return false;
    }
    
    
    
    /**
     * Interrupt the current request and store if for later resumption. This request will
     * send an http redirect telling the client to authenticate first. Once that has been finished
     * then the request can be resumed.
     * 
     * @param objectModel The Cocoon object Model
     * @param header A message header (i18n tag)
     * @param message A message for why the request was interrupted (i18n tag)
     * @param characters An untranslated messsage, perhaps an error message?
     */
    public static void interruptRequest(Map objectModel, String header, String message, String characters)
    {
    	final HttpServletRequest request = 
    		(HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
        
        // Store this interrupted request untill after the user successfully authenticates.
        RequestInfo interruptedRequest = new RequestInfo(request);
        
        // Set the request as interrupted
        session.setAttribute(REQUEST_INTERRUPTED,interruptedRequest);
        session.setAttribute(REQUEST_RESUME, null); // just to be clear.
        
        // Set the interrupt message
        session.setAttribute(REQUEST_INTERRUPTED_HEADER, header);
        session.setAttribute(REQUEST_INTERRUPTED_MESSAGE, message);
        session.setAttribute(REQUEST_INTERRUPTED_CHARACTERS, characters);
        
        
    }
    
    
    /**
     * Set the interrupted request to a resumable state. The
     * next request that the server recieves (for this session) that
     * has the same servletPath will be replaced with the previously
     * inturrupted request.
     * 
     * @param objectModel The Cocoon object Model
     * @return
     */
    public static String resumeInterruptedRequest(Map objectModel)
    {
    	final HttpServletRequest request = 
    		(HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
        
    	// Clear the interrupt message
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER, null);
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE, null);
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS, null);
    	
        // Set the request as interrupted
        Object interruptedObject = session.getAttribute(REQUEST_INTERRUPTED);
        if (interruptedObject instanceof RequestInfo)
        {
        	RequestInfo interruptedRequest = (RequestInfo) interruptedObject;
        	
        	session.setAttribute(REQUEST_INTERRUPTED, null);
        	session.setAttribute(REQUEST_RESUME, interruptedRequest); 
        	
        	// Return the path for which this request belongs too. Only urls
        	// for this path may be resumed.
        	return interruptedRequest.getServletPath();
        }
        
        // No request was interrupted.
        return null;
    }
    
    
    
    /**
     * Check to see if this request should be resumed.
     * 
     * @param realHttpRequest The current real request
     * @return Either the current real request or a stored request that was previously interrupted.
     */
    public static HttpServletRequest resumeRequest(HttpServletRequest realHttpRequest) 
    {
    	// First check to see if there is a resumed request.
    	HttpSession session = realHttpRequest.getSession();
    	//session.setMaxInactiveInterval(60);
        Object object = session.getAttribute(REQUEST_RESUME);
    	
        // Next check to make sure it's the right type of object, 
        // there should be no condition where it is not - but always 
        // safe to check.
        if (object instanceof RequestInfo)
        {
        	RequestInfo interruptedRequest = (RequestInfo) object;
        
        	// Next, check to make sure this real request if for the same url
        	// path, if so then resume the previous request.
        	String interruptedServletPath = interruptedRequest.getServletPath();
        	String realServletPath = realHttpRequest.getServletPath();
        	
        	if (realServletPath != null && 
        		realServletPath.equals(interruptedServletPath))
        	{
        		// Clear the resumed request and send the request back to be resumed.
        		session.setAttribute(REQUEST_INTERRUPTED, null);
        		session.setAttribute(REQUEST_RESUME, null);
 
        		return interruptedRequest.wrapRequest(realHttpRequest);
        	}
        }
    	// Otherwise return the real request.
    	return realHttpRequest;
    }
    
}
