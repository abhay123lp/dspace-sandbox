package org.w3.atom;

import nu.xom.Element;
import nu.xom.Elements;

import org.purl.sword.Namespaces;
import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement; 
import org.purl.sword.base.SwordElementInterface;

/**
 * Represents an Author type, as used in ATOM. This class is used as the 
 * base class for the different areas of ATOM that represent information 
 * about people. This includes the atom:author and atom:contributor
 * elements. 
 * 
 * @author Neil Taylor
 */
public class Author extends XmlElement implements SwordElementInterface
{
   /**
    * The author's name. 
    */
   private String name; 
   
   /**
    * The author's URI.  
    */
   private String uri;
   
   /**
    * The author's email. 
    */
   private String email; 
   
   
   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'author'.  
    */
   public Author()
   {
      this("atom", "author");   
   }
   
   /**
    * Create a new instance and set the element name. 
    * 
    * @param prefix The prefix to use when marshalling the data. 
    * @param localName The localName to use when marshalling the data. 
    */
   public Author(String prefix, String localName )
   {
      super(prefix, localName);
   }
      
   /**
    * Marshall the data in this object to a XOM Element. The element
    * will have the full name that is specified in the constructor. 
    * 
    * @return A XOM Element. 
    */
   public Element marshall()
   {
	   Element element = new Element(getQualifiedName(), Namespaces.NS_ATOM);
	         
	   if( name != null )
	   {
	      Element nameElement = new Element(getQualifiedName("name"), Namespaces.NS_ATOM);
	      nameElement.appendChild(name);
	      element.appendChild(nameElement);
	   }
	   
	   if( uri != null )
	   {
	      Element uriElement = new Element(getQualifiedName("uri"), Namespaces.NS_ATOM);
	      uriElement.appendChild(uri);
	      element.appendChild(uriElement);
	   }
	   
	   if( email != null )
	   {
	      Element emailElement = new Element(getQualifiedName("email"), Namespaces.NS_ATOM);
	      emailElement.appendChild(email);
	      element.appendChild(emailElement);
	   }
	   
	   return element;
   }
   

   /**
    * Unmarshall the author details from the specified element. The element 
    * is a XOM element. 
    * 
    */
   public void unmarshall(Element author)
   throws UnmarshallException
   {
      if( ! isInstanceOf( author, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException("Element is not of the correct type");
      }
      
	  try
	  {
		 // retrieve all of the sub-elements
		 Elements elements = author.getChildElements();
		 Element element = null; 
		 int length = elements.size();

		 for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            
    		if( isInstanceOf(element, "name", Namespaces.NS_ATOM ))
			{
			    name = unmarshallString(element);
			}
    		if( isInstanceOf(element, "uri", Namespaces.NS_ATOM ))
			{
			    uri = unmarshallString(element);
			}
    		if( isInstanceOf(element, "email", Namespaces.NS_ATOM ))
			{
			    email = unmarshallString(element);
			}
			else
			{
			   // unknown element type
			   //counter.other++; 
			}
         } // for 
	  }
	  catch( UnmarshallException ex )
	  {
         InfoLogger.getLogger().writeError("Unable to parse an element in " + getQualifiedName() + ": " + ex.getMessage());
         throw ex;
	  }
      
   }

   /**
    * Retrieve the author name. 
    * 
    * @return The name. 
    */
   public String getName() 
   {
	  return name;
   }

   /**
    * Set the author name. 
    * 
    * @param name The name. 
    */
   public void setName(String name) 
   {
	  this.name = name;
   }

   /**
    * Get the author URI. 
    * 
    * @return The URI. 
    */
   public String getUri() 
   {
	  return uri;
   }

   /**
    * Set the author URI. 
    * 
    * @param uri the URI. 
    */
   public void setUri(String uri) 
   {
	  this.uri = uri;
   }

   /**
    * Get the author email. 
    * 
    * @return The email. 
    */
   public String getEmail() 
   {
	  return email;
   }

   /**
    * Set the author email. 
    * 
    * @param email The email. 
    */
   public void setEmail(String email) 
   {
	  this.email = email;
   } 

}
