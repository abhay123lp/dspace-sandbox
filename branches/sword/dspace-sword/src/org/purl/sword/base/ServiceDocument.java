package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import nu.xom.Builder; 
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;


/**
 * A representation of a SWORD Service Document.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class ServiceDocument 
{
   /** 
    * The Service object that is held by this object. 
    */
   private Service service; 
   
   /**
    * Create a new instance and set the initial service level to Zero. 
    */
   public ServiceDocument()
   {
      this(ServiceLevel.ZERO);   
   }
   
   /**
    * Create a new instance and set the specified service level. 
    * 
    * @param complianceLevel The service compliance level. 
    */
   public ServiceDocument(ServiceLevel complianceLevel)
   {
      service = new Service(complianceLevel);   
   }
   
   /**
    * Create a new instance and store the specified Service document. 
    * 
    * @param service The Service object.
    */
   public ServiceDocument(Service service)
   {
      this.service = service; 
   }

   /**
    * Instantiate a ServiceDocument
    * 
    * @param complianceLevel The compliance level of this implementation
    * @param noOp Whether or not the NOOP option is available
    * @param verbose Whether or not the verbose option is available
    * @param workspaceTitle The name of the workspace title
    * @param workspaceCollections A Collection of workspaces
    * 
    * @deprecated Please use the other constructors.  
    */
   public ServiceDocument(ServiceLevel complianceLevel,
                          boolean noOp, 
                          boolean verbose, 
                          String workspaceTitle,
                          Collection workspaceCollections) {
      
      service = new Service( complianceLevel, noOp, verbose );
      Workspace workspace = new Workspace( workspaceTitle );
      workspace.addCollection(workspaceCollections); // FIXME - not quite right?
      service.addWorkspace(workspace);
   }
   
   /**
    * Get the compliance level from this Service Document
    * 
    * @return The compliance level
    * 
    * @deprecated Please access the compliance level directly from the service. 
    */
   public ServiceLevel getComplianceLevel() {
      // Return the compliance level
      return service.getComplianceLevel();
   }
   
   /**
    * Returns a boolean depending on whether or not the Service Document
    * says the server supports the NOOP option
    * 
    * @return The NOOP option status
    * 
    * @deprecated Please access the value directly from the service.
    */
   public boolean supportsNoOp() {
      // Return the NOOP option status
      return service.isNoOp();
   }
   
   /**
    * 
    * @param noOp
    * 
    * @deprecated Please access the value directly from the service.
    */
   public void setNoOp(boolean noOp)
   {
      service.setNoOp(noOp);
   }
   
   /**
    * Returns a boolean depending on whether or not the Service Document
    * says the server supports the verbose option
    * 
    * @return The verbose option status
    * 
    * @deprecated Please access the value directly from the service.
    */
   public boolean supportsVerbose() {
      // Return the verbose option status
      return service.isVerbose();
   }
   
   /**
    * 
    * @param verbose
    * 
    * @deprecated Please access the value directly from the service.
    */
   public void setVerbose(boolean verbose)
   {
      service.setVerbose(verbose);
   }
   
   /**
    * Returns the Collectinos in the workspace described by the Service Document
    * 
    * @return The workspaces
    * 
    * @deprecated Please access the value directly from the service.
    */
   public Iterator<Collection> getWorkspaceCollections() {
      // Return the collections
      return null; // FIXME service.getWorkspaces().collectionIterator();
   }
   
   /**
    * @deprecated Please access the value directly from the service.
    */
   public Iterator<Workspace> getWorkspaces() 
   {
	   return service.getWorkspaces();
   }
   
   /**
    * 
    * @param workspace
    * 
    * @deprecated Please access the value directly from the service.
    */
   public void addWorkspace(Workspace workspace)
   {
	   service.addWorkspace(workspace);
   }
   
   /**
    * Set the service object associated with this document. 
    * 
    * @param service The new Service object. 
    */
   public void setService(Service service)
   {
      this.service = service; 
   }
   
   /**
    * Retrieve the Service object associated with this document.  
    * 
    * @return The Service object. 
    */
   public Service getService()
   {
      return service; 
   }
   
   /**
    * Return the Service Document in it's XML form.
    * 
    * @return The ServiceDocument
    */
   public String toString() 
   {
      return marshall();
   }  
   
   /**
    * Marshall the data in the Service element and generate a String representation. 
    * The returned string is UTF-8 format. 
    * 
    * @return A string of XML, or <code>null</code> if there was an error
    *         marshalling the data. 
    */
   public String marshall( )
   {
      try 
      {
         ByteArrayOutputStream stream = new ByteArrayOutputStream();
         Serializer serializer = new Serializer(stream, "UTF-8");
         serializer.setIndent(3);
         serializer.setMaxLength(64);
         
         Document doc = new Document(service.marshall());
         serializer.write(doc);  
         
         return stream.toString();
       }
       catch (IOException ex) {
          System.err.println(ex); 
       }
       
       return null;
   }
   
   /**
    * Convert the specified XML string into a set of objects
    * used within the service. A new Service object will be 
    * created and stored. This will dispose of any previous
    * Service object associated with this object. 
    * 
    * @param xml The XML string. 
    * @throws UnmarshallException If there was a problem unmarshalling the 
    *                             data. This might be as a result of an 
    *                             error in parsing the XML string, 
    *                             extracting information. 
    */
   public void unmarshall( String xml )
   throws UnmarshallException
   {
      //
      try
      {
         Builder builder = new Builder(); 
         Document doc = builder.build(xml, "http://something.com/here");
         Element root = doc.getRootElement(); 
         
         service = new Service( );
         service.unmarshall(root);
         
      }
      catch( ParsingException ex )
      {
         throw new UnmarshallException("Unable to parse the XML", ex );
      }
      catch( IOException ex )
      {
         throw new UnmarshallException("Error acessing the file?", ex);

      }
   }
}