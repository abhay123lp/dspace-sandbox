package org.w3.atom;

import nu.xom.Attribute;
import nu.xom.Element;

import org.purl.sword.Namespaces;
import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;
import org.purl.sword.base.SwordElementInterface;

/**
 * Represents an ATOM Content element. 
 * 
 * @author Neil Taylor
 *
 */
public class Content extends XmlElement implements SwordElementInterface
{
   /**
    * The identifier for the src attribute. 
    */
   public static final String ATTR_SRC = "src";
   
   /**
    * The identifier for the type attribute. 
    */
   public static final String ATTR_TYPE = "type";
   
   /**
    * The data for the type attribute. 
    */
   private String type; 
   
   /**
    * The data for the source attribute. 
    */
   private String source; 

   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'content'.  
    */
   public Content()
   {
      super("atom", "content");
   }
   
   /**
    * Get the Source. 
    * 
    * @return The Source. 
    */
   public String getSource()
   {
      return source;
   }

   /**
    * Set the Source. 
    * 
    * @param source The source. 
    */
   public void setSource(String source)
   {
      this.source = source;
   }

   /**
    * Get the type. 
    * 
    * @return The type. 
    */
   public String getType()
   {
      return type;
   }

   /**
    * Set the type for the content. This should match the pattern 
    * ".* /.*" [Note, there is no space before the /, this has been added
    * to allow this text to be written in a Java comment.]. 
    * 
    * An example of the type is <code>application/zip</code>. 
    * 
    * @param type The specified type. 
    * @throws InvalidMediaTypeException If the specified type is null or
    * it does not match the specified pattern. 
    */
   public void setType(String type)
   throws InvalidMediaTypeException
   {
      if( type == null || ! type.matches(".*/.*") )
      {
         throw new InvalidMediaTypeException("Type: '" + type + "' does not match .*/.*");
      }
      
      this.type = type;
   }

   /**
    * Marshall the data in this object to an Element object.
    * 
    * @return A XOM Element that holds the data for this Content element. 
    */
   public Element marshall()
   {
      Element content = new Element(getQualifiedName(), Namespaces.NS_ATOM);
         
      if( type != null )
      {
         Attribute typeAttribute = new Attribute(ATTR_TYPE, type);
         content.addAttribute(typeAttribute);
      }
       
      if( source != null )
      {
         Attribute typeAttribute = new Attribute(ATTR_SRC, source);
         content.addAttribute(typeAttribute);
      }
      
      return content;
   }

   /**
    * Unmarshall the content element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             content element or if there are problems
    *                             accessing the data. 
    */
   public void unmarshall(Element content)
   throws UnmarshallException 
   {
      if( ! isInstanceOf( content, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException("Element is not of the correct type");
      }
      
      try
      {
         // get the attributes
         int attributeCount = content.getAttributeCount();
         Attribute attribute = null; 
         for( int i = 0; i < attributeCount; i++ )
         {
            attribute = content.getAttribute(i);
            String name = attribute.getQualifiedName();
            if( ATTR_TYPE.equals(name))
            {
                type = attribute.getValue();
            }
            
            if( ATTR_SRC.equals(name) )
            {
               source = attribute.getValue();
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in Content: " + ex.getMessage());
         throw new UnmarshallException("Error parsing Content", ex);
      }
   }

}
