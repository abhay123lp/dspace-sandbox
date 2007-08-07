package org.w3.atom;

/**
 * An invalid media type has been detected during parsing. 
 * 
 * @author Neil Taylor
 */
public class InvalidMediaTypeException extends Exception
{
   /**
    * Create a new instance and store the message. 
    * 
    * @param message The exception's message. 
    */
   public InvalidMediaTypeException( String message )
   {
      super(message);
   }
}
