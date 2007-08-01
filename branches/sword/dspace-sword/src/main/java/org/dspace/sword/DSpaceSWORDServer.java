package org.dspace.sword;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.purl.sword.SWORDServer;
import org.purl.sword.base.SWORDDeposit;
import org.purl.sword.base.SWORDDepositResponse;
import org.purl.sword.base.ServiceDocument;

/**
 * An implementation of the SWORDServer interface to allow SWORD deposit
 * operations on DSpace.  See:
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Richard Jones
 */
public class DSpaceSWORDServer implements SWORDServer
{
	public static Logger log = Logger.getLogger(DSpaceSWORDServer.class);
	
	private Context context;
	
	// my very own constructor
	//////////////////////////
	
	public DSpaceSWORDServer()
	{
		// We may need this if we are not getting the context from the servlet
//		try
//		{
//			this.context = new Context();
//		}
//		catch (SQLException e)
//		{
//			log.error("caught exception: ", e);
//		}
	}
	
	// methods custom to the DSpace implementation of SWORDServer
	/////////////////////////////////////////////////////////////
	
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	// methods required by SWORDServer interface
	////////////////////////////////////////////
	
	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#authenticates(java.lang.String, java.lang.String)
	 */
	public boolean authenticates(String username, String password)
	{
		SWORDAuthentication auth = new SWORDAuthentication();
		boolean result = auth.authenticates(context, username, password);
		log.info(LogManager.getHeader(context, "sword_authentication", "username=" + username + ",authenticated=" + Boolean.toString(result)));
		return result;
	}

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doServiceDocument(java.lang.String, java.lang.String)
	 */
	public ServiceDocument doServiceDocument(String username, String onBehalfOf)
	{
		log.info(LogManager.getHeader(context, "sword_service_document_request", "username=" + username + ",on_behalf_of=" + onBehalfOf));
		SWORDService service = new SWORDService();
		service.setContext(context);
		service.setUsername(username);
		service.setOnBehalfOf(onBehalfOf);
		ServiceDocument doc = service.getServiceDocument();
		return doc;
	}

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doServiceDocument(java.lang.String)
	 */
	public ServiceDocument doServiceDocument(String username)
	{
		log.info(LogManager.getHeader(context, "sword_service_document_request", "username=" + username));
		SWORDService service = new SWORDService();
		service.setContext(context);
		service.setUsername(username);
		ServiceDocument doc = service.getServiceDocument();
		return doc;
	}

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doSWORDDeposit(java.lang.String, org.purl.sword.base.SWORDDeposit)
	 */
	public SWORDDepositResponse doSWORDDeposit(String username, SWORDDeposit deposit)
	{
		log.info(LogManager.getHeader(context, "sword_deposit_request", "username=" + username + ",deposit_id=" + deposit.getDepositID()));
		DepositManager dm = new DepositManager();
		dm.setContext(context);
		dm.setUsername(username);
		dm.setDeposit(deposit);
		SWORDDepositResponse resp = dm.deposit();
		log.info(LogManager.getHeader(context, "sword_deposit", "username=" + username + ", id=" + resp.getId()));
		return resp;
	}
	
}
