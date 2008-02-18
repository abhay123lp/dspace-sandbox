/*
 * EPerson.java
 *
 * Version: $Revision: 1772 $
 *
 * Date: $Date: 2008-01-29 22:46:01 +0100 (mar, 29 gen 2008) $
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
package org.dspace.eperson;

import java.util.Map;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;

import org.apache.log4j.Logger;

/**
 * Class representing an e-person.
 *
 * @author David Stuve
 * @version $Revision: 1772 $
 */
@Entity
public class EPerson extends DSpaceObject {
	private static Logger log = Logger.getLogger(EPerson.class);

	/** See EPersonMetadataField. */
	//private Map<EPersonMetadataField, String> metadata;
	private Map<String, EPersonMetadata> epersonMetadata;

	private boolean selfRegistered;
	private boolean enabled; //the old "canLogin"
	private boolean requireCertificate;

	/** Sort fields */
	public static final int EMAIL = 1;
	public static final int LASTNAME = 2;
	public static final int ID = 3;
	public static final int NETID = 4;
	public static final int LANGUAGE = 5;

	/** Flag set when data is modified, for events */
	private boolean modified;

	/** Flag set when metadata is modified, for events */
	private boolean modifiedMetadata;

	public enum EPersonMetadataField {
		FIRSTNAME("firstname"), LASTNAME("lastname"), PASSWORD("password"), EMAIL(
				"email"), PHONE("phone"), NETID("netid"), LANGUAGE("language");

		private String name;

		private EPersonMetadataField(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public static EPersonMetadataField fromString(String name) {
			for (EPersonMetadataField f : values()) {
				if (f.toString().equals(name)) {
					return f;
				}
			}

			throw new IllegalArgumentException(name
					+ " isn't a valid metadata field for EPeople.");
		}

		@Transient
		public String getName() {
			return name;
		}

		@Transient
		public void setName(String name) {
			this.name = name;
		}
	}

	protected EPerson() {
	}

	public EPerson(Context context) {

		this.context = context;
		epersonMetadata = new TreeMap<String, EPersonMetadata>();
//		metadata = new EnumMap<EPersonMetadataField, String>(EPersonMetadataField.class);
	}
	
    @OneToMany(mappedBy="eperson",cascade = CascadeType.ALL)
	@MapKey(name = "field")
	public Map<String, EPersonMetadata> getEpersonMetadata() {
		return epersonMetadata;
	}

	public void setEpersonMetadata(Map<String, EPersonMetadata> epersonMetadata) {
		this.epersonMetadata = epersonMetadata;
	}

	@Transient
	public String getLanguage() {
		return epersonMetadata.get(EPersonMetadataField.LANGUAGE).getValue();
	}

	public void setLanguage(String language) {
		if (language != null) {
			language = language.toLowerCase();
		}
		setMetadata(EPersonMetadataField.LANGUAGE.toString(), language);
		modified=true;
	}

	@Transient
	public String getEmail() {
		return epersonMetadata.get(EPersonMetadataField.EMAIL).getValue();
	}

	public void setEmail(String email) {
		if (email != null) {
			email = email.toLowerCase();
		}
		setMetadata(EPersonMetadataField.EMAIL.toString(), email);
		modified = true;		
	}

	@Transient
	public String getNetid() {
		return epersonMetadata.get(EPersonMetadataField.NETID).getValue();
	}

	public void setNetid(String netid) {
		if (netid != null) {
			netid = netid.toLowerCase();
		}
		setMetadata(EPersonMetadataField.NETID.toString(), netid);
		modified = true;
		
	}

	@Transient
	public String getName() {
		return getEmail();
	}

	/**
	 * Get the e-person's full name, combining first and last name in a
	 * displayable string.
	 *
	 * @return their full name
	 */
	@Transient
	public String getFullName() {
		String firstName = epersonMetadata.get(EPersonMetadataField.FIRSTNAME).getValue();
		String lastName = epersonMetadata.get(EPersonMetadataField.LASTNAME).getValue();

		if ((lastName == null) && (firstName == null)) {
			return getEmail();
		} else if (firstName == null) {
			return lastName;
		} else {
			return (firstName + " " + lastName);
		}
	}

	@Transient
	public String getFirstName() {
		return epersonMetadata.get(EPersonMetadataField.FIRSTNAME).getValue();
	}

	public void setFirstName(String firstName) {
		setMetadata(EPersonMetadataField.FIRSTNAME.toString(), firstName);
		modified = true;
	}

	@Transient
	public String getLastName() {
		return epersonMetadata.get(EPersonMetadataField.LASTNAME).getValue();
	}

	public void setLastName(String lastName) {
		setMetadata(EPersonMetadataField.LASTNAME.toString(), lastName);
		modified = true;		
	}

	@Transient
	//FIXME metodo da togliere
	public void setCanLogIn(boolean canLogin) {
		this.enabled = canLogin;
		modified = true;
		
	}

	@Transient
	//FIXME metodo da togliere
	public boolean canLogIn() {
		return enabled;
	}

	public void setRequireCertificate(boolean requireCertificate) {
		this.requireCertificate = requireCertificate;
		modified = true;
		
	}

	@Column(name = "require_certificate")
	public boolean getRequireCertificate() {
		return requireCertificate;
	}

	public void setSelfRegistered(boolean selfRegistered) {
		this.selfRegistered = selfRegistered;
		modified = true;
		
	}

	@Column(name = "self_registered")
	public boolean getSelfRegistered() {
		return selfRegistered;
	}

	@Transient
	public String getMetadata(EPersonMetadataField field) {
		return epersonMetadata.get(field.toString()).getValue();
	}

	public void setMetadata(EPersonMetadataField field, String value) {
		setMetadata(field.toString(), value);
	}

	@Transient
	public String getMetadata(String field) {
		return epersonMetadata.get(field).getValue();
		//return metadata.get(EPersonMetadataField.fromString(field));
	}

	@Transient
	public void setMetadata(String field, String value) {
		EPersonMetadata em = epersonMetadata.get(field);
		if(em!=null) {
			em.setValue(value);
		} else {
			epersonMetadata.put(field, new EPersonMetadata(this, field, value));
		}		
		modifiedMetadata = true;
		addDetails(field);
	}

	public void setPassword(String password) {
		setMetadata(EPersonMetadataField.PASSWORD.toString(), Utils.getMD5(password));		
		modified = true;
	}

	public boolean checkPassword(String attempt) {
		String encoded = Utils.getMD5(attempt);

		return (encoded.equals(epersonMetadata.get(EPersonMetadataField.PASSWORD).getValue()));
	}

	@Column(name = "can_log_in")
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////

	/**
	 * return type found in Constants
	 */
	@Transient
	public int getType() {
		return Constants.EPERSON;
	}

	////////////////////////////////////////////////////////////////////
	// Deprecated methods
	////////////////////////////////////////////////////////////////////

	//    @Deprecated
	//    public static EPerson[] findAll(Context context, int sortField)
	//    {
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//        List<EPerson> epeople = dao.getEPeople(sortField);
	//
	//        return (EPerson[]) epeople.toArray(new EPerson[0]);
	//    }
	//
	//    @Deprecated
	//    public static EPerson find(Context context, int id)
	//    {
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//
	//        return dao.retrieve(id);
	//    }
	//
	//    @Deprecated
	//    public static EPerson[] search(Context context, String query)
	//    {
	//        return search(context, query, -1, -1);
	//    }
	//
	//    @Deprecated
	//    public static EPerson[] search(Context context, String query,
	//            int offset, int limit)
	//	{
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//        List<EPerson> epeople = dao.search(query, offset, limit);
	//
	//        return (EPerson[]) epeople.toArray(new EPerson[0]);
	//	}
	//
	//    @Deprecated
	//    public static EPerson findByEmail(Context context, String email)
	//    {
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//
	//        return dao.retrieve(EPersonMetadataField.EMAIL, email);
	//    }
	//
	//    @Deprecated
	//    public static EPerson findByNetid(Context context, String netid)
	//    {
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//
	//        return dao.retrieve(EPersonMetadataField.NETID, netid);
	//    }
	//
	//    @Deprecated
	//    public static EPerson create(Context context) throws AuthorizeException
	//    {
	//        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
	//
	//        return dao.create();
	//    }

	/*    @Deprecated
	 public void update() throws AuthorizeException
	 {
	 dao.update(this);

	 if (modified)
	 {
	 context.addEvent(new Event(Event.MODIFY, Constants.EPERSON, getId(), null));
	 modified = false;
	 }
	 if (modifiedMetadata)
	 {
	 context.addEvent(new Event(Event.MODIFY_METADATA, Constants.EPERSON, getId(), getDetails()));
	 modifiedMetadata = false;
	 clearDetails();
	 }
	 }
	 */
	/*    @Deprecated
	 public void delete() throws AuthorizeException, EPersonDeletionException
	 {
	 dao.delete(getId());
	 context.addEvent(new Event(Event.DELETE, Constants.EPERSON, getId(), getEmail()));
	 }*/
}
