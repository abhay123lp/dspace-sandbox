package org.w3.atom;

import org.w3.atom.Author;

/**
 * Represents an ATOM Contributor. 
 * 
 * @author Neil Taylor
 */
public class Contributor extends Author
{
   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'contributor'. 
    */
   public Contributor()
   {
      super("atom", "contributor");
   }
}
