package org.purl.sword.base;

/**
 * Definition of the error codes that will be used in 
 * the SWORD protocol (in X-Error-Code).
 * 
 * @author Stuart Lewis
 */
public interface ErrorCodes
{
	/**
     * ErrorContent - where the supplied format is not the same as that 
     * identified in the X-Format-Namespace and/or that supported by the
     * server
     */
	public static final String ERROR_CONTENT = "ErrorContent";
 
	/**
	 * ErrorChecksumMismatch - where the checksum of the file recevied does 
	 * not match the checksum given in the header
	 */
	public static final String ERROR_CHECKSUM_MISMATCH = "ErrorChecksumMismatch";
	
	/**
	 * ErrorBadRequest - where parameters are not understood
	 */
	public static final String ERROR_BAD_REQUEST = "ErrorBadRequest";
	
	/**
	 * TargetOwnerUnknown - where the server cannot identify the specified
	 * TargetOwner
	 */
	public static final String TARGET_OWNER_UKNOWN = "TargetOwnerUnknown";
	
	/**
	 * MediationNotAllowed - where a client has attempted a mediated deposit,
	 * but this is not supported by the server
  	 */
	public static final String MEDIATION_NOT_ALLOWED = "MediationNotAllowed";
}
