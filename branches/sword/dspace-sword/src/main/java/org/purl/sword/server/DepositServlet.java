package org.purl.sword.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;

public class DepositServlet extends HttpServlet {
	
	private SWORDServer myRepository;
	
	private String authN;
	
	private int maxMemorySize;
	
	private File tempDirectory;
	
	private int maxRequestSize;
	
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
		if ((authN == null) || (authN.equals(""))) {
			authN = "None";
		}
		log.info("Authentication type set to: " + authN);
		
		String temp = getServletContext().getInitParameter("upload-max-memory-size");
		if ((temp == null) || (temp.equals(""))) {
			maxMemorySize = 10;
		} else {
			maxMemorySize = Integer.parseInt(temp);
		}
		log.info("Upload max size to store in memory: " + maxMemorySize + "KB");
		
		String tempDir = getServletContext().getInitParameter("upload-temp-directory");
		if ((tempDir == null) || (tempDir.equals(""))) {
			tempDir = System.getProperty("java.io.tmpdir");
		}
		tempDirectory = new File(tempDir);
		log.info("Upload temporary directory set to: " + tempDir);
		if (!tempDirectory.isDirectory()) {
			log.fatal("Upload temporary directory is not a directory: " + tempDir);
		}
		if (!tempDirectory.canWrite()) {
			log.fatal("Upload temporary directory cannot be written to: " + tempDir);
		}
		
		temp = getServletContext().getInitParameter("upload-max-memory-size");
		if ((temp == null) || (temp.equals(""))) {
			maxMemorySize = 10;
		} else {
			maxMemorySize = Integer.parseInt(temp);
		}
		log.info("Upload max size to store in memory: " + maxMemorySize + "KB");	
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
		Date date = new Date();
    	log.debug("Starting deposit processing at " + date.toString() + " by " + request.getRemoteAddr());
    	
		// Are there any authentcation details?
		String usernamePassword = getUsernamePassword(request);
		if ((usernamePassword != null) && (!usernamePassword.equals(""))) {
			int p = usernamePassword.indexOf(":");
			if (p != -1) {
				d.setUsername(usernamePassword.substring(0, p));
				d.setPassword(usernamePassword.substring(p+1));
			} 
        } else if (authenticateWithBasic()) {
			String s = "Basic realm=\"SWORD\"";
	    	response.setHeader("WWW-Authenticate", s);
	    	response.setStatus(401);
	    	log.debug("No authentication credentials given. Asking for some");
		}
		 
		// Do the processing
		try {
			// Check the MD5 hash
			String receivedMD5 = ChecksumUtils.generateMD5(request.getInputStream());
			log.debug("Received filechecksum: " + receivedMD5);
			d.setMd5(receivedMD5);
			String md5 = request.getHeader("Content-MD5"); 
			log.debug("Received file checksum header: " + md5);
			if ((md5 != null) && (!md5.equals(receivedMD5))) {
				response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
				response.setHeader(HttpHeaders.X_ERROR_CODE, "ErrorChecksumMismatch");
				log.debug("Bad MD5 for file. Aborting with appropriate error message");
			} else {
				// Set the file
				d.setFile(request.getInputStream());
				
				// Set the X-On-Behalf-Of header
				d.setOnBehalfOf(request.getHeader(HttpHeaders.X_ON_BEHALF_OF.toString()));
				
				// Set the X-Format-Namespace header
				d.setFormatNamespace(request.getHeader(HttpHeaders.X_FORMAT_NAMESPACE));
	
				// Set the X-No-Op header
				String noop = request.getHeader(HttpHeaders.X_NO_OP);
				if ((noop != null) && (noop.equals("true"))) {
					d.setNoOp(true);
				} else {
					d.setNoOp(false);
				}
	
				// Set the X-Verbose header
				String verbose = request.getHeader(HttpHeaders.X_VERBOSE);
				if ((verbose != null) && (verbose.equals("true"))) {
					d.setVerbose(true);
				} else {
					d.setVerbose(false);
				}
				
				// Set the IP address
				d.setIPAddress(request.getRemoteAddr());
				
		        // Get the DepositResponse
				DepositResponse dr = myRepository.doDeposit(d);
				
				// Print out the Deposit Response
				// response.setContentType("application/atomserv+xml");
				response.setContentType("application/xml");
				PrintWriter out = response.getWriter();
		        out.write(dr.marshall());
			}
		} catch (SWORDAuthenticationException sae) {
			if (authN.equals("Basic")) {
		    	String s = "Basic realm=\"SWORD\"";
		    	response.setHeader("WWW-Authenticate", s);
		    	response.setStatus(401);
			}
		} catch (SWORDException se) {
			// Throw a HTTP 500
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			
			// Is there an appropriate error header to return?
			if (se.getErrorCode() != null) {
				response.setHeader(HttpHeaders.X_ERROR_CODE, se.getErrorCode());
			}
			System.out.println(se.toString());
			log.error(se.toString());
		} catch (IOException ioe) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(ioe.toString());
		} catch (NoSuchAlgorithmException nsae) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(nsae.toString());
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
