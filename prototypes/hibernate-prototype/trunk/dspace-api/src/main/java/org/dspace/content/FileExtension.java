package org.dspace.content;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class FileExtension
{
    private int id;
    private String extension;
    private BitstreamFormat bitstreamFormat;
    
    public FileExtension() {}

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name="file_extension_id")
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    @Column(name="extension")
    public String getExtension()
    {
        return extension;
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    @ManyToOne
    @JoinColumn(name="bitstream_format_id")
    public BitstreamFormat getBitstreamFormat()
    {
        return bitstreamFormat;
    }

    public void setBitstreamFormat(BitstreamFormat bitstreamFormat)
    {
        this.bitstreamFormat = bitstreamFormat;
    }
}
