package org.dspace.authenticate;

import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * Internal class to manage LDAP query and results, mainly
 * because there are multiple values to return.
 */
public class DefaultSpeakerToLDAP implements SpeakerToLDAP {
    private static final Logger olog = Logger.getLogger(DefaultSpeakerToLDAP.class);
    private final String os_provider_url;
    private final String os_id_field;
    private final String os_search_context;
    private final String os_object_context;
    private final String os_email_field;
    private final String os_givenname_field;
    private final String os_surname_field;
    private final String os_phone_field;
            

    /**
     * Constructor allows injection of 
     * configuration parameters.
     *
     * @param s_provider_url to the server - we assume simple authentication
     * @param s_id_field attribute of user object - usually cn
     * @param s_search_context subtree under which to search for user info,
     *                          ex: ou=People,dc=myschool,dc=edu
     * @param s_object_context of user bind-path - 
     *         ex: ou=People,dc=myschool,dc=edu leads to bind attempt
     *         againt cn=username,ou=People,dc=myschool,dc=edu
     * @param s_email_field in user record
     * @param s_givenname_field in user record
     * @param s_surname_field in user record, usually sn
     * @param s_phone_field in user record
     */
    public DefaultSpeakerToLDAP( String s_provider_url,
                            String s_id_field,
                            String s_search_context,
                            String s_object_context,
                            String s_email_field,
                            String s_givenname_field,
                            String s_surname_field,
                            String s_phone_field
                            ) 
    {
        os_provider_url = s_provider_url;
        os_id_field = s_id_field;
        os_search_context = s_search_context;
        os_object_context = s_object_context;
        os_email_field = s_email_field;
        os_givenname_field = s_givenname_field;
        os_surname_field = s_surname_field;
        os_phone_field = s_phone_field;
    }

    /**
     * Default constructor extracts LDAP-server parameters
     * from ConfigurationManager (dspace.cfg):
     *     ldap.provider_url, ldap_id_field,
     *     ldap_search_contect, ldap_object_context
     */
    public DefaultSpeakerToLDAP() {
        os_provider_url = ConfigurationManager.getProperty("ldap.provider_url");
        os_id_field = ConfigurationManager.getProperty("ldap.id_field");
        os_search_context = ConfigurationManager.getProperty("ldap.search_context");
        os_object_context = ConfigurationManager.getProperty("ldap.object_context");
        os_email_field = ConfigurationManager.getProperty("ldap.email_field");
        os_givenname_field = ConfigurationManager.getProperty("ldap.givenname_field");
        os_surname_field = ConfigurationManager.getProperty("ldap.surname_field");
        os_phone_field = ConfigurationManager.getProperty("ldap.phone_field");
    }

    /**
     * contact the ldap server and attempt to authenticate
     */
    public DataFromLDAP ldapAuthenticate(String s_netid, String s_password ) throws NamingException
    {
        if (s_password.equals("")) 
        {
            return null;
        }
        // Set up environment for creating initial context
        Hashtable env = new Hashtable(11);
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, os_provider_url);
        
        // Authenticate
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        final String  s_ad_key = "WINDOWS_DOMAIN:";
        if ( os_object_context.toUpperCase ().startsWith( s_ad_key ) ) {
            // Active Directory bind
            String s_principal = os_object_context.substring( s_ad_key.length () ) + "\\" + s_netid;
            olog.debug( "Binding principal to: " + s_principal );
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, s_principal );
        } else {
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, os_id_field+"="+s_netid+","+os_object_context);
        }
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, s_password);
        
        DirContext ctx = new InitialDirContext(env);
        try {
            Attributes search_attributes = new BasicAttributes(true);
            search_attributes.put(new BasicAttribute(os_id_field, s_netid));
            
            String[] v_result_atts = {os_email_field, os_givenname_field, os_surname_field, os_phone_field};
            
            olog.debug( "Searching LDAP for " + os_id_field + "=" + s_netid 
                        + " under " + os_search_context
                        );

            NamingEnumeration answer = ctx.search(os_search_context, 
                                                  "(" + os_id_field + "=" + s_netid + ")",
                                                  new SearchControls( SearchControls.SUBTREE_SCOPE,
                                                                      1, 20000,
                                                                      v_result_atts,
                                                                      false, false 
                                                                      )
                                                  );
            if( ! answer.hasMore()) {
                olog.info( "Able to bind as " + s_netid + ", but unable to find LDAP record" );
                return null;
            }
            // look up attributes
            String ldapEmail = null;
            String ldapGivenName = null;
            String ldapSurname = null;
            String ldapPhone = null;
            SearchResult sr = (SearchResult)answer.next();
            Attributes atts = sr.getAttributes();
            Attribute att;
            
            if (v_result_atts[0]!=null) {
                att = atts.get(v_result_atts[0]);
                if (att != null) ldapEmail = (String)att.get();
            }
            
            if (v_result_atts[1]!=null) {
                att = atts.get(v_result_atts[1]);
                if (att != null) {
                    ldapGivenName = (String)att.get();
                }
            }
            
            if (v_result_atts[2]!=null) {
                att = atts.get(v_result_atts[2]);
                if (att != null) {
                    ldapSurname = (String)att.get();
                }
            }
            
            if (v_result_atts[3]!=null) {
                att = atts.get(v_result_atts[3]);
                if (att != null) {
                    ldapPhone = (String)att.get();
                }
            }
            return new SimpleDataFromLDAP( ldapEmail, ldapGivenName, ldapSurname, ldapPhone, s_netid );
        } finally {
            // Close the context when we're done
            try {
                if (ctx != null) {
                    ctx.close();
                }
            } catch (NamingException e) { }
        }
        // Unreachable
    }
}
