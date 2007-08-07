package org.purl.sword.base;

/**
 * Represents a ServiceDocumentRequest. 
 * 
 * @author Stuart Lewis 
 * 
 */
public class ServiceDocumentRequest 
{
   
   private String username;
   
   private String password;
   
   private String onBehalfOf;
   
   private String  IPAddress;

   /**
    * @return the authenticatedUserName
    */
   public String getUsername() {
      return username;
   }

   /**
    * @param authenticatedUserName the authenticatedUserName to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * @return the authenticatedUserPassword
    */
   public String getPassword() {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /**
    * @return the onBehalfOf
    */
   public String getOnBehalfOf() {
      return onBehalfOf;
   }

   /**
    * @param onBehalfOf the onBehalfOf to set
    */
   public void setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
   }
   
   /**
    * Get the IP address of the user
    * 
    * @return the the IP address
    */
   public String getIPAddress() {
	   return IPAddress;
   }
   
   /**
    * Set the IP address of the user
    *
    * @param String the IP address
    */
   public void setIPAddress(String IPAddress) {
	   this.IPAddress = IPAddress;
   }
}
