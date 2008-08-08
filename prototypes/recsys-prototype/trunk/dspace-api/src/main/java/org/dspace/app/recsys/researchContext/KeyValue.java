/*
 * KeyValue.java
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

/**
 * <code>KeyValue</code> objects represent metadata in a
 * <code>ResearchContext</code>. The <code>key</code> is the String
 * representation of the metadata, the <code>Value</code> is the number
 * of times this Key is represented, the <code>essenceID</code> is the
 * <code>Essence</code> this <code>KeyValue</code> is stored in, and
 * <code>researchContextID</code> is the <code>ResearchContext</code> this
 * <code>KeyValue</code> is stored in.
 *
 * @author Desmond Elliott
 */
public class KeyValue {

    /** Database ID of this object */
    public int keyValueID;

    /** <code>Key</code> of this <code>KeyValue</code> */
    public String key;

    /** Frequency of occurence of this <code>KeyValue</code> */
    public int value;

    /** Metadata type of this <code>KeyValue</code> */
    public String type;

    /** <code>Essence</code> this <code>KeyValue</code> is paired with */
    public int essenceID;

    /** <code>ResearchContext</code> this <code>KeyValue</code> is paired with
     * */
    public int researchContextID;

    /**
     * Creates and returns a new <code>KeyValue</code> with the given ID
     *
     * @param id database ID of this <code>KeyValue</code>
     */
    public KeyValue(int id)
    {
        keyValueID = id;
    }

    /**
     * Gets the database ID of this <code>KeyValue</code>
     *
     * @return the database ID of this <code>KeyValue</code>
     */
    public int getKeyValueID() {
        return keyValueID;
    }

    /**
     * Sets the database ID of this <code>KeyValue</code>
     *
     * @param keyValueID the databse ID of this <code>KeyValue</code>
     */
    public void setKeyValueID(int keyValueID) {
        this.keyValueID = keyValueID;
    }

    /**
     * Gets the Key of this <code>KeyValue</code>. Keys are usually extracted
     * metadata from <code>Bookmarked</code> items
     *
     * @return the Key of this <code>KeyValue</code> as a String.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the <code>Key</code> of this <code>KeyValue</code>
     *
     * @param key the <code>Key</code> to set
     */
    public void setKey(String key) {
        this.key = key;
    }    

    /**
     * Gets the type of this <code>KeyValue</code>. The types which can be
     * stored are usually defined by the administrator in the
     * <code>quambo.similarity.terms</code> configuration.
     *
     * @return the type of this <code>KeyValue</code>
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this <code>KeyValue</code>. The types which can be
     * stored are usually defined by the administrator in the
     * <code>quambo.similarity.terms</code> configuration.
     *
     * @param type the type to set to this <code>KeyValue</code>
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the frequency of occurence of this <code>KeyValue</code>
     *
     * @return the number of times this <code>KeyValue</code> occurs in this
     *         <code>Essence</code> in this <code>ResearchContext</code>
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the frequency of occurence of this <code>KeyValue</code>
     *
     * @param value the number of times this <code>KeyValue</code> occurs in
     *              this <code>Essence</code> in this
     *              <code>ResearchContext</code>
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Gets the <code>Essence</code> ID of this <code>KeyValue</code>. A
     * <code>KeyValue</code> is assocated with at most one <code>Essence</code>.
     *
     * @return the Essence ID of this <code>KeyValue</code>
     */
    public int getEssenceID() {
        return essenceID;
    }

    /**
     * Sets the <code>Essence</code> ID of this <code>KeyValue</code>
     *
     * @param essenceID the <code>Essence</code> ID of this
     *                  <code>KeyValue</code>
     */
    public void setEssenceID(int essenceID) {
        this.essenceID = essenceID;
    }

    /**
     * Gets the <code>ResearchContext</code> ID of this <code>KeyValue</code>
     *
     * @return the <code>ResearchContext</code> ID of this <code>KeyValue</code>
     */
    public int getResearchContextID() {
        return researchContextID;
    }

    /**
     * Sets the <code>ResearchContext</code> ID of this <code>KeyValue</code>
     *
     * @param researchContextID the <code>ResearchContext</code> ID of this
     *                          <code>KeyValue</code>
     */
    public void setResearchContextID(int researchContextID) {
        this.researchContextID = researchContextID;
    }

}
