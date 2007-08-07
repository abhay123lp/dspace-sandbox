package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

/**
 * Represents the SWORD Service Level. 
 * 
 * @author Neil Taylor
 *
 */
public enum ServiceLevel
{
   ZERO (0), 
   ONE (1),
   UNDEFINED (-1);
   
   /**
    * Holds the number associated with the ServiceLevel object. 
    */
   private final int number; 
   
   /**
    * Create a new ServiceLevel with the specified number. 
    * 
    * @param num The number to be associated with the service level. 
    */
   private ServiceLevel(int num)
   {
      this.number = num;   
   }
   
   /**
    * Get the number for the ServiceLevel object. 
    * 
    * @return The number. 
    */
   public int number() { return this.number; }
}
