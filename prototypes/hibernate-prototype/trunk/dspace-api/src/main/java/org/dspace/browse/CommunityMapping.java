package org.dspace.browse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.content.Community;
import org.dspace.content.Item;

@Entity
@Table(name="communities2item")
public class CommunityMapping
{
    private Item item;
    private Community community;
    private int id;
    
    public CommunityMapping() {}

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

    @ManyToOne
    @JoinColumn(name="community_id")
    public Community getCommunity()
    {
        return community;
    }

    public void setCommunity(Community community)
    {
        this.community = community;
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
}
