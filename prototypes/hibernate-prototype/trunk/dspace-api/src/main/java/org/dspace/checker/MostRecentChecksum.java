package org.dspace.checker;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.dspace.content.Bitstream;

@Entity
@Table(name="most_recent_checksum", uniqueConstraints={@UniqueConstraint(columnNames={"bitstream"})})
public class MostRecentChecksum
{
    private Bitstream bitstream;
    private boolean toBeProcessed;
    private String expectedChecksum;
    private String currentChecksum;
    private Date lastProcessStartDate;
    private Date lastProcessEndDate;
    private String checksumAlgorithm;
    private boolean matchedPrevChecksum;
    private String result;
    private int id; //FIXME fa schifo, da togliere. come si mettono le chiavi esterne in jpa????
    
    
    public MostRecentChecksum() {}
   
    @OneToOne
    @JoinColumn(name="bitstream_id")
    public Bitstream getBitstream()
    {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream)
    {
        this.bitstream = bitstream;
    }

    @Column(name="to_be_processed")
    public boolean isToBeProcessed()
    {
        return toBeProcessed;
    }

    public void setToBeProcessed(boolean toBeProcessed)
    {
        this.toBeProcessed = toBeProcessed;
    }

    @Column(name="expected_checksum")
    public String getExpectedChecksum()
    {
        return expectedChecksum;
    }

    public void setExpectedChecksum(String expectedChecksum)
    {
        this.expectedChecksum = expectedChecksum;
    }

    @Column(name="current_checksum")
    public String getCurrentChecksum()
    {
        return currentChecksum;
    }

    public void setCurrentChecksum(String currentChecksum)
    {
        this.currentChecksum = currentChecksum;
    }

    @Column(name="last_process_start_date")
    @Temporal(TemporalType.DATE)
    public Date getLastProcessStartDate()
    {
        return lastProcessStartDate;
    }

    public void setLastProcessStartDate(Date lastProcessStartDate)
    {
        this.lastProcessStartDate = lastProcessStartDate;
    }
    
    @Column(name="last_process_end_date")
    @Temporal(TemporalType.DATE)
    public Date getLastProcessEndDate()
    {
        return lastProcessEndDate;
    }

    public void setLastProcessEndDate(Date lastProcessEndDate)
    {
        this.lastProcessEndDate = lastProcessEndDate;
    }

    @Column(name="checksum_algorithm")
    public String getChecksumAlgorithm()
    {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm)
    {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Column(name="matched_prev_checksum")
    public boolean isMatchedPrevChecksum()
    {
        return matchedPrevChecksum;
    }

    public void setMatchedPrevChecksum(boolean matchedPrevChecksum)
    {
        this.matchedPrevChecksum = matchedPrevChecksum;
    }

    @Column(name="result")
    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    @Id    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    
}
