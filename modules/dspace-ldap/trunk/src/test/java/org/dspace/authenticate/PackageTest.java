package org.dspace.authenticate;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

/**
 * Specialization of AbstactSpeakerToLDAPTest configured
 * to run a DefaultSpeakerToLDAPTest through a test.
 */
public class PackageTest extends TestCase {
    private static final Logger olog = Logger.getLogger( PackageTest.class );


    /**
     * Build up the batch of tests to run for the org.dspace.authenticate 
     * package.  You'll have to modify the properties injected into the
     * test SpeakerToLDAP to work with your environment
     */
    public static TestSuite suite () {
        TestSuite     suite = new TestSuite( PackageTest.class.getName () );
        SpeakerToLDAP ldap = new DefaultSpeakerToLDAP ( "ldap://ldaps.university.edu",
                                                        "cn",
                                                        "OU=People,OU=AUMain,DC=auburn,DC=edu",
                                                        "WINDOWS_DOMAIN:AUBURN",
                                                        "mail",
                                                        "givenName",
                                                        "sn",
                                                        "telephoneNumber"
                                                        );
        suite.addTest ( new SpeakerToLDAPCase( "testAuthenticate", ldap,
                                               "USER", "PASSWORD"
                                               )
                        );
        return suite;
    }

}
