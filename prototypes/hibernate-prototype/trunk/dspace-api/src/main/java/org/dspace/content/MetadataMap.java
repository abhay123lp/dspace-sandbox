package org.dspace.content;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class MetadataMap {
	private String field;
	private String value;
	private Collection collection;
	private int id;
	
	public MetadataMap() {}
	
	public MetadataMap(Collection collection, String field, String value) {
		this.collection=collection;
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
	@ManyToOne
	public Collection getCollection() {
		return collection;
	}
	public void setCollection(Collection collection) {
		this.collection = collection;
	}
	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
