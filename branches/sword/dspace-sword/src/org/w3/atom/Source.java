package org.w3.atom;

import org.purl.sword.Namespaces;
import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

import nu.xom.Element; 
import nu.xom.Elements;

/**
 * Represents an ATOM Generator element. 
 * 
 * @author Neil Taylor
 */
public class Source extends XmlElement implements SwordElementInterface
{
   /**
    * The generator data for this object. 
    */
   private Generator generator; 
   
   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'source'.  
    */
   public Source()
   {
      super("atom", "source");   
   }
   
   /**
    * Marshall the data stored in this object into Element objects. 
    * 
    * @return An element that holds the data associated with this object. 
    */
   public Element marshall()
   {
      Element source = new Element(getQualifiedName(), Namespaces.NS_ATOM);
      
      if( generator != null )
      {
         source.appendChild(generator.marshall());
      }
      
      return source;
   }
   
   /**
    * Unmarshall the contents of the source element into the internal data objects
    * in this object. 
    * 
    * @param source The Source element to process. 
    *
    * @throws UnmarshallException If the element does not contain an ATOM Source
    *         element, or if there is a problem processing the element or any 
    *         sub-elements. 
    */
   public void unmarshall(Element source) 
   throws UnmarshallException 
   {
      if( ! isInstanceOf(source, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException( "Not an atom:source element" );
      }
      
      try
      {
         // retrieve all of the sub-elements
         Elements elements = source.getChildElements();
         Element element = null; 
         int length = elements.size();

         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            if( isInstanceOf(element, "generator", Namespaces.NS_ATOM ) )
            {
               generator = new Generator(); 
               generator.unmarshall(element);
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in Source: " + ex.getMessage());
         ex.printStackTrace();
         throw new UnmarshallException("Unable to parse an element in Source", ex);
      }
   }

   /**
    * Get the generator. 
    * 
    * @return The generator. 
    */
   public Generator getGenerator()
   {
      return generator;
   }

   /**
    * Set the generator. 
    * 
    * @param generator The generator. 
    */
   public void setGenerator(Generator generator)
   {
      this.generator = generator;
   }
}
