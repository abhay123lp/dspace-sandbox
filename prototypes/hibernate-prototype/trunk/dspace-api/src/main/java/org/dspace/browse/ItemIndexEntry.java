package org.dspace.browse;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.content.Item;

@Entity
@Table(name="bi_item_index") 
public class ItemIndexEntry
{
    private String sortValue;
    private int indexNumber;
    private Item item;
    private boolean withdrawn;
    private int id;
    
    public ItemIndexEntry () {}

    @Column(name="sort_value")
    public String getSortValue()
    {
        return sortValue;
    }

    public void setSortValue(String sortValue)
    {
        this.sortValue = sortValue;
    }

    @ManyToOne
    @JoinColumn(name="item_id")
    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @Column(name="item_withdrawn")
    public boolean isWithdrawn()
    {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn)
    {
        this.withdrawn = withdrawn;
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
