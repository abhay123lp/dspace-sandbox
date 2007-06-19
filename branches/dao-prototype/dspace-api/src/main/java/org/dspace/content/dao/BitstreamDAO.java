/*
 * BitstreamDAO.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.content.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Bitstream;
import org.dspace.content.proxy.BitstreamProxy;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;

/**
 * @author James Rutherford
 */
public abstract class BitstreamDAO extends ContentDAO
{
    protected Logger log = Logger.getLogger(BitstreamDAO.class);

    protected Context context;

    public abstract Bitstream create() throws AuthorizeException;

    public Bitstream retrieve(int id)
    {
        return (Bitstream) context.fromCache(Bitstream.class, id);
    }

    public Bitstream retrieve(UUID uuid)
    {
    }

    public void update(Bitstream bitstream) throws AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(context, this, Constants.WRITE);

        log.info(LogManager.getHeader(context, "update_bitstream",
                "bitstream_id=" + getID()));
    }

    public void delete(int id) throws AuthorizeException
    {
        Bitstream bitstream = retrieve(id);
        this.update(bitstream); // Sync in-memory object with db before removal

        // changed to a check on remove
        // Check authorisation
        //AuthorizeManager.authorizeAction(context, this, Constants.DELETE);
        log.info(LogManager.getHeader(context, "delete_bitstream",
                "bitstream_id=" + getID()));

        // Remove from cache
        context.removeCached(this, getID());

        // Remove policies
        AuthorizeManager.removeAllPolicies(context, this);
    }

    public abstract List<Bitstream> getBitstreamsByBundle(Bundle bundle);
    public abstract List<Bitstream> getBitstreamsByItem(Item bitstream);
}
