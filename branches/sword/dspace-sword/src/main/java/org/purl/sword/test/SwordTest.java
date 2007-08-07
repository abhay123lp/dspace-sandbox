package org.purl.sword.test;

import org.purl.sword.base.*;

public class SwordTest
{

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      System.out.println("There is some code in here");
      ServiceDocument document = new ServiceDocument(ServiceLevel.ZERO);
      document.setVerbose(true);
      document.setNoOp(false);
      
      Workspace workspace = new Workspace();
      workspace.setTitle("This is a test");
      
      Collection collection = new Collection(); 
      collection.setTitle("The first collection");
      collection.setLocation("http://www.somewhere.com/here");
      
      workspace.addCollection(collection);
      
      document.addWorkspace(workspace);
      
      workspace = new Workspace();
      workspace.setTitle("This is a second test");
      
      collection = new Collection(); 
      collection.setTitle("The second collection");
      collection.setLocation("http://www.somewhere.com/here/something");
      collection.addAccepts("application/zip");
      collection.addAccepts("application/xml");
      collection.setAbstract("An abstract goes in here");
      collection.setCollectionPolicy("A collection policy");
      collection.setMediation(true);
      collection.setNamespace("a namespace in here");
      collection.setTreatment("treatment in here too");
      
      
      workspace.addCollection(collection);
      
      document.addWorkspace(workspace);
      
      // display the XML document that has been constructed
      String doc = document.marshall(); 
      System.out.println(doc);
      
      try
      {
    	 ServiceDocument unmarshalledDocument = new ServiceDocument(ServiceLevel.ZERO); 
         unmarshalledDocument.unmarshall(doc);
      
         System.out.println(unmarshalledDocument.marshall());
      }
      catch( Exception e )
      {
    	  e.printStackTrace();
      }
      
   }

}
