package org.purl.sword;

import org.purl.sword.base.SWORDDeposit;
import org.purl.sword.base.SWORDDepositResponse;
import org.purl.sword.base.ServiceDocument;

/**
 * An abstract interface to be implemnted by repositories wishing to provide
 * a SWORD compliant service.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Stuart Lewis
 */
public interface SWORDServer {
	
	/**
	 * Answer a Service Document request
	 * 
	 * @param username The username of the authenticated user (or null if not authenticated)
	 * 
	 * @return The ServiceDocument representing the service document
	 */
	public ServiceDocument doServiceDocument(String username);
	
	/**
	 * Answer a Service Document request sent on behalf of a user
	 * 
	 * @param username The username of the authenticated user (or null if not authenticated)
	 * @param onBehalfOf The value of the X-On-Behalf-Of header
	 *
	 * @return The ServiceDocument representing the service document
	 */
	public ServiceDocument doServiceDocument(String username, String onBehalfOf);
	
	/**
	 * Answer a SWORD deposit
	 * 
	 * @param username The username of the authenticated user (or null if not authenticated)
	 * @param deposit The SWORDDeposit item
	 * 
	 * @return The response to the deposit
	 */
	public SWORDDepositResponse doSWORDDeposit(String username, SWORDDeposit deposit);
	
	/**
	 * Authenticate a user
	 * 
	 * @param usenrame The username to authenticate
	 * @param password The password to autheenticate with
	 * 
	 * @return Whether or not the user credentials authenticate
	 */
	public boolean authenticates(String username, String password);
}
