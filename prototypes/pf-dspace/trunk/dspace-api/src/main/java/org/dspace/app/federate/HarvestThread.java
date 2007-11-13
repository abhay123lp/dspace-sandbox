/*
 * HarvestThread.java
 *
 * Version: $Revision: 1.7 $
 *
 * Date: $Date: 2006/07/30 03:57:56 $
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
package org.dspace.app.federate;

import java.io.IOException;
import java.util.Date;

import org.jdom.JDOMException;

import org.dspace.app.mediafilter.MediaFilterManager;
import org.dspace.app.federate.OAIRepository;
import org.dspace.app.federate.dao.RemoteRepositoryDAO;
import org.dspace.app.federate.dao.postgres.RemoteRepositoryDAOPostgres;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
//import org.dspace.handle.HandleManager;

import org.apache.log4j.Logger;

/**
 *
 *
 * @author Talent
 * @version $Revision: 1.7 $
 */
public class HarvestThread extends Thread
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HarvestThread.class);

    private volatile boolean stopHarvest;

    private Thread runThread;

    private String epersonEmail;

    private Date date;

    private int[] repositoryIDs;

    public HarvestThread(EPerson e)
    {
        super();
        epersonEmail = e.getEmail();
    }

    public void run()
    {
        runThread = Thread.currentThread();
        stopHarvest = false;

        try
        {
            Context c = new Context();
            c.setCurrentUser(EPerson.findByEmail(c, epersonEmail));

			RemoteRepositoryDAO dao = new RemoteRepositoryDAOPostgres(c);
            ItemReplicator replicator = new ItemReplicator(c);
            
            for (int i = 0; i < repositoryIDs.length; i++)
            {
                if (stopHarvest)
                {
                    break;
                }
                RemoteRepository rr = dao.retrieve(repositoryIDs[i]);

				try
				{
					// This is a simple 'ping' test to see if we can reach the
					// repository. If for some reason we can't connect, this
					// operation will fail, as it performs an Identify request
					// on the remote repository. We also take this opportunity
					// to update the list of metadata formats supported by the
					// repository (this information only ever exists in
					// memory).
					OAIRepository oair = new OAIRepository(rr.getBaseURL());
					rr.setDateLastSeen(date);
					rr.setAlive(true);
					rr.setMetadataFormats(oair.getMetadataFormats());
					replicator.replicateFrom(rr, date);
				}
				catch (IOException ioe)
				{
					rr.setAlive(false);
					log.error("HarvestThread.run() : ", ioe);
					continue;
				}
				catch (JDOMException jdome)
				{
					rr.setAlive(false);
					log.error("HarvestThread.run() : ", jdome);
					continue;
				}
            }

            c.complete();

            // Update Handle records
//            c = new Context();
//            HandleManager.syncHandles(c);
//            c.complete();

            // Create thumbnails, do not recreate search index
            // This was crashing tomcat, so it got commented out :) --JR
            //String[] args = { "-n" };
            //MediaFilterManager.main(args);

			// FIXME: maybe some pretty-printed stats for how many records were
			// retrieved, how many failed imports, etc.
			log.info("Harvest operation completed successfully.");
            Thread.currentThread().interrupt();
        }
        catch (Throwable t)
        {
            log.error("HarvestThread.run() : ", t);
            System.err.println(t.getLocalizedMessage());
        }
    }

    public void stopHarvest()
    {
        log.info("stopping harvest");
        stopHarvest = true;

        if (runThread != null)
        {
            runThread.interrupt();
        }
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public int[] getRepositories()
    {
        return repositoryIDs;
    }

    public void setRepositories(int[] repositoryIDs)
    {
        this.repositoryIDs = repositoryIDs;
    }
}
