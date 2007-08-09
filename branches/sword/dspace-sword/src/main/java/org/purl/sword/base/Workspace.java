package org.purl.sword.base;

/**
 *   Author   : $Author$
 *   Date     : $Date$
 *   Revision : $Revision$
 *   Name     : $Name$
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.purl.sword.base.Namespaces;
import org.w3.atom.ContentType;
import org.w3.atom.Title;

import nu.xom.Element;
import nu.xom.Elements; 

/**
 * Represents an Atom Publishing Protocol Workspace element. 
 * 
 * @author Neil Taylor
 */
public class Workspace extends XmlElement implements SwordElementInterface
{
	/** 
	 * The element name that is used in the textual representatin of the XML data. 
	 */
	public static final String ELEMENT_NAME = "workspace";

	/**
	 * The title for the workspace. 
	 */
   private Title title; 
   
   /**
    * A list of collections associated with this workspace. 
    */
   private List<Collection> collections; 
   
   /**
    * Create a new instance of the workspace, with no title. 
    */
   public Workspace( ) 
   {
      this(null); 
   }
   
   /**
    * Create a new instance of the workspace with the specified title. 
    * 
    * @param title The title. 
    */
   public Workspace( String title )
   {
      super("app", ELEMENT_NAME);
      setTitle(title);
      collections = new ArrayList<Collection>();
   }
   
   /**
    * Set the title. The type for the title will be set to 
    * <code>ContentType.TEXT</code>
    * 
    * @param title The title. 
    */
   public void setTitle( String title )
   {
      if( this.title == null)
      {
         this.title = new Title();
      }
      this.title.setContent(title);
      this.title.setType(ContentType.TEXT);
      
   }
   
   /**
    * Get the content of the Title element. 
    * 
    * @return The title. 
    */
   public String getTitle( )
   {
      if( title == null ) 
      {
         return null;
      }
      
      return title.getContent(); 
   }
   
   /**
    * Add a collection to the Workspace. 
    * 
    * @param collection The collection. 
    */
   public void addCollection( Collection collection )
   {
      collections.add(collection);
   }
   
   /**
    * Get a list of collections. 
    * 
    * @return An iterator. 
    */
   public Iterator<Collection> collectionIterator( )
   {
      return collections.iterator();
   }
   
   /**
    * Marshall the data in this element to an Element. 
    * 
    * @return An element that contains the data in this object. 
    */
   public Element marshall( ) 
   {
   // convert data into XOM elements and return the 'root', i.e. the one 
      // that represents the collection. 
      Element workspace = new Element(ELEMENT_NAME, Namespaces.NS_APP);
      
      workspace.appendChild(title.marshall());
      
      for( Collection item : collections )
      {
         workspace.appendChild(item.marshall());
      }
      
      return workspace;   
   }

   /**
    * Unmarshall the workspace element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             workspace element or if there are problems
    *                             accessing the data. 
    */
   public void unmarshall( Element workspace )
   throws UnmarshallException 
   {
      if( ! isInstanceOf(workspace, ELEMENT_NAME, Namespaces.NS_APP))
      {
         throw new UnmarshallException( "Not an app:workspace element" );
      }
      
      try
      {
         collections.clear(); 
         
         // retrieve all of the sub-elements
         Elements elements = workspace.getChildElements();
         Element element = null; 
         int length = elements.size();
         
         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            // FIXME - atom assumes that it has been defined. WHAT DID I MEAN???
            if( isInstanceOf(element, "title", Namespaces.NS_ATOM ) )
            {
               title = new Title();
               title.unmarshall(element);   
            }
            else if( isInstanceOf(element, "collection", Namespaces.NS_APP ))
            {
               Collection collection = new Collection( );
               collection.unmarshall(element);
               collections.add(collection);
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in workspace: " + ex.getMessage());
         throw new UnmarshallException("Unable to parse element in workspace.", ex);
      }
   }
   
}
