package org.dspace.authenticate;

import javax.naming.NamingException;

/**
 * Interface for LDAP interaction handler
 */
public interface SpeakerToLDAP {
    /**
     * Authenticate the given user with LDAP, 
     * and get some data about him/her from the directory.
     *
     * @param s_netid cn of the user to authenticate
     * @param s_password
     * @return user info
     */
    public DataFromLDAP ldapAuthenticate( String s_netid, 
                                          String s_password
                                          ) throws NamingException;
}
