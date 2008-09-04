package org.dspace.authenticate;


/**
 * POJO data bucket interface for user-data obtained during
 * LDAP authentication
 */
public interface DataFromLDAP {
    public String getEmail ();
    public String getGivenName ();
    public String getSurname();
    public String getPhone ();
    /** LDAP common name (cn) for the user */
    public String getNetId ();
}
