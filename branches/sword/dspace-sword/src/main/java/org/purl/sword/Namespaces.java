/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */
package org.purl.sword;

/**
 * List of the namespaces that are used by SWORD. 
 * 
 * Last updated on: $Date: 2007/07/25 07:42:30 $
 * 
 * @author Neil Taylor
 * @version $Revision: 1.1 $
 *
 */
public interface Namespaces {

	/**
	 * Atom Publishing Protocol (APP) Namespace. 
	 */
	public static final String NS_APP = "http://purl.org/atom/app#";
	
	/**
	 * ATOM Namespace.
	 */
	public static final String NS_ATOM = "http://www.w3.org/2005/Atom";
	
	/**
	 * Sword Namespace. 
	 */
	public static final String NS_SWORD = "http://purl.org/net/sword/";
		  
   /**
    * DC Terms Namespace.
    */
	public static final String NS_DC_TERMS = "http://purl.org/dc/terms/";
	
}
