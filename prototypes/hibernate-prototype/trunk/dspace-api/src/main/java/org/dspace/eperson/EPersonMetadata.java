package org.dspace.eperson;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints={@UniqueConstraint(columnNames={"eperson_id", "field"})})
public class EPersonMetadata {
	private String field;
	private String value;
	private EPerson eperson;
	private int id;
	
	protected EPersonMetadata() {}
	
	protected EPersonMetadata(EPerson eperson, String field, String value) {
		this.eperson=eperson;
		this.field=field;
		this.value=value;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}	

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	@ManyToOne
	public EPerson getEperson() {
		return eperson;
	}

	public void setEperson(EPerson eperson) {
		this.eperson = eperson;
	}
}
