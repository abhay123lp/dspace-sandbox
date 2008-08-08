/*
 * Essence.java
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

package org.dspace.app.recsys.researchContext;

import org.dspace.core.Context;
import org.dspace.app.recsys.researchContext.dao.EssenceDAOFactory;

/**
 * <code>Essence</code> objects represent the source of <code>KeyValue</code>
 * objects. Each <code>ResearchContext</code> will have at least one
 * <code>Essence</code>, know as the local essence.  Additional
 * <code>Essences</code> are usually added to <code>ResearchContexts</code> by
 * adding in remote <code>Atom feeds</code>.
 *
 * @author Desmond Elliott
 */
public class Essence {

    /** Database ID of this <code>Essence</code> */
    public int ID;

    /** Weight of this <code>Essence</code> */
    public int weight;

    /** URI of this <code>Essence</code> source */
    public String uri;

    /** ID of <code>ResearchContext</code> this <code>Essence</code> is
     * paired with */
    public int researchContextID;

    /** User-defined name for this <code>Essence</code> */
    public String name;

    /** <code>DSpace Context</code> for database access */
    private Context context;

    /**
     * Creates and returns a new <code>Essence</code> with a given databse ID
     *
     * @param ID the database ID of this <code>Essence</code>
     * @param context <code>DSpace Context</code>
     */
    public Essence(int ID, Context context)
    {
        this.ID = ID;
        this.context = context;
    }

    /**
     * Gets the ID of this <code>Essence</code>
     *
     * @return the ID of this <code>Essence</code>
     */
    public int getID() {
        return ID;
    }

    /**
     * Gets the weight of this <code>Essence</code>
     *
     * @return the weight of this <code>Essence</code>
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets the weight of this <code>Essence</code>. The weight is used to
     * modify the value of each <code>KeyValue</code> object associated with
     * this <code>Essence</code>.
     *
     * @param weight the weight to set. Must be greater than or equal to 0.
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Gets the URI of this <code>Essence</code>
     *
     * @return the URI of this <code>Essence</code>
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the URI of this <code>Essence</code>. The URI defines the source of
     * the <code>Essence</code>. The source can be remote or local, so the
     * complete URI is required to avoid special case testing.
     *
     * @param uri the URI to set, not checked for validity.
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the <code>ResearchContext</code> ID this <code>Essence</code> is
     * paired with
     *
     * @return the ID of the <code>ResearchContext</code> this
     *         <code>Essence</code> is paired with
     */
    public int getResearchContextID() {
        return researchContextID;
    }

    /**
     * Sets the <code>ResearchContext</code> ID this <code>Essence</code> is
     * paired with.
     *
     * @param researchContextID ID to set. Is not validity checked.
     */
    public void setResearchContextID(int researchContextID) {
        this.researchContextID = researchContextID;
    }

    /**
     * Gets the user-defined name of this <code>Essence</code>.
     *
     * @return the user-defined name of this <code>Essence</code>.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the user-defined name of this <code>Essence</code>. A user-defined
     * name for an <code>Essence</code> lets users recall the association of an
     * <code>Essence</code> with their  <code>ResearchContext</code>
     *
     * @param name name of the <code>Essence</code>.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Determines if this <code>Essence</code> is the local Essence for
     * the ResearchContext it is associated with.
     *
     * @return true if this is the local Essence
     */
    public boolean isLocalEssence()
    {
        return EssenceDAOFactory.getInstance(context)
                                .isLocalEssence(ID, researchContextID);
    }
}