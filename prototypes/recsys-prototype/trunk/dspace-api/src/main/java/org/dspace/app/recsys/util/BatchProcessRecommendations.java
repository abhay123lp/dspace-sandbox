/*
 * BatchProcessRecommendations.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2007, Hewlett-Packard Company and Massachusetts
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

import org.dspace.app.recsys.recommendation.dao.Item2ItemRecommendationDAO;
import org.dspace.app.recsys.recommendation.dao.Item2ItemRecommendationDAOFactory;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAO;
import org.dspace.app.recsys.recommendation.dao.Item2ResearchContextRecommendationDAOFactory;
import org.dspace.core.Context;
import org.apache.log4j.Logger;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

/**
 * <code>BatchProcessRecommendations</code> performs a batch process of
 * Item-to-Item recommendation calcualtions for each <code>Item</code> in the
 * repository.
 *
 * TODO: Consider a multi-threaded approach for capable systems, bit at which
 * level should this be threaded? It will be advantageous to actually multi-
 * thread it at the per determining recommendations for each individual item
 * basis.
 *
 * TODO: Implement a priority based batch processing system
 *
 * @author Desmond Elliott
 */
public class BatchProcessRecommendations
{

    private static Item2ItemRecommendationDAO i2irDAO;
    private static Item2ResearchContextRecommendationDAO i2rcrDAO;
    public static Logger log =
            Logger.getLogger(BatchProcessRecommendations.class);

    /**
     * Calculates the Item-to-Item recommendations as part of a batch process.
     * If the command line argument is -all, recommendations for all items
     * are calculated; if the arugment is -from-deferred-queue, the item IDs
     * and 'votes' are retrieved from the database and a PriorityQueue is
     * created which determines recommendations for the most accessed items
     * to the least accessed items. 
     *
     * @param args either <code>-i -a</code> or <code>-r -a</code>
     */
    public static void main(String[] args)
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "item", false, "determine recommendations for" +
                                              " Items");
        options.addOption("r", "research-contexts", false, "determine " +
                                                           "recommendations " +
                                                           "for Research" +
                                                           " Contexts");        
        options.addOption("a", "all", false, "determine recommendations for" +
                                             " all Items or Research Contexts");
        try
        {
            CommandLine line = parser.parse(options, args);

            Context context = new Context();
            context.setIgnoreAuthorization(true);

            if (line.hasOption("i") && line.hasOption("a"))
            {
                i2irDAO = Item2ItemRecommendationDAOFactory.getInstance(context);
                i2irDAO.calculateAllRecommendations();
            }
            else if (line.hasOption("r") && line.hasOption("a"))
            {
                i2rcrDAO =
              Item2ResearchContextRecommendationDAOFactory.getInstance(context);
                i2rcrDAO.calculateAllRecommendations();
            }
            else if (line.hasOption("i") & line.hasOption("r") ||
                     !line.hasOption("i") & !line.hasOption("r"))
            {
                System.out.println("You must use either -i or -r");
                System.exit(1);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
