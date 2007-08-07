package org.w3.atom;

/**
 * Represents an ATOM Summary element. This is a simple subclass of the 
 * TextConstruct class. 
 * 
 * @author Neil Taylor
 */
public class Summary extends TextConstruct
{
   /** 
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'summary'. 
    */
   public Summary()
   {
      super("atom", "summary");
   }
}
