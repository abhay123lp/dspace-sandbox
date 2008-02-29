package org.dspace.browse;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.dspace.content.Item;

public class EntityBi
{
    private int id;
    private Item item;
    private String sort1;
    private String sort2;
    private String sort3;
    private String biType;
    
    public EntityBi() {}

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

    @OneToOne
    @JoinColumn(name="item_id")
    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    @Column(name="sort_1")
    public String getSort1()
    {
        return sort1;
    }

    public void setSort1(String sort1)
    {
        this.sort1 = sort1;
    }

    @Column(name="sort_2")
    public String getSort2()
    {
        return sort2;
    }

    public void setSort2(String sort2)
    {
        this.sort2 = sort2;
    }

    @Column(name="sort_3")
    public String getSort3()
    {
        return sort3;
    }

    public void setSort3(String sort3)
    {
        this.sort3 = sort3;
    }

    @Column(name="type")
    public String getBiType()
    {
        return biType;
    }

    public void setBiType(String biType)
    {
        this.biType = biType;
    }
    
    
}
