/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */
package org.purl.sword.base;

/**
 * Definition of the additional HTTP Header tags that will be used in 
 * the SWORD protocol. 
 * 
 * @author Neil Taylor
 *
 */
public interface HttpHeaders
{
  /**
   * The HTTP Header label that specifies the MD5 label. 
   */
  public static final String CONTENT_MD5 = "Content-MD5";
  
  /**
   * The HTTP Header label that specifies the On Behalf Of information.  
   */
  public static final String X_ON_BEHALF_OF = "X-On-Behalf-Of";
  
  /**
   * The HTTP Header label that specifies the Deposit ID. 
   * @deprecated
   */
  public static final String X_DEPOSIT_ID = "X-Deposit-ID";
  
  /**
   * The HTTP Header label that specifies the Format Namespace information.
   */
  public static final String X_FORMAT_NAMESPACE = "X-Format-Namespace";
  
  /**
   * The HTTP Header label that specifies the desired Verbose status. 
   */
  public static final String X_VERBOSE = "X-Verbose";
  
  /**
   * The HTTP Header label that specifies the desired NoOp status.  
   */
  public static final String X_NO_OP = "X-No-Op";
  
  /**
   * The HTTP Header that specifies the error code information. 
   */
  public static final String X_ERROR_CODE = "X-Error-Code";
  
}
