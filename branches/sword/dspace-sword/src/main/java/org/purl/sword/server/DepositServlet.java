package org.purl.sword.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.SWORDServer;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;

public class DepositServlet extends HttpServlet {
	
	private SWORDServer myRepository;
	
	private String authN;
	
	private static Logger log = Logger.getLogger(DepositServlet.class);
	
	public void init() {
		// Instantiate the correct SWORD Server class
		String className = getServletContext().getInitParameter("server-class");
		if (className == null) {
			log.fatal("Unable to read value of 'sword-server-class' from Servlet context");
		} else {
			try {
				myRepository = (SWORDServer)Class.forName(className).newInstance();
				log.info("Using " + className + " as the SWORDServer");
			} catch (Exception e) {
				log.fatal("Unable to instantiate class from 'sword-server-class': " + className);
			}
		}
		
		authN = getServletContext().getInitParameter("authentication-method");
		if ((authN == null) || (authN == "")) {
			authN = "None";
		}
		log.info("Authentication type set to: " + authN);
	}
	
	protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException
    {
		// Send a '501 Not Implemented'
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
   
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException
    {
    	// Create the Deposit request
		Deposit d = new Deposit();
    	
		// Are there any authentcation details?
		String usernamePassword = getUsernamePassword(request);
		if (usernamePassword != null) {
			int p = usernamePassword.indexOf(":");
			if (p != -1) {
				d.setUsername(usernamePassword.substring(0, p));
				d.setPassword(usernamePassword.substring(p+1));
			} 
        } else if (authenticateWithBasic()) {
			String s = "Basic realm=\"SWORD\"";
	    	response.setHeader("WWW-Authenticate", s);
	    	response.setStatus(401);
		}
		
		// Set the x-on-behalf-of header
		d.setOnBehalfOf(request.getHeader(HttpHeaders.X_ON_BEHALF_OF.toString()));
		
		// Set the IP address
		d.setIPAddress(request.getRemoteAddr());
		
        // Get the ServiceDocument
		try {
			DepositResponse dr = myRepository.doDeposit(d);
			
			// Print out the Service Document
			// response.setContentType("application/atomserv+xml");
			response.setContentType("application/xml");
			PrintWriter out = response.getWriter();
	        out.write(dr.marshall());
		} catch (SWORDAuthenticationException sae) {
			if (authN.equals("Basic")) {
		    	String s = "Basic realm=\"SWORD\"";
		    	response.setHeader("WWW-Authenticate", s);
		    	response.setStatus(401);
			}
		} catch (SWORDException se) {
			// Throw a HTTP 500
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
    }
	
    /**
     * Utiliy method to return the username and password (separated by a colon ':')
     * 
     * @param request
     * @return The username and password combination
     */
	private String getUsernamePassword(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                StringTokenizer st = new StringTokenizer(authHeader);
                if (st.hasMoreTokens()) {
                    String basic = st.nextToken();
                    if (basic.equalsIgnoreCase("Basic")) {
                        String credentials = st.nextToken();
                        String userPass = new String(Base64.decodeBase64(credentials.getBytes()));
                        return userPass;
                    }
                }
            }
        } catch (Exception e) {
        	log.debug(e.toString());
        }
        return null;
    }
	
	/**
	 * Utility method to deicde if we are using HTTP Basic authentication
	 * 
	 * @return if HTTP Basic authentication is in use or not
	 */
	private boolean authenticateWithBasic() {
		if (authN.equalsIgnoreCase("Basic")) {
			return true;
		} else {
			return false;
		}
	}
}
