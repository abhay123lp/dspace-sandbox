package org.dspace.sword;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.authorize.AuthorizeException;

import org.purl.sword.server.SWORDServer;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.Deposit;

import java.sql.SQLException;

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
		
	// methods required by SWORDServer interface
	////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doServiceDocument(org.purl.sword.base.ServiceDocumentRequest)
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException
	{
		if (log.isDebugEnabled())
		{
			log.debug(LogManager.getHeader(context, "sword_do_service_document", ""));
		}
		
		try
		{
			// first authenticate the request
			// note: this will build our context for us
			this.authenticate(request);
			
			// log the request
			log.info(LogManager.getHeader(context, "sword_service_document_request", "username=" + request.getUsername() + ",on_behalf_of=" + request.getOnBehalfOf()));
			
			// prep the service request, then get the service document out of it
			SWORDService service = new SWORDService();
			service.setContext(context);
			ServiceDocument doc = service.getServiceDocument();
			
			return doc;
		}
		catch (DSpaceSWORDException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("The DSpace SWORD interface experienced an error", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doSWORDDeposit(org.purl.sword.server.Deposit)
	 */
	public DepositResponse doDeposit(Deposit deposit)
		throws SWORDAuthenticationException, SWORDException
	{
		if (log.isDebugEnabled())
		{
			log.debug(LogManager.getHeader(context, "sword_do_deposit", ""));
		}
		
		// first authenticate the request
		// note: this will build our context for us
		this.authenticate(deposit);
		
		// log the request
		log.info(LogManager.getHeader(context, "sword_deposit_request", "username=" + deposit.getUsername() + ",on_behalf_of=" + deposit.getOnBehalfOf()));
		
		DepositManager dm = new DepositManager();
		dm.setContext(context);
		dm.setDeposit(deposit);
		DepositResponse response = dm.deposit();
		
		return response;
	}
	
	// internal methods
	///////////////////
	
	private void constructContext(String ip)
	{
		try
		{
			this.context = new Context();
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
		}
		
		// Set the session ID and IP address
        this.context.setExtraLogInfo("session_id=0:ip_addr=" + ip);
	}
	
	private void authenticate(ServiceDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException
	{
		this.constructContext(request.getIPAddress());
		this.authenticate(request.getUsername(), request.getPassword(), request.getOnBehalfOf());
	}
	
	private void authenticate(Deposit deposit)
		throws SWORDAuthenticationException, SWORDException
	{
		this.constructContext(deposit.getIPAddress());
		this.authenticate(deposit.getUsername(), deposit.getPassword(), deposit.getOnBehalfOf());
	}
	
	private void authenticate(String un, String pw, String obo)
		throws SWORDAuthenticationException, SWORDException
	{
		log.info(LogManager.getHeader(context, "sword_authenticate", "username=" + un + "on_behalf_of=" + obo));
		try
		{
			// attempt to authenticate the primary user
			SWORDAuthentication auth = new SWORDAuthentication();
			EPerson ep = null;
			if (auth.authenticates(this.context, un, pw))
			{
				if (obo != null)
				{
					ep = EPerson.findByEmail(this.context, obo);
				}
				else
				{
					ep = EPerson.findByEmail(this.context, un);
				}
			}
			
			// deal with the context or throw an authentication exception
			if (ep != null)
			{
				this.context.setCurrentUser(ep);
				log.info(LogManager.getHeader(context, "sword_set_authenticated_user", "user_id=" + ep.getID()));
			}
			else
			{
				log.info(LogManager.getHeader(context, "sword_unable_to_set_user", "username=" + un + ",on_behalf_of=" + obo));
				throw new SWORDAuthenticationException("Unable to authenticate the supplied used or onBehalfOf account");
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem accessing the repository user database", e);
		}
		catch (AuthorizeException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDAuthenticationException("There was a problem authenticating or authorising the user", e);
		}
	}
}
