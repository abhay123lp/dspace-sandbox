package org.w3.atom;

/**
 * Represents an ATOM Title element. This is a simple subclass of the 
 * TextConstruct class. 
 * 
 * @author Neil Taylor
 */
public class Title extends TextConstruct
{
   /** 
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'title'.  
    */
   public Title()
   {
      super("atom", "title");
   }
}
