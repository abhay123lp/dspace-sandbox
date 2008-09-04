package org.dspace.authenticate;


import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * Generic test runner for SpeakerToLDAP implementations.
 */
public class SpeakerToLDAPCase extends TestCase {
    private static final Logger olog = Logger.getLogger( SpeakerToLDAPCase.class );

    private SpeakerToLDAP   oldap;
    private String          os_netid;
    private String          os_password;


    /**
     * Inject the test dependencies - initializes test properties
     *
     * @param s_name of test - pass to super
     * @param ldap instance to authenticate agains
     * @param s_netid user-id to authenticate as
     * @param s_password for s_netid
     */
    public SpeakerToLDAPCase ( String s_name, SpeakerToLDAP ldap,
                               String s_netid, String s_password
                               ) {
        super( s_name );
        oldap = ldap;
        os_netid = s_netid;
        os_password = s_password;
    }

    /**
     * Try to authenticate against the constructor-supplied
     * (SpeakerToLDAP, s_netid, s_password)
     */
    public void testAuthenticate() {
        try {
            DataFromLDAP ldap_info = oldap.ldapAuthenticate( os_netid, os_password );
            assertTrue( "Test user logged in ok: " + os_netid,
                        null != ldap_info
                        );
            // need e-mail to key into eperson database
            olog.info( "Got e-mail for " + os_netid + ": " + ldap_info.getEmail () );
            assertTrue( "Got e-mail info for " + os_netid + " from ldap",
                        null != ldap_info.getEmail ()
                        );
        } catch ( Exception e ) {
            olog.info( "Failed to authenticate user: " + os_netid, e );
            assertTrue( "Failed to authenticate user: " + os_netid + ", caught: " + e, false );
        }
    }
}
