package org.dspace.authenticate;


/**
 * Simple implementation of DataFromLDAP
 */
public class SimpleDataFromLDAP implements DataFromLDAP {
    /**
     * Constructor injects all the property values
     */
    public SimpleDataFromLDAP ( String s_email,
                          String s_given_name,
                          String s_surname,
                          String s_phone,
                          String s_netid
                          ) {
        os_email = s_email;
        os_given_name = s_given_name;
        os_surname = s_surname;
        os_phone = s_phone;
        os_netid = s_netid;
    }

    private final String os_email;
    public String getEmail () {
        return os_email;
    }

    private final String os_given_name;
    public String getGivenName () {
        return os_given_name;
    }

    private final String os_surname;
    public String getSurname() {
        return os_surname;
    }

    private final String os_phone;
    public String getPhone () {
        return os_phone;
    }

    private final String os_netid;
    public String getNetId () {
        return os_netid;
    }
}
