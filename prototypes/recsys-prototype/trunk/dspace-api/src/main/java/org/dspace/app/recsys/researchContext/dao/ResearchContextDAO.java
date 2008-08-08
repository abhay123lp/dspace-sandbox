/*
 * ResearchContextDAO.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.recsys.researchContext.dao;

import org.apache.log4j.Logger;
import org.dspace.app.recsys.researchContext.ResearchContext;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.content.Item;
import org.dspace.content.DCValue;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;

/**
 * <code>ResearchContextDAO</code> handles database access to
 * create, retrieve, update, and delete <code>ResearchContext</code> objects.
 * Any non database-specific code must be declared in this class instead of
 * <code>ResearchContextDAOPostgres</code>.
 *
 * @author Desmond Elliott
 */
public abstract class ResearchContextDAO {

    /** Logs data to <code>dspace.log</code> */
    protected Logger log = Logger.getLogger(EssenceDAO.class);

    /** <code>DSpace Context</code> which is used for database access */
    protected Context context;

    /**
     * Creates and returns a new <code>ResearchContext</code> by creating a
     * new table row.
     *
     * @param uri the URI of the local Essence
     *
     * @return A new <code>ResearchContext</code> object with
     *         <code>ID</code> equal to the <code>ID</code> of the database
     *         table row created.
     */
    public abstract ResearchContext create(String uri);

    /**
     * Retrieves a <code>ResearchContext</code> from the
     * database based on the argument <code>id</code>. If the the
     * <code>ID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param id the <code>ID</code> of the <code>ResearchContext</code>
     *           to retrieve from the database
     * @return a <code>ResearchContext</code> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract ResearchContext retrieve(int id);

    /**
     * Retrieves a <code>ResearchContext</code> from the
     * database based on the argument <code>uuid</code>. If the the
     * <code>UUID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param uuid the <code>UUID</code> of the
     *             <code>ResearchContext</code> to retrieve from the
     *             database
     * @return a <code>ResearchContext</code>> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract ResearchContext retrieve(UUID uuid);

    /**
     * Updates the database row that represents the
     * <code>ResearchContext</code> object based on the <code>ID</code>
     * of the argument object. If the the <code>ID</code> does not exist in the
     * database, or any of the fields in the argument object are null,
     * this method will not succeed.
     *
     * @param e the <code>ResearchContext</code> to write to the
     *             database
     */
    public abstract void update(ResearchContext e);

    /**
     * Deletes the database row of the <code>ResearchContext</code>
     * object represented by the <code>id</code> argument. If a database row in
     * the <code>quambo_research_context</code> table does not exist
     * with the argument <code>id</code>, an Exception is thrown.
     *
     * @param id the <code>ID</code> of the
     *           <code>ResearchContext</code> object
     */
    public abstract void delete(int id);

    /**
     * Gets all <code>ResearchContext</code>s
     *
     * @return A <code>List</code> of all
     *         <code>ResearchContext</code>s stored in the database
     */
    public abstract List<ResearchContext> getAllResearchContexts();
    /**
     * Gets all <code>ResearchContext</code>s associated with a particular
     * <code>EPerson</code>
     *
     * @param eperson <code>EPerson</code> to get
     *                <code>ResearchContext</code>(s) for
     *
     * @return A <code>List</code> of the
     *         <code>ResearchContext</code>(s) stored in the database
     *         associated with the <code>EPerson</code>
     */
    public abstract List<ResearchContext> getResearchContexts(EPerson eperson);

    /**
     * Gets a List of the <code>ResearchContext</code>s an <code>Item</code> is
     * or is not <code>Bookmarked</code> in depending on the status of the
     *  <code>isBookmarked</code> argument and the logged in user.
     *
     * The signature should be read as "Get the Research Contexts this EPerson
     * has/has not bookmarked this Item"
     *
     * @param e the logged in user
     * @param isBookmarked determines the ResearchContexts returned  
     * @param i the Item
     * @return a List of ResearchContexts
     */
    public abstract List<ResearchContext>
      getResearchContexts(EPerson e, boolean isBookmarked, Item i);


    /**
     * Extracts metadata from the Item and adds it to the ResearchContext's
     * local Essence. The metadata extracted from the Item is defined in
     * quambo.similarity.terms in dspace.cfg.
     *
     * @param item the Item to extract metadata from
     * @param r the ResearchContext to add the metadata to
     */
    public void addMetadata(Item item, ResearchContext r)
    {
        List<String> attributes = getDefinedAttributes();

        // Based on the attributes defined in dspace.cfg, extract metadata
        // from each item
        for (String attribute: attributes)
        {
            DCValue[] terms = item.getMetadata(attribute);

            for (DCValue d: terms)
            {
                r.addKeyValue(d.value, 1, attribute, r.getLocalEssenceID());
            }
        }

        update(r);
    }

    /**
     * Removes the metadata occuring in the Item from the ResearchContext's
     * local Essence. The metadata extracted from the Item is defined in
     * quambo.similarity.terms in dspace.cfg.
     *
     * @param item the Item to extract metadata from
     * @param r the ResearchContext to add the metadata to
     */
    public void removeMetadata(Item item, ResearchContext r)
    {
        List<String> attributes = getDefinedAttributes();

        // Based on the attributes defined in dspace.cfg, extract metadata
        // from each item
        for (String attribute: attributes)
        {
            DCValue[] terms = item.getMetadata(attribute);

            for (DCValue d: terms)
            {
                r.addKeyValue(d.value, -1, attribute, r.getLocalEssenceID());
            }
        }

        update(r);
    }

    /**
     * Determines if the <code>ResearchContext</code> with the given <code>UUID</code> is the <code>initial
     * ResearchContext</code> for an <code>EPerson</code>
     *
     * @param uuid the <code>UUID</code> of the <code>ResearchContext</code>
     * @return true if it is the <code>initial ResearchContext</code> for the
     *         <code>EPerson</code> in this <code>Context</code>
     */
    public abstract boolean isInitialResearchContext(UUID uuid);

    /**
     * Gets <code>quambo.similarity.terms</code> as defined in
     * <code>dspace.cfg</code>. Does not perform error checking.
     *
     * @return a List of the terms to be used to extract metadata from items for
     *         the purposes of comparison.
     */
    private List<String> getDefinedAttributes()
    {
        // Extract the defined terms from dspace.cfg
        String terms =
                ConfigurationManager.getProperty("quambo.similarity.terms");

        String[] splitTerms = terms.split(",");

        for (int i = 0; i < splitTerms.length; i++)
        {
            splitTerms[i] = splitTerms[i].trim();
        }

        return Arrays.asList(splitTerms);
    }
}
