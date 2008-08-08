/*
 * EssenceDAO.java
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

import org.dspace.app.recsys.researchContext.Essence;
import org.dspace.core.Context;
import org.apache.log4j.Logger;

import java.util.UUID;
import java.util.List;

/**
 * <code>EssenceDAO</code> handles database access to
 * create, retrieve, update, and delete <code>Essences</code> objects.
 * Any non database-specific code must be declared in this class instead of
 * <code>EssenceDAOPostgres</code>.
 *
 * @author Desmond Elliott
 */
public abstract class EssenceDAO {

    /** Logs data to <code>dspace.log</code> */
    protected Logger log = Logger.getLogger(EssenceDAO.class);

    /** <code>DSpace Context</code> which is used for database access */
    protected Context context;

    /**
     * Creates and returns a new <code>Essence</code> by creating a new table
     * row
     *
     * @return A new <code>Essence</code> object with
     *         <code>ID</code> equal to the <code>ID</code> of the database
     *         table row created.
     */
    public abstract Essence create();

    /**
     * Retrieves an <code>Essence</code> from the
     * database based on the argument <code>id</code>. If the the
     * <code>ID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param id the <code>ID</code> of the <code>Essence</code>
     *           to retrieve from the database
     * @return a <code>Essence</code> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract Essence retrieve(int id);

    /**
     * Retrieves an <code>Essence</code> from the
     * database based on the argument <code>uuid</code>. If the the
     * <code>UUID</code> does not exist in the database, this method returns
     * <code>null</code>.
     *
     * @param uuid the <code>UUID</code> of the
     *             <code>Essence</code> to retrieve from the
     *             database
     * @return a <code>Essence</code> object if it exists in the
     *         database, otherwise returns <code>null</code>.
     */
    public abstract Essence retrieve(UUID uuid);

    /**
     * Updates the database row that represents the
     * <code>Essence</code> object based on the <code>ID</code>
     * of the argument object. If the the <code>ID</code> does not exist in the
     * database, or any of the fields in the argument object are null,
     * this method will not succeed.
     *
     * @param e the <code>Essence</code> to write to the
     *             database
     */
    public abstract void update(Essence e);

    /**
     * Deletes the database row of the <code>Essence</code>
     * object represented by the <code>id</code> argument. If a database row in
     * the <code>quambo_essence</code> table does not exist
     * with the argument <code>id</code>, an Exception is thrown.
     *
     * @param id the <code>ID</code> of the
     *           <code>Essence</code> object
     */
    public abstract void delete(int id);

    /**
     * Gets all <code>Essence</code>s associated with a particular ResearchContext
     *
     * @param researchContextID <code>ResearchContext</code> to get
     *                          <code>Essence</code>(s) for
     *
     * @return A <code>List</code> of the
     *         <code>Essence</code>(s) stored in the database
     *         associated with the ResearchContext
     */
    public abstract List<Essence> getEssences(int researchContextID);

    /**
     * Determines if the <code>Essence</code> argument is the local Essence for
     * the ResearchContext argument.
     * 
     * <p>
     * <code>otherItem</code> and <code>viewedItem</code> can be the same
     * <code>Item</code>, however, it does not make sense because an
     * <code>Item</code> would always provide a similarity of 1.0 against
     * itself.
     * </p>
     *
     * @param eID the ID of the Essence
     * @param rID the ID of the ResearchContext
     * @return true if <code>eID</code> is the local Essence for
     *         <code>rID</code>
     */
    public abstract boolean isLocalEssence(int eID, int rID);
}