package org.dspace.browse;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="bi_dis")
public class EntityDis
{
    private int id;
    private String value;
    private String sortValue;
    private String disType;
    
    public EntityDis() {}

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

    @Column(name="type")
    public String getDisType()
    {
        return disType;
    }

    public void setDisType(String disType)
    {
        this.disType = disType;
    }
}
