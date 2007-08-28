package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

/**
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 *
 */
public class DepositResponse 
{
   private SWORDEntry entry; 
   
   private int httpResponse;

   public DepositResponse( int httpResponse ) 
   {
      entry = new SWORDEntry();  
      this.httpResponse = httpResponse;
   }
      
   /**
    * 
    * @param entry
    */
   public void setEntry( SWORDEntry entry )
   {
      this.entry = entry;
   }
   
   /**
    * 
    * @param entry
    */
   public SWORDEntry getEntry( )
   {
      return entry;
   }
   
   public int getHttpResponse() {
	   return httpResponse;
   }
   
   public void setHttpResponse(int httpResponse) {
	   this.httpResponse = httpResponse;
   }
   
   /**
    * 
    * @return
    */
   public String marshall( )
   {
	   try 
	   {
		   ByteArrayOutputStream stream = new ByteArrayOutputStream();
		   Serializer serializer = new Serializer(stream, "UTF-8");
		   serializer.setIndent(3);
		   serializer.setMaxLength(64);

		   if( entry != null ) 
		   {
		      Document doc = new Document(entry.marshall());
		      serializer.write(doc);  
		      System.out.println(stream.toString());
    		   return stream.toString();
		   }
	   }
	   catch (IOException ex) 
	   {
		   System.err.println(ex); 
	   }

	   return null;   // default return value. 
   }
   
   /**
    * 
    * @param xml
    * @throws UnmarshallException
    */
   public void unmarshall(String xml)
   throws UnmarshallException
   {
	   try
	   {  
		   Builder builder = new Builder(); 
		   Document doc = builder.build(xml, "http://something.com/here");
		   Element root = doc.getRootElement(); 

		   entry = new SWORDEntry( );
		   entry.unmarshall(root);
		   
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
   
   public String toString() 
   {
      return marshall();
   }  

}
