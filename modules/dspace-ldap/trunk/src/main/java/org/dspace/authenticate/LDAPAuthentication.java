/*
 * LDAPAuthentication.java
 *
 * Version: $Revision: 3051 $
 *
 * Date: $Date: 2008-08-17 09:11:20 -0500 (Sun, 17 Aug 2008) $
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
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Authentication module to authenticate against a flat LDAP tree where
 * all users are in the same unit.
 *
 * @author Larry Stone, Stuart Lewis
 * @version $Revision: 3051 $
 */
public class LDAPAuthentication
    implements AuthenticationMethod {

    /** log4j category */
    private static final Logger olog = Logger.getLogger(LDAPAuthentication.class);

    private final SpeakerToLDAP  oldap;

    /**
     * Constructor injects SpeakerToLDAP dependency
     *
     * @param ldap SpeakerToLDAP knows how to authenticate
     *                 against and query the LDAP directory
     * @param b_autoregister set true to auto-register an eperson
     *               for a new user that succesfully authenticates
     *               with LDAP
     * @param v_special_group ids of user groups that an LDAP-authenticated
     *          user should be considered an implicit member of
     */
    public LDAPAuthentication ( SpeakerToLDAP ldap,
                                boolean b_autoregister,
                                int[] v_special_group
                                ) {
        oldap = ldap;
        ob_autoregister = b_autoregister;
        ov_special_group = v_special_group;
    }

    /**
     * Constructor invoked by dspace.cfg based configuration
     * engine sets up DefaultSpeakerToLDAP,
     * checks ldap.autoregister and ldap.dspace.autogroup
     * configuration values to determine canSelfRegister
     * and getSpecialGroups property values.
     */
    public LDAPAuthentication () {
        String s_groups = ConfigurationManager.getProperty("ldap.dspace.autogroup");

        List<Integer> v_group_id = new ArrayList<Integer>();
        if ( null != s_groups ) {
            String[]     v_group_name = s_groups.trim ().split( ",\\s*" );
            for ( int i=0; i < v_group_name.length; ++i ) {
                String s_group = v_group_name[i].trim ();
                if ( s_group.length () > 0 ) {
                    try {
                        v_group_id.add ( Integer.parseInt( s_group ) );
                    } catch ( Exception e ) {
                        olog.warn( "Exception parsing group " + s_group, e );
                    }
                }
            }
        }
        oldap = new DefaultSpeakerToLDAP ();
        ob_autoregister = ConfigurationManager.getBooleanProperty("webui.ldap.autoregister");
        ov_special_group = new int[ v_group_id.size () ];
        int i_count = 0;
        for ( Integer i_group_id : v_group_id ) {
            ov_special_group[ i_count ] = i_group_id;
            ++i_count;
        }
    }

    private final boolean ob_autoregister;
    /**
     * Let a real auth method return true if it wants.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException
    {
        // XXX might also want to check that username exists in LDAP.
        return ob_autoregister;
    }

    /**
     *  Nothing here, initialization is done when auto-registering.
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson)
        throws SQLException
    {
        // XXX should we try to initialize netid based on email addr,
        // XXX  for eperson created by some other method??
    }

    /**
     * Cannot change LDAP password through dspace, right?
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException
    {
        // XXX is this right?
        return false;
    }

    /*
     * This is an explicit method.
     */
    public boolean isImplicit()
    {
        return false;
    }

    private final int[] ov_special_group;
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        return ov_special_group;
    }

    /*
     * MIT policy on certs and groups, so always short-circuit.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     */
    public int authenticate(Context context,
                            String netid,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException
    {
        olog.info(LogManager.getHeader(context, "auth", "attempting auth of user="+netid));

        // Skip out when no netid or password is given.
        if (netid == null || password == null) {
            return BAD_ARGS;
        }

        // Locate the eperson
        EPerson      eperson = null;
        try
        {
            eperson = EPerson.findByNetid(context, netid.toLowerCase());
        }
        catch (SQLException e)
        {
        }

        olog.debug( "Found eperson for " + netid );

        // if they entered a netid that matches an eperson
        if (eperson != null)
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate()) {
                return CERT_REQUIRED;
            } else if (!eperson.canLogIn()) {
                return BAD_ARGS;
            }
            try {
                // authenticate
                olog.debug( "Attempting LDAP auth-1 for " + netid );
                DataFromLDAP ldap_info = oldap.ldapAuthenticate( netid, password );
                if ( null != ldap_info ) {
                    context.setCurrentUser(eperson = EPerson.findByNetid(context, netid.toLowerCase()));
                    olog.info(LogManager
                             .getHeader(context, "authenticate", "type=ldap"));
                    return SUCCESS;
                } 
            } catch ( NamingException e ) {
                olog.warn( "Failed to authenticate user: " + netid, e );
            }
            //else {
            return BAD_CREDENTIALS;
        } 
        // eperson == null
        if ( null != eperson ) {
            throw new AssertionError( "eperson should be null here!" );
        }
        olog.debug( "Attempting LDAP auth-2 for " + netid );
        DataFromLDAP ldap_info = null;
        try {
            ldap_info = oldap.ldapAuthenticate( netid, password );
        } catch ( NamingException e ) {
            olog.warn( "Failed to authenticate user: " + netid, e );
        }
        if ( (null == ldap_info) 
             || (ldap_info.getEmail()==null)
             || ldap_info.getEmail().equals("")
             ) {
            return BAD_ARGS; // failed to authenticate or get e-mail address
        }

        //
        // autoregister the ldap-authenticated user
        //
        olog.info(LogManager.getHeader(context,
                                      "autoregister", "netid=" + netid)
                 );
        
        try {
            eperson = EPerson.findByEmail(context, ldap_info.getEmail());
            if (eperson!=null) {
                // Just need to set the netid on the eperson record
                olog.info(LogManager.getHeader(context,
                                              "type=ldap-login", "type=ldap_but_already_email"));
                context.setIgnoreAuthorization(true);
                eperson.setNetid(netid);
                eperson.update();
                context.commit();
                context.setIgnoreAuthorization(false);
                context.setCurrentUser(eperson);
                return SUCCESS;
            } else if (canSelfRegister(context, request, netid)) {
                // TEMPORARILY turn off authorisation
                try {
                    context.setIgnoreAuthorization(true);
                    eperson = EPerson.create(context);
                    eperson.setEmail(ldap_info.getEmail());
                    if ((ldap_info.getGivenName()!=null)
                        &&(!ldap_info.getGivenName().equals(""))
                        ) {
                        eperson.setFirstName(ldap_info.getGivenName());
                    }
                    if ((ldap_info.getSurname()!=null)
                        &&(!ldap_info.getSurname().equals(""))
                        ) { 
                        eperson.setLastName(ldap_info.getSurname());
                    }
                    if ((ldap_info.getPhone()!=null)
                        &&(!ldap_info.getPhone().equals(""))
                        ) { 
                        eperson.setMetadata("phone", ldap_info.getPhone());
                    }
                    eperson.setNetid(ldap_info.getNetId());
                    eperson.setCanLogIn(true);
                    AuthenticationManager.initEPerson(context, request, eperson);
                    eperson.update();
                    context.commit();
                    context.setCurrentUser(eperson);
                } catch (AuthorizeException e) {
                    return NO_SUCH_USER;
                } finally {
                    context.setIgnoreAuthorization(false);
                }
                olog.info(LogManager.getHeader(context, "authenticate",
                                              "type=ldap-login, created ePerson"));
                return SUCCESS;
            } else {
                // No auto-registration for valid certs
                olog.info(LogManager.getHeader(context,
                                              "failed_login", "type=ldap_but_no_record"));
                return NO_SUCH_USER;
            }
        } catch (AuthorizeException e) {
            eperson = null;
            // authentication failed
            return BAD_ARGS;
        } finally {
            context.setIgnoreAuthorization(false);
        }
        // Unreachable!
    }


    /*
     * Returns URL to which to redirect to obtain credentials (either password
     * prompt or e.g. HTTPS port for client cert.); null means no redirect.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context,
                            HttpServletRequest request,
                            HttpServletResponse response)
    {
        return response.encodeRedirectURL(request.getContextPath() +
                                          "/ldap-login");
    }

    /**
     * Returns message key for title of the "login" page, to use
     * in a menu showing the choice of multiple login methods.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return "org.dspace.eperson.LDAPAuthentication.title";
    }
}
