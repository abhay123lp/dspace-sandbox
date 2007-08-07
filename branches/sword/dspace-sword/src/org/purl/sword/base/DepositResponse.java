package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

   public DepositResponse() 
   {
      entry = new SWORDEntry();  
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
   
   /*public String toString() {
      StringBuffer response = new StringBuffer();
      response.append("<?xml version=\"1.0\"?>");
      response.append("<entry xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
                       "xmlns:sword=\"http://purl.org/sword/\" " +
                       "xmlns:dcterms=\"http://purl.org/dc/terms/\">");
      if (title != null) {
         response.append("<title>" + title + "</title>");
      }
      if (id != null) {
         response.append("<id>" + id + "</id>");
      }
      if (updated != null) {
         response.append("<updated>" + updated + "</updated>");
      }
      if (author != null) {
         response.append("<author><name>" + author + "</name></author>");
      }
      if (contributor != null) {
         response.append("<contributor><name>" + contributor + "</name></contributor>");
      }
      if (summary != null) {
         response.append("<summary type=\"text\">" + summary + "</summary>");
      } else {
         response.append("<summary type=\"text\" />");
      }
      response.append("<content type=\"" + contentType + "\" " +
                        "src=\"" + contentSource + "\"/>");
      response.append("<link rel=\"edit-media\" " +
                        "href=\"" + editMediaLink + "\" />");
      response.append("<link rel=\"edit\" " +
                        "href=\"" + editLink + "\" />");
      if ((generatorText != null) || (generatorVersion != null) || (generatorURI != null)) {
         response.append("<source>");
         response.append("<generator");
         if (generatorVersion != null) {
            response.append(" version=\"" + generatorVersion + "\"");
         }
         if (generatorURI != null) {
            response.append(" version=\"" + generatorURI + "\"");
         }
         response.append(">");
         if (generatorText != null) {
            response.append(generatorText);
         }
         response.append("</generator>");
         response.append("</source>");
      } else {
         response.append("<source />");
      }
      if (treatment != null) {
         response.append("<sword:treatment>" + treatment + "</sword:treatment>");
      }
      if (verboseDescription != null) {
         response.append("<sword:verboseDescription>" + verboseDescription + "</sword:verboseDescription>");
      }
      if (noOpStatus != null) {
         response.append("<sword:noOp>" + noOpStatus + "</sword:noOp>");
      }
      if (formatNamespace != null) {
         response.append("<sword:formatNamespace>" + formatNamespace + "</sword:formatNamespace>");
      }
      response.append("</entry>");
      
      return response.toString();
   }*/
}
