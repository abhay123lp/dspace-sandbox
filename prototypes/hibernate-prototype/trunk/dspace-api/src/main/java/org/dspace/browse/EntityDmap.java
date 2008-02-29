package org.dspace.browse;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.content.Item;

@Entity
@Table(name="bi_dmap")
public class EntityDmap
{
    private int id;
    private EntityDis entityDis;
    private Item item;
    private String dmapType;
    
    public EntityDmap() {}

    @Id
    @Column(name="map_id")
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name="distinct_id")
    public EntityDis getEntityDis()
    {
        return entityDis;
    }

    public void setEntityDis(EntityDis entityDis)
    {
        this.entityDis = entityDis;
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

    @Column(name="type")
    public String getDmapType()
    {
        return dmapType;
    }

    public void setDmapType(String dmapType)
    {
        this.dmapType = dmapType;
    }
    
    
}
