package org.w3.atom;

/**
 * 
 * @author Neil Taylor
 *
 */
public enum ContentType
{
   TEXT ("text"),
   HTML ("html"),
   XHTML ("xhtml");
   
   /**
    * String representation of the type. 
    */
   private final String type; 
   
   /**
    * Create a new instance and set the string 
    * representation of the type. 
    * 
    * @param type The type, expressed as a string. 
    */
   private ContentType(String type)
   {
      this.type = type;   
   }
   
   /**
    * Retrieve a string representation of this object.
    * 
    *  @return A string. 
    */
   public String toString() { return this.type; }
}
