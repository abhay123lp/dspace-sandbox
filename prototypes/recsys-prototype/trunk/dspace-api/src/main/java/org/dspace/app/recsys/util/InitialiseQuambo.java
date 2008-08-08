/*
 * InitialiseQuambo.java
 *
 * Version: $Revision $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.recsys.util;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAO;
import org.dspace.app.recsys.researchContext.dao.ResearchContextDAOFactory;
import org.dspace.app.recsys.researchContext.ResearchContext;

import java.util.List;
import java.io.FileReader;

/**
 * <code>InitialiseQuambo</code> lets repository administrators initialise the
 * recommender system for their DSpace installation.
 *
 * @author Desmond Elliott
 */
public class InitialiseQuambo {

    public static EPersonDAO epersonDAO;
    public static ResearchContextDAO researchContextDAO;
    public static String recsysSchema =
            ConfigurationManager.getProperty("dspace.dir") +
                    "/etc/recsys-schema.sql";

    /**
     * Loads the Quambo SQL schema into the database and for each
     * <code>EPerson</code> creates a <code>ResearchContext</code> and all
     * required structures to support a <code>ResearchContext</code>.
     *
     * @param args command line arguments, not used.
     */
    public static void main(String[] args)
    {
        try
        {
            Context context = new Context();
            context.setIgnoreAuthorization(true);

            DatabaseManager.loadSql(new FileReader(recsysSchema));

            epersonDAO = EPersonDAOFactory.getInstance(context);
            researchContextDAO = ResearchContextDAOFactory.getInstance(context);

            List<EPerson> allEPersons = epersonDAO.getEPeople(EPerson.ID);
            for (EPerson e: allEPersons)
            {
                String uri = ConfigurationManager.getProperty("dspace.url")
                        + "/atom/essence/";

                ResearchContext r = researchContextDAO.create(uri);
                r.setEperson(e);
                r.setName("Initial Context");
                researchContextDAO.update(r);

                TableRow row = DatabaseManager.create(context,
                                                      "quambo_eperson");

                row.setColumn("eperson_id", e.getID());
                row.setColumn("initial_research_context_uuid",
                              r.getUUID().toString());

                row.setColumn("last_research_context_uuid",
                              r.getUUID().toString());

                DatabaseManager.update(context, row);
                context.commit();                
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}