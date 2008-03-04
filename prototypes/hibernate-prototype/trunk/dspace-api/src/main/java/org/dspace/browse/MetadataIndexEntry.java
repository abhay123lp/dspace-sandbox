package org.dspace.browse;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.content.Item;

@Entity
@Table(name="bi_metadata_index_distinct", uniqueConstraints={@UniqueConstraint(columnNames="value")})
public class MetadataIndexEntry
{
    private int id;
    private List<Item> items;
    private String value;
    private String sortValue;
    private int indexNumber;
    
    public MetadataIndexEntry() {}

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name="map_id")
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @ManyToMany
    @JoinTable(name="bi_metadata_map")
    public List<Item> getItems()
    {
        return items;
    }

    public void setItems(List<Item> items)
    {
        this.items = items;
    }
    
    public void addItem(Item item) {
        items.add(item);
    }

    @Column(name="value")
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Column(name="sort_value")
    public String getSortValue()
    {
        return sortValue;
    }

    public void setSortValue(String sortValue)
    {
        this.sortValue = sortValue;
    }

    @Column(name="index_number")
    public int getIndexNumber()
    {
        return indexNumber;
    }

    public void setIndexNumber(int indexNumber)
    {
        this.indexNumber = indexNumber;
    }
    
}
