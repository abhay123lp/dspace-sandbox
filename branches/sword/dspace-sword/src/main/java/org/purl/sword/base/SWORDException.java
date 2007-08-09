package org.purl.sword.base;

/**
 * Represents a generic SWORD exception. If this thrown by a repository,
 * it would result in a HTTP 500 message being returned to the user.
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class SWORDException extends Exception 
{
   /**
    * Create a new instance and store the specified message and source data. 
    * 
    * @param message The message for the exception. 
    * @param source  The original exception that lead to this exception. This
    *                can be <code>null</code>. 
    */
   public SWORDException(String message, Exception source)
   {  
      super(message, source);  
   }
   
   /**
    * Create a new instance and store the specified message. 
    * 
    * @param message The message for the exception. 
    */
   public SWORDException(String message)
   {
      super(message);
   }
   
}
