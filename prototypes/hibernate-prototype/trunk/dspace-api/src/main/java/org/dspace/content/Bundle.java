/*
 * Bundle.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.content.factory.BitstreamFactory;

/**
 * Class representing bundles of bitstreams stored in the DSpace system
 * <P>
 * The corresponding Bitstream objects are loaded into memory. At present, there
 * isn't reallyt any metadata associated with bundles - they are simple
 * containers. Thus, the <code>update</code> method doesn't do much yet.
 * Creating, adding or removing bitstreams has instant effect in the database.
 * 
 * @author James Rutherford
 * @version $Revision$
 */
@Entity
public class Bundle extends DSpaceObject {

	private static Logger log = Logger.getLogger(Bundle.class);

	/* The Item owner of the bundle */
	private Item item;
	private String name;
	private Bitstream primaryBitstream;
	private List<Bitstream> bitstreams;

	private boolean modifiedMetadata;
	private boolean modified;

	private Context context;

	public Bundle(Context context) {
		this.context = context;
		name = "";
		
		bitstreams = new ArrayList<Bitstream>();
		modified = modifiedMetadata = false;
	}

	public Bitstream createBitstream(InputStream is) throws AuthorizeException,
			IOException {
		AuthorizeManager.authorizeAction(context, this, Constants.ADD);
		/*FIXME: bisogna che il bitstream venga creato tramite is */
		Bitstream b = BitstreamFactory.getInstance(context);

		addBitstream(b);

		return b;
	}
	
	/* FIXME: rivedere la responsabilità di register() e completare */
/*	public Bitstream registerBitstream(int assetstore, String bitstreamPath)
			throws AuthorizeException, IOException {
		AuthorizeManager.authorizeAction(context, this, Constants.ADD);

		Bitstream b = BitstreamDAO.register(assetstore, bitstreamPath);

		addBitstream(b);

		return b;
	}
*/
	public void addBitstream(Bitstream b) throws AuthorizeException {
		for (Bitstream bitstream : bitstreams) {
			if (bitstream.equals(b)) {
				return;
			}
		}

		AuthorizeManager.addPolicy(context, b, Constants.WRITE, context
				.getCurrentUser());
		AuthorizeManager.inheritPolicies(context, this, b);

		bitstreams.add(b);

		context.addEvent(new Event(Event.ADD, Constants.BUNDLE, getId(),
				Constants.BITSTREAM, b.getId(), String.valueOf(b
						.getSequenceID())));
	}

	public void removeBitstream(Bitstream b) throws AuthorizeException {
		Iterator<Bitstream> i = bitstreams.iterator();
		while (i.hasNext()) {
			Bitstream bitstream = i.next();
			if (bitstream.getId() == b.getId()) {
				i.remove();

				context.addEvent(new Event(Event.REMOVE, Constants.BUNDLE,
						getId(), Constants.BITSTREAM, b.getId(), String
								.valueOf(b.getSequenceID())));
			}
		}
	}
	@Column(name="name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		modifiedMetadata = true;
	}


	/*
	 * FIXME: confrontare questo metodo con quello di jim
	 */
	@OneToOne
	@JoinColumn(name="primary_bitstream_id")
	public Bitstream getPrimaryBitstream() {
		return primaryBitstream;
	}
	
	public void setPrimaryBitstream(Bitstream primaryBitstream) {
		this.primaryBitstream=primaryBitstream;
	}
	
	@Transient
	public Bitstream getBitstreamByName(String name) {
		Bitstream target = null;

		for (Bitstream bitstream : bitstreams) {
			if (name.equals(bitstream.getName())) {
				target = bitstream;
				break;
			}
		}

		return target;
	}

	public void setBitstreams(List<Bitstream> bitstreams) {
		this.bitstreams = bitstreams;
	}
	@OneToMany(mappedBy="bundle")
	public List<Bitstream> getBitstreams() {
		return bitstreams;
	}

	@Transient
	public int getType()
    {
        return Constants.BUNDLE;
    }
	@ManyToOne
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}
	@Transient
	public boolean isModifiedMetadata() {
		return modifiedMetadata;
	}
	@Transient
	public boolean isModified() {
		return modified;
	}
}
