package org.dspace.sword;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.authorize.AuthorizeException;

import org.purl.sword.SWORDServer;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.server.Deposit;

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
	
	// methods required by SWORDServer interface
	////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doServiceDocument(org.purl.sword.base.ServiceDocumentRequest)
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest request)
	{
		if (log.isDebugEnabled())
		{
			log.debug(LogManager.getHeader(context, "sword_do_service_document", ""));
		}
		
		try
		{
			// first build a context for the request
			this.constructContext(request);

			// now authenticate the user
			SWORDAuthentication auth = new SWORDAuthentication();
			EPerson ep = null;
			if (auth.authenticates(this.context, request.getUserName(), request.getUserPassword()))
			{
				ep = EPerson.findByEmail(this.context, request.getUserName());
			}
			if (ep != null)
			{
				this.context.setCurrentUser(ep);
			}
			else
			{
				// FIXME: what do we do here?  Throw an exception?
			}
			
			// log the request
			log.info(LogManager.getHeader(context, "sword_service_document_request", "username=" + request.getUserName() + ",on_behalf_of=" + request.getOnBehalfOf()));
			
			// prep the service request, then get the service document out of it
			SWORDService service = new SWORDService();
			service.setContext(context);
			service.setUsername(request.getUserName());
			service.setOnBehalfOf(request.getOnBehalfOf());
			ServiceDocument doc = service.getServiceDocument();
			return doc;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
		}
		catch (AuthorizeException e)
		{
			log.error("caught exception: ", e);
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doSWORDDeposit(org.purl.sword.server.Deposit)
	 */
	public DepositResponse doSWORDDeposit(Deposit deposit)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	// internal methods
	///////////////////
	
	private void constructContext(ServiceDocumentRequest request)
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
        // this.context.setExtraLogInfo("session_id=0:ip_addr=" + request.getIP());
	}
	
}
