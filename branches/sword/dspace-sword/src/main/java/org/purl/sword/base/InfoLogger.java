package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

/**
 * 
 */
public class InfoLogger
{
	/** 
	 * Single instance of the InfoLogger that can be used by all 
	 * calling classes.
	 */
   private static InfoLogger logger = null; 
   
   /**
    * Returns the single instance of this class. If this is the first call, 
    * the logger is created on this call. 
    * 
    * @return The InfoLogger. 
    */
   public static InfoLogger getLogger()
   {
      if( logger == null )
      {
         logger = new InfoLogger();
      }
      return logger;
   }
   
   public void writeError(String message)
   {
      System.err.println("Error: " + message );
   }
   
   public void writeInfo(String message)
   {
      System.err.println("Info: " + message );
   }
   
   public void writeWarning(String message)
   {
      System.err.println("Warning: " + message );
   }
}
