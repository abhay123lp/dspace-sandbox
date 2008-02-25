/*
 * Bitstream.java
 *
 * Version: $Revision: 1772 $
 *
 * Date: $Date: 2008-01-29 22:46:01 +0100 (mar, 29 gen 2008) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ApplicationService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import org.apache.log4j.Logger;


/**
 * Class representing bitstreams stored in the DSpace system.
 * <P>
 * When modifying the bitstream metadata, changes are not reflected in the
 * database until <code>update</code> is called. Note that you cannot alter
 * the contents of a bitstream; you need to create a new bitstream.
 * 
 * @author Robert Tansley
 * @version $Revision: 1772 $
 */
@Entity
public class Bitstream extends DSpaceObject
{   
    /** log4j logger */
    private static Logger log = Logger.getLogger(Bitstream.class);
   
    /* The bundle owner of the bitstream */
    private Bundle bundle;

    private int sequenceID;
    private String name;
    private String source;
    private String description;
    private String checksum;
    private String checksumAlgorithm;
    private Long size;
    private String userFormatDescription;
    private BitstreamFormat bitstreamFormat;
    private int storeNumber;
    private String internalID;
    private boolean deleted;


    private boolean modifiedMetadata;
    private boolean modified;
    
    private Context context;
    
    public Bitstream(Context context) {
        this.context = context;
        modified = modifiedMetadata = false;
        clearDetails();        
    }
    
    public Bitstream() {}
    
    @Column(name = "sequence_id")
    public int getSequenceID() {
        return sequenceID;
    }
    
    public void setSequenceID(int sequenceID) {
        this.sequenceID = sequenceID;
        modifiedMetadata = true;
        addDetails("SequenceID");
    }
    @Column(name = "name")
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name=name;
        modifiedMetadata = true;
        addDetails("Name");
    }
    @Column(name = "source")
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source=source;
        modifiedMetadata = true;
        addDetails("Source");
    }
    @Column(name="description")
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description=description;
        modifiedMetadata = true;
        addDetails("Description");
    }
    @Column(name="checksum")
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum=checksum;
    }
    @Column(name="checksum_algorithm")
    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }
    
    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm=checksumAlgorithm;
    }
    @Column(name = "size_bytes")
    public Long getSize() {
        return (size == null ? 0 : size.longValue());
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    @Column(name="user_format_description")
    public String getUserFormatDescription() {
        return userFormatDescription;
    }
    
    public void setUserFormatDescription(String userFormatDescription) {
//      setFormat(null);
        this.userFormatDescription = userFormatDescription;
//        modifiedMetadata = true;
//        addDetails("UserFormatDescription");
    }
    @Transient
    public String getFormatDescription() {
        if (BitstreamFormat.UNKNOWN_SHORT_DESCRIPTION.equals(
                    bitstreamFormat.getShortDescription())) {
            // Get user description if there is one
            if (userFormatDescription == null) {
                return BitstreamFormat.UNKNOWN_SHORT_DESCRIPTION;
            }
            return userFormatDescription;
        }
        // not null or Unknown
        return bitstreamFormat.getShortDescription();
    }    
    @ManyToOne
    public BitstreamFormat getFormat() {
        return bitstreamFormat;
    }
    
    @ManyToOne
    @JoinColumn(name="bitstream_format_id")
    public BitstreamFormat getBitstreamFormat() {
        return bitstreamFormat;
    }
    
    public void setBitstreamFormat(BitstreamFormat bitstreamFormat) {
        this.bitstreamFormat=bitstreamFormat;
    }
    
    public void setFormat(BitstreamFormat f) {
        if (f == null)
        {
            
            // Use "Unknown" format
            //bitstreamFormat = BitstreamFormat.findUnknown(context);
            bitstreamFormat = ApplicationService.findBitstreamFormatByShortDescription("Unknown", context);
        }
        else
        {
            bitstreamFormat = f;
        }

        // Remove user type description
        userFormatDescription = null;
        modified = true;
    }
    @Column(name = "store_number")
    public int getStoreNumber() {
        return storeNumber;
    }
    
    public void setStoreNumber(int storeNumber) {
        this.storeNumber=storeNumber;
    }
    @Column(name="internal_id")
    public String getInternalID() {
        return internalID;
    }
    
    public void setInternalID(String internalID) {
        this.internalID=internalID;
    }
    @Column(name = "deleted")
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Transient
    public boolean isRegisteredBitstream()
    {
        return BitstreamStorageManager.isRegisteredBitstream(internalID);
    }
    @Transient
    public int getType()
    {
        return Constants.BITSTREAM;
    }
    @ManyToOne
    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }
    @Transient
    public boolean isModifiedMetadata() {
        return modifiedMetadata;
    }
    @Transient
    public boolean isModified() {
        return modified;
    }

}
