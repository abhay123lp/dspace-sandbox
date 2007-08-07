package org.w3.atom;

/**
 * Represents an ATOM Rights element. This is a simple subclass of the 
 * TextConstruct class. 
 * 
 * @author Neil Taylor
 */
public class Rights extends TextConstruct
{

   /** 
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'rights'. 
    */
   public Rights()
   {
      super("atom", "rights");
   }

}
