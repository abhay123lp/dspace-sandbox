/**
 * RemoteRepositoryDAO.java
 *
 * Version: $Id$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.federate.dao;

import org.dspace.app.federate.MetadataFormat;
import org.dspace.app.federate.OAIRepository;
import org.dspace.app.federate.RemoteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public abstract class RemoteRepositoryDAO
{
    public abstract RemoteRepository create();

    public RemoteRepository create(RemoteRepository rr)
    {
        rr.setName("");
		try
		{
			rr.setBaseURL(new URL(""));
		}
		catch (MalformedURLException mue) { }

        Date date = new Date();
		rr.setAdminEmail("");
		rr.setActive(false);
		rr.setPublic(false);
		rr.setAlive(true);
		rr.setDateAdded(date);
		rr.setDateLastSeen(date);
		rr.setDateLastHarvested(new Date(0));
		rr.setMetadataFormats(new ArrayList<MetadataFormat>());
		rr.setFailedImports(new ArrayList<String>());

		return rr;
    }
    public abstract RemoteRepository create(OAIRepository oair);
	public abstract RemoteRepository retrieve(int repoID);
	public abstract RemoteRepository retrieve(UUID uuid);
    public abstract RemoteRepository retrieve(URL url);
    public abstract List<RemoteRepository> getRemoteRepositories();
	public abstract List<RemoteRepository> getActiveRemoteRepositories();
	public abstract List<RemoteRepository> getPublicRemoteRepositories();
	public abstract void update(RemoteRepository rr);
	public abstract void delete(int repoID);
}

