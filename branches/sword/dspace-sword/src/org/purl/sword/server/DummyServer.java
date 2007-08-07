package org.purl.sword.server;

import org.purl.sword.SWORDServer;
import org.purl.sword.base.Collection;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.ServiceLevel;
import org.purl.sword.base.Workspace;
import org.w3.atom.Author;
import org.w3.atom.Title;

/**
 * A 'dummy server' which acts as dumb repository which implements the
 * SWORD ServerInterface. It accepts any type of deposit, and tries to
 * return appropriate responses.
 * 
 * It supports authentication: if the username and password match
 * (case sensitive) it authenticates the user, if not, the authentication 
 * fails.
 * 
 * @author Stuart Lewis
 */
public class DummyServer implements SWORDServer {

	/** A counter to count submissions, so the response to a deposito can increment */
	private static int counter = 0;

	/**
	 * Provides a dumb but plausible service document - it contains
	 * an anonymous workspace and collection, and one personalised
	 * for the onBehalfOf user.
	 * 
	 * @param onBehalfOf The user that the client is acting on behalf of
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr) {
		// Authenticate the user
		String username = sdr.getUsername();
		String password = sdr.getPassword();
		if ((username != null) && 
		    (password != null) && 
		    (!username.equalsIgnoreCase(password))) {
				// User not authenticated
				return null;
		}
		
		// Create and return a dummy ServiceDocument
		ServiceDocument document = new ServiceDocument();
		Service service = new Service(ServiceLevel.ZERO, true, true);
	    document.setService(service);
	    
	    Workspace workspace = new Workspace();
	    workspace.setTitle("Anonymous submitters workspace");
	    Collection collection = new Collection(); 
	    collection.setTitle("Anonymous submitters collection");
	    collection.setLocation("http://" + sdr.getIPAddress() + "/anon");
	    workspace.addCollection(collection);
	    service.addWorkspace(workspace);
	     
	    if (sdr.getUsername() != null) {
	    	workspace = new Workspace();
		    workspace.setTitle("Authenticated workspace for " + username);
		    collection = new Collection(); 
		    collection.setTitle("Authenticated collection for " + username);
		    collection.setLocation("http://www.example.com/authenticated");
		    workspace.addCollection(collection);
		    service.addWorkspace(workspace);
	    }
	    
	    String onBehalfOf = sdr.getOnBehalfOf();
	    if (onBehalfOf != null) {
		    workspace = new Workspace();
		    workspace.setTitle("Personal workspace for " + onBehalfOf);
		    collection = new Collection(); 
		    collection.setTitle("Personal collection for " + onBehalfOf);
		    collection.setLocation("http://www.example.com/user");
		    collection.addAccepts("application/zip");
		    collection.addAccepts("application/xml");
		    collection.setAbstract("An abstract goes in here");
		    collection.setCollectionPolicy("A collection policy");
		    collection.setMediation(true);
		    collection.setTreatment("treatment in here too");
		    workspace.addCollection(collection);
		    service.addWorkspace(workspace);
	    }
	    
	    return document;
	}

	public DepositResponse doDeposit(Deposit deposit) {
		// Authenticate the user
		String username = deposit.getUsername();
		String password = deposit.getPassword();
		if ((username != null) && 
		    (password != null) && 
		    (!username.equalsIgnoreCase(password))) {
				// User not authenticated
				return null;
		}
		
		// Handle the deposit
		counter++;
		DepositResponse dr = new DepositResponse();
		SWORDEntry se = new SWORDEntry();
		se.addCategory("Category");
		se.setId("" + counter);
		Author a = new Author();
		if (username != null) {
			a.setName(username);
		} else {
			a.setName("unknown");
		}
		se.addAuthors(a);
		Title t = new Title();
		t.setContent("Title: " + counter);
		se.setTitle(t);
		dr.setEntry(se);
		
		return dr;
	}
}
