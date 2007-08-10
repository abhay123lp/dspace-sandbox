/*
 * LogAnalyser.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.app.statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Long;
import java.lang.StringBuffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import org.dspace.app.statistics.LogLine;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * This class performs all the actual analysis of a given set of DSpace log
 * files.  Most input can be configured; use the -help flag for a full list
 * of usage information.
 *
 * The output of this file is plain text and forms an "aggregation" file which
 * can then be used for display purposes using the related ReportGenerator
 * class.
 *
 * @author  Richard Jones
 */
public class LogAnalyser
{

    // set up our class globals
    // FIXME: there are so many of these perhaps they should exist in a static
    // object of their own

    /////////////////
    // aggregators
    /////////////////

    /** aggregator for all actions performed in the system */
    private static Map actionAggregator = new HashMap();

    /** aggregator for all searches performed */
    private static Map searchAggregator = new HashMap();

    /** aggregator for user logins */
    private static Map userAggregator = new HashMap();

    /** aggregator for item views */
    private static Map itemAggregator = new HashMap();

    /** aggregator for current archive state statistics */
    private static Map archiveStats = new HashMap();

    /** warning counter */
    private static int warnCount = 0;

    /** log line counter */
    private static int lineCount = 0;

    //////////////////
    // config data
    //////////////////

    /** list of actions to be included in the general summary */
    private static List generalSummary = new ArrayList();

    /** list of words not to be aggregated */
    private static List excludeWords = new ArrayList();

    /** list of search types to be ignored, such as "author:" */
    private static List excludeTypes = new ArrayList();

    /** list of characters to be excluded */
    private static List excludeChars = new ArrayList();

    /** list of item types to be reported on in the current state */
    private static List itemTypes = new ArrayList();

    /** bottom limit to output for search word analysis */
    private static int searchFloor;

    /** bottom limit to output for item view analysis */
    private static int itemFloor;

    /** number of items from most popular to be looked up in the database */
    private static int itemLookup;

    /** mode to use for user email display */
    private static String userEmail;

    /** URL of the service being analysed */
    private static String url;

    /** Name of the service being analysed */
    private static String name;

    /** Name of the service being analysed */
    private static String hostName;

    /** the average number of views per item */
    private static int views = 0;

    ///////////////////////
    // regular expressions
    ///////////////////////

   /** Exclude characters regular expression pattern */
   private static Pattern excludeCharRX = null;

   /** URI indicator string regular expression pattern */
   private static Pattern uriRX = null;

   /** item id indicator string regular expression pattern */
   private static Pattern itemRX = null;

   /** query string indicator regular expression pattern */
   private static Pattern queryRX = null;

   /** collection indicator regular expression pattern */
   private static Pattern collectionRX = null;

   /** community indicator regular expression pattern */
   private static Pattern communityRX = null;

   /** results indicator regular expression pattern */
   private static Pattern resultsRX = null;

   /** single character regular expression pattern */
   private static Pattern singleRX = null;

   /** a pattern to match a valid version 1.3 log file line */
   private static Pattern valid13 = null;

   /** a pattern to match a valid version 1.4 log file line */
   private static Pattern valid14 = null;

   /** pattern to match valid log file names */
   private static Pattern logRegex = null;

   /** pattern to match commented out lines from the config file */
   private static Pattern comment = Pattern.compile("^#");

   /** pattern to match genuine lines from the config file */
   private static Pattern real = Pattern.compile("^(.+)=(.+)");

   /** pattern to match all search types */
   private static Pattern typeRX = null;

   /** pattern to match all search types */
   private static Pattern wordRX = null;

   //////////////////////////
   // Miscellaneous variables
   //////////////////////////

   /** process timing clock */
   private static Calendar startTime = null;

   /////////////////////////
   // command line options
   ////////////////////////

   /** the log directory to be analysed */
   private static String logDir = ConfigurationManager.getProperty("dspace.dir") +
                        File.separator + "log";

   /** the regex to describe the file name format */
   private static String fileTemplate = "dspace\\.log.*";

   /** the config file from which to configure the analyser */
   private static String configFile = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "config" + File.separator +
                            "dstat.cfg";

   /** the output file to which to write aggregation data */
   private static String outFile = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "log" + File.separator + "dstat.dat";

   /** the starting date of the report */
   private static Date startDate = null;

   /** the end date of the report */
   private static Date endDate = null;

   /** the starting date of the report as obtained from the log files */
   private static Date logStartDate = null;

   /** the end date of the report as obtained from the log files */
   private static Date logEndDate = null;

   /** are we looking stuff up in the database */
   private static boolean lookUp = false;


    /**
     * main method to be run from command line.  See usage information for
     * details as to how to use the command line flags (-help)
     */
    public static void main(String [] argv)
        throws Exception, SQLException
    {
        // first, start the processing clock
        startTime = new GregorianCalendar();

        // create context as super user
        Context context = new Context();
        context.setIgnoreAuthorization(true);

        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        String myOutFile = null;
        Date myStartDate = null;
        Date myEndDate = null;
        boolean myLookUp = false;

        // read in our command line options
        for (int i = 0; i < argv.length; i++)
        {
            if (argv[i].equals("-log"))
            {
                myLogDir = argv[i+1];
            }

            if (argv[i].equals("-file"))
            {
                myFileTemplate = argv[i+1];
            }

            if (argv[i].equals("-cfg"))
            {
                myConfigFile = argv[i+1];
            }

            if (argv[i].equals("-out"))
            {
                myOutFile = argv[i+1];
            }

            if (argv[i].equals("-help"))
            {
                LogAnalyser.usage();
                System.exit(0);
            }

            if (argv[i].equals("-start"))
            {
                myStartDate = parseDate(argv[i+1]);
            }

            if (argv[i].equals("-end"))
            {
                myEndDate = parseDate(argv[i+1]);
            }

            if (argv[i].equals("-lookup"))
            {
                myLookUp = true;
            }
        }

        // now call the method which actually processes the logs
        processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile, myStartDate, myEndDate, myLookUp);
    }

    /**
     * using the pre-configuration information passed here, analyse the logs
     * and produce the aggregation file
     *
     * @param   context     the DSpace context object this occurs under
     * @param   myLogDir    the passed log directory.  Uses default if null
     * @param   myFileTemplate  the passed file name regex.  Uses default if null
     * @param   myConfigFile    the DStat config file.  Uses default if null
     * @param   myOutFile    the file to which to output aggregation data.  Uses default if null
     * @param   myStartDate     the desired start of the analysis.  Starts from the beginning otherwise
     * @param   myEndDate       the desired end of the analysis.  Goes to the end otherwise
     * @param   myLookUp        force a lookup of the database
     */
    public static void processLogs(Context context, String myLogDir,
                                    String myFileTemplate, String myConfigFile,
                                    String myOutFile, Date myStartDate,
                                    Date myEndDate, boolean myLookUp)
        throws IOException, SQLException
    {
        // FIXME: perhaps we should have all parameters and aggregators put
        // together in a single aggregating object

        // if the timer has not yet been started, then start it
        if (startTime != null)
        {
            startTime = new GregorianCalendar();
        }

        // set the parameters for this analysis
        setParameters(myLogDir, myFileTemplate, myConfigFile, myOutFile, myStartDate, myEndDate, myLookUp);

        // pre prepare our standard file readers and buffered readers
        FileReader fr = null;
        BufferedReader br = null;

        // read in the config information, throwing an error if we fail to open
        // the given config file
        readConfig(configFile);

        // assemble the regular expressions for later use (requires the file
        // template to build the regex to match it
        setRegex(fileTemplate);

        // get the log files
        File[] logFiles = getLogFiles(logDir);

        // standard loop counter
        int i = 0;

        // for every log file do analysis
        // FIXME: it is easy to implement not processing log files after the
        // dates exceed the end boundary, but is there an easy way to do it
        // for the start of the file?  Note that we can assume that the contents
        // of the log file are sequential, but can we assume the files are
        // provided in a data sequence?
        for (i = 0; i < logFiles.length; i++)
        {
            // check to see if this file is a log file agains the global regex
            Matcher matchRegex = logRegex.matcher(logFiles[i].getName());
            if (matchRegex.matches())
            {
                // if it is a log file, open it up and lets have a look at the
                // contents.
                try
                {
                    fr = new FileReader(logFiles[i].toString());
                    br = new BufferedReader(fr);
                }
                catch (IOException e)
                {
                    System.out.println("Failed to read log file " + logFiles[i].toString());
                    System.exit(0);
                }

                // for each line in the file do the analysis
                // FIXME: perhaps each section needs to be dolled out to an
                // analysing class to allow pluggability of other methods of
                // analysis, and ease of code reading too - Pending further thought
                String line = null;
                while ((line = br.readLine()) != null)
                {
                    // get the log line object
                    LogLine logLine = getLogLine(line);

                    // if there are line segments get on with the analysis
                    if (logLine != null)
                    {
                        // first find out if we are constraining by date and
                        // if so apply the restrictions
                        if (logLine.beforeDate(startDate))
                        {
                            continue;
                        }

                        if (logLine.afterDate(endDate))
                        {
                            break;
                        }

                        // count the number of lines parsed
                        lineCount++;

                        // if we are not constrained by date, register the date
                        // as the start/end date if it is the earliest/latest so far
                        // FIXME: this should probably have a method of its own
                        if (startDate == null)
                        {
                            if (logStartDate != null)
                            {
                                if (logLine.beforeDate(logStartDate))
                                {
                                    logStartDate = logLine.getDate();
                                }
                            }
                            else
                            {
                                logStartDate = logLine.getDate();
                            }
                        }

                        if (endDate == null)
                        {
                            if (logEndDate != null)
                            {
                                if (logLine.afterDate(logEndDate))
                                {
                                    logEndDate = logLine.getDate();
                                }
                            }
                            else
                            {
                                logEndDate = logLine.getDate();
                            }
                        }

                        // count the warnings
                        if (logLine.isLevel("WARN"))
                        {
                            // FIXME: really, this ought to be some kind of level
                            // aggregator
                            warnCount++;
                        }

                        // is the action a search?
                        if (logLine.isAction("search"))
                        {
                            // get back all the valid search words from the query
                            String[] words = analyseQuery(logLine.getParams());

                            // for each search word add to the aggregator or
                            // increment the aggregator's counter
                            for (int j = 0; j < words.length; j++)
                            {
                                // FIXME: perhaps aggregators ought to be objects
                                // themselves
                                searchAggregator.put(words[j], increment(searchAggregator, words[j]));
                            }
                        }

                        // is the action a login, and are we counting user logins?
                        if (logLine.isAction("login") && !userEmail.equals("off"))
                        {
                            userAggregator.put(logLine.getUser(), increment(userAggregator, logLine.getUser()));
                        }

                        // is the action an item view?
                        if (logLine.isAction("view_item"))
                        {
                            String uri = logLine.getParams();

                            // strip the uri string
                            Matcher matchURI = uriRX.matcher(uri);
                            uri = matchURI.replaceAll("");

                            // strip the item id string
                            Matcher matchItem = itemRX.matcher(uri);
                            uri = matchItem.replaceAll("");

                            uri.trim();

                            // either add the uri to the aggregator or
                            // increment its counter
                            itemAggregator.put(uri, increment(itemAggregator, uri));
                        }

                        // log all the activity
                        actionAggregator.put(logLine.getAction(), increment(actionAggregator, logLine.getAction()));
                    }
                }

                // close the file reading buffers
                br.close();
                fr.close();

            }
        }

        // do we want to do a database lookup?  Do so only if the start and
        // end dates are null or lookUp is true
        // FIXME: this is a kind of separate section.  Would it be worth building
        // the summary string separately and then inserting it into the real
        // summary later?  Especially if we make the archive analysis more complex
        archiveStats.put("All Items", getNumItems(context));
        for (i = 0; i < itemTypes.size(); i++)
        {
            archiveStats.put(itemTypes.get(i), getNumItems(context, (String) itemTypes.get(i)));
        }

        // now do the host lookup
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();
        }
        catch (UnknownHostException e)
        {
            hostName = "unknown host";
        }

        // do the average views analysis
        if (((Integer) archiveStats.get("All Items")).intValue() != 0)
        {
            // FIXME: this is dependent on their being a query on the db, which
            // there might not always be if it becomes configurable
            Double avg = new Double(
                        Math.ceil(
                            ((Integer) actionAggregator.get("view_item")).intValue() /
                            ((Integer) archiveStats.get("All Items")).intValue()));
            views = avg.intValue();
        }

        // finally, write the output
        createOutput();

        return;
    }


    /**
     * set the passed parameters up as global class variables.  This has to
     * be done in a separate method because the API permits for running from
     * the command line with args or calling the processLogs method statically
     * from elsewhere
     *
     * @param   myLogDir    the log file directory to be analysed
     * @param   myFileTemplate  regex for log file names
     * @param   myConfigFile    config file to use for dstat
     * @param   myOutFile   file to write the aggregation into
     * @param   myStartDate requested log reporting start date
     * @param   myEndDate   requested log reporting end date
     * @param   myLookUp    requested look up force flag
     */
    public static void setParameters(String myLogDir, String myFileTemplate,
                                    String myConfigFile, String myOutFile,
                                    Date myStartDate, Date myEndDate,
                                    boolean myLookUp)
    {
        if (myLogDir != null)
        {
            logDir = myLogDir;
        }

        if (myFileTemplate != null)
        {
            fileTemplate = myFileTemplate;
        }

        if (myConfigFile != null)
        {
            configFile = myConfigFile;
        }

        if (myStartDate != null)
        {
            startDate = myStartDate;
        }

        if (myEndDate != null)
        {
            endDate = myEndDate;
        }

        if (myLogDir != null)
        {
            lookUp = myLookUp;
        }

        if (myOutFile != null)
        {
            outFile = myOutFile;
        }

        return;
    }


    /**
     * generate the analyser's output to the specified out file
     */
    public static void createOutput()
    {
        // start a string buffer to hold the final output
        StringBuffer summary = new StringBuffer();

        // define an iterator that will be used to go over the hashmap keys
        Iterator keys = null;

        // output the number of lines parsed
        summary.append("log_lines=" + Integer.toString(lineCount) + "\n");

        // output the number of warnings encountered
        summary.append("warnings=" + Integer.toString(warnCount) + "\n");

        // set the general summary config up in the aggregator file
        for (int i = 0; i < generalSummary.size(); i++)
        {
            summary.append("general_summary=" + generalSummary.get(i) + "\n");
        }

        // output the host name
        summary.append("server_name=" + hostName + "\n");

        // output the service name
        summary.append("service_name=" + name + "\n");

        // output the date information if necessary
        SimpleDateFormat sdf = new SimpleDateFormat("dd'/'MM'/'yyyy");

        if (startDate != null)
        {
            summary.append("start_date=" + sdf.format(startDate) + "\n");
        }
        else if (logStartDate != null)
        {
            summary.append("start_date=" + sdf.format(logStartDate) + "\n");
        }

        if (endDate != null)
        {
            summary.append("end_date=" + sdf.format(endDate) + "\n");
        }
        else if (logEndDate != null)
        {
            summary.append("end_date=" + sdf.format(logEndDate) + "\n");
        }

        // write out the archive stats
        keys = archiveStats.keySet().iterator();
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            summary.append("archive." + key + "=" + archiveStats.get(key) + "\n");
        }

        // write out the action aggregation results
        keys = actionAggregator.keySet().iterator();
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            summary.append("action." + key + "=" + actionAggregator.get(key) + "\n");
        }

        // depending on the config settings for reporting on emails output the
        // login information
        summary.append("user_email=" + userEmail + "\n");
        int address = 1;
        keys = userAggregator.keySet().iterator();

        // for each email address either write out the address and the count
        // or alias it with an "Address X" label, to keep the data confidential
        // FIXME: the users reporting should also have a floor value
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            summary.append("user.");
            if (userEmail.equals("on"))
            {
                summary.append(key + "=" + userAggregator.get(key) + "\n");
            }
            else if (userEmail.equals("alias"))
            {
                summary.append("Address " + Integer.toString(address++) + "=" + userAggregator.get(key) + "\n");
            }
        }

        // FIXME: all values which have floors set should provide an "other"
        // record which counts how many other things which didn't make it into
        // the listing there are

        // output the search word information
        summary.append("search_floor=" + searchFloor + "\n");
        keys = searchAggregator.keySet().iterator();
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            if (((Integer) searchAggregator.get(key)).intValue() >= searchFloor)
            {
                summary.append("search." + key + "=" + searchAggregator.get(key) + "\n");
            }
        }

        // FIXME: we should do a lot more with the search aggregator
        // Possible feature list:
        //  - constrain by collection/community perhaps?
        //  - we should consider building our own aggregator class which can
        //      be full of rich data.  Perhaps this and the Stats class should
        //      be the same thing.

        // item viewing information
        summary.append("item_floor=" + itemFloor + "\n");
        summary.append("host_url=" + url + "\n");
        summary.append("item_lookup=" + itemLookup + "\n");

        // write out the item access information
        keys = itemAggregator.keySet().iterator();
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            if (((Integer) itemAggregator.get(key)).intValue() >= itemFloor)
            {
                summary.append("item." + key + "=" + itemAggregator.get(key) + "\n");
            }
        }

        // output the average views per item
        if (views > 0)
        {
            summary.append("avg_item_views=" + views + "\n");
        }

        // insert the analysis processing time information
        Calendar endTime = new GregorianCalendar();
        long timeInMillis = (endTime.getTimeInMillis() - startTime.getTimeInMillis());
        summary.append("analysis_process_time=" + Long.toString(timeInMillis / 1000) + "\n");

        // finally write the string into the output file
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
            out.write(summary.toString());
            out.close();
        }
        catch (IOException e)
        {
            System.out.println("Unable to write to output file " + outFile);
            System.exit(0);
        }

        return;
    }


    /**
     * get an array of file objects representing the passed log directory
     *
     * @param   logDir  the log directory in which to pick up files
     *
     * @return  an array of file objects representing the given logDir
     */
    public static File[] getLogFiles(String logDir)
    {
        // open the log files directory, read in the files, check that they
        // match the passed regular expression then analyse the content
        File logs = new File(logDir);

        // if log dir is not a directory throw and error and exit
        if (!logs.isDirectory())
        {
            System.out.println("Passed log directory is not a directory");
            System.exit(0);
        }

        // get the files in the directory
        return logs.listFiles();
    }


    /**
     * set up the regular expressions to be used by this analyser.  Mostly this
     * exists to provide a degree of segregation and readability to the code
     * and to ensure that you only need to set up the regular expressions to
     * be used once
     *
     * @param   fileTemplate    the regex to be used to identify dspace log files
     */
    public static void setRegex(String fileTemplate)
    {
        // build the exclude characters regular expression
        StringBuffer charRegEx = new StringBuffer();
        charRegEx.append("[");
        for (int i = 0; i < excludeChars.size(); i++)
        {
            charRegEx.append("\\" + (String) excludeChars.get(i));
        }
        charRegEx.append("]");
        excludeCharRX = Pattern.compile(charRegEx.toString());

        // regular expression to find URI indicators in strings
        uriRX = Pattern.compile("uri=");

        // regular expression to find item_id indicators in strings
        itemRX = Pattern.compile(",item_id=.*$");

        // regular expression to find query indicators in strings
        queryRX = Pattern.compile("query=");

        // regular expression to find collections in strings
        collectionRX = Pattern.compile("collection_id=[0-9]*,");

        // regular expression to find communities in strings
        communityRX = Pattern.compile("community_id=[0-9]*,");

        // regular expression to find search result sets
        resultsRX = Pattern.compile(",results=(.*)");

        // regular expressions to find single characters anywhere in the string
        singleRX = Pattern.compile("( . |^. | .$)");

        // set up the standard log file line regular expression
        String logLine13 = "^(\\d\\d\\d\\d-\\d\\d\\-\\d\\d) \\d\\d:\\d\\d:\\d\\d,\\d\\d\\d (\\w+)\\s+\\S+ @ ([^:]+):[^:]+:([^:]+):(.*)";
        String logLine14 = "^(\\d\\d\\d\\d-\\d\\d\\-\\d\\d) \\d\\d:\\d\\d:\\d\\d,\\d\\d\\d (\\w+)\\s+\\S+ @ ([^:]+):[^:]+:[^:]+:([^:]+):(.*)";
        valid13 = Pattern.compile(logLine13);
        valid14 = Pattern.compile(logLine14);

        // set up the pattern for validating log file names
        logRegex = Pattern.compile(fileTemplate);

        // set up the pattern for matching any of the query types
        StringBuffer typeRXString = new StringBuffer();
        typeRXString.append("(");
        for (int i = 0; i < excludeTypes.size(); i++)
        {
            if (i > 0)
            {
                typeRXString.append("|");
            }
            typeRXString.append((String) excludeTypes.get(i));
        }
        typeRXString.append(")");
        typeRX = Pattern.compile(typeRXString.toString());

        // set up the pattern for matching any of the words to exclude
        StringBuffer wordRXString = new StringBuffer();
        wordRXString.append("(");
        for (int i = 0; i < excludeWords.size(); i++)
        {
            if (i > 0)
            {
                wordRXString.append("|");
            }
            wordRXString.append(" " + (String) excludeWords.get(i) + " ");
            wordRXString.append("|");
            wordRXString.append("^" + (String) excludeWords.get(i) + " ");
            wordRXString.append("|");
            wordRXString.append(" " + (String) excludeWords.get(i) + "$");
        }
        wordRXString.append(")");
        wordRX = Pattern.compile(wordRXString.toString());

        return;
    }


    /**
     * read in the given config file and populate the class globals
     *
     * @param   configFile  the config file to read in
     */
    public static void readConfig(String configFile)
        throws IOException
    {
        // prepare our standard file readers and buffered readers
        FileReader fr = null;
        BufferedReader br = null;

        String record = null;
        try
        {
            fr = new FileReader(configFile);
            br = new BufferedReader(fr);
        }
        catch (IOException e)
        {
            System.out.println("Failed to read config file");
            System.exit(0);
        }

        // read in the config file and set up our instance variables
        while ((record = br.readLine()) != null)
        {
            // check to see what kind of line we have
            Matcher matchComment = comment.matcher(record);
            Matcher matchReal = real.matcher(record);

            // if the line is not a comment and is real, read it in
            if (!matchComment.matches() && matchReal.matches())
            {
                // lift the values out of the matcher's result groups
                String key = matchReal.group(1).trim();
                String value = matchReal.group(2).trim();

                // read the config values into our instance variables (see
                // documentation for more info on config params)
                if (key.equals("general.summary"))
                {
                    actionAggregator.put(value, new Integer(0));
                    generalSummary.add(value);
                }

                if (key.equals("exclude.word"))
                {
                    excludeWords.add(value);
                }

                if (key.equals("exclude.type"))
                {
                    excludeTypes.add(value);
                }

                if (key.equals("exclude.character"))
                {
                    excludeChars.add(value);
                }

                if (key.equals("item.type"))
                {
                    itemTypes.add(value);
                }

                if (key.equals("item.floor"))
                {
                    itemFloor = Integer.parseInt(value);
                }

                if (key.equals("search.floor"))
                {
                    searchFloor = Integer.parseInt(value);
                }

                if (key.equals("item.lookup"))
                {
                    itemLookup = Integer.parseInt(value);
                }

                if (key.equals("user.email"))
                {
                    userEmail = value;
                }

                if (key.equals("host.url"))
                {
                    url = value;
                }

                if (key.equals("host.name"))
                {
                    name = value;
                }
            }
        }

        // close the inputs
        br.close();
        fr.close();

        return;
    }

    /**
     * increment the value of the given map at the given key by one.
     *
     * @param   map     the map whose value we want to increase
     * @param   key     the key of the map whose value to increase
     *
     * @return          an integer object containing the new value
     */
    public static Integer increment(Map map, String key)
    {
        Integer newValue = null;
        if (map.containsKey(key))
        {
            // FIXME: this seems like a ridiculous way to add Integers
            newValue = new Integer(((Integer) map.get(key)).intValue() + 1);
        }
        else
        {
            newValue = new Integer(1);
        }
        return newValue;
    }

    /**
     * Take the standard date string requested at the command line and convert
     * it into a Date object.  Throws and error and exits if the date does
     * not parse
     *
     * @param   date    the string representation of the date
     *
     * @return          a date object containing the date, with the time set to
     *                  00:00:00
     */
    public static Date parseDate(String date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd");
        Date parsedDate = null;

        try
        {
             parsedDate = sdf.parse(date);
        }
        catch (ParseException e)
        {
            System.out.println("The date is not in the correct format");
            System.exit(0);
        }
        return parsedDate;
    }

    /**
     * Take a search query string and pull out all of the meaningful information
     * from it, giving the results in the form of a String array, a single word
     * to each element
     *
     * @param   query   the search query to be analysed
     *
     * @return          the string array containing meaningful search terms
     */
    public static String[] analyseQuery(String query)
    {
        // register our standard loop counter
        int i = 0;

        // make the query string totally lower case, to ensure we don't miss out
        // on matches due to capitalisation
        query = query.toLowerCase();

        // now perform successive find and replace operations using pre-defined
        // global regular expressions
        Matcher matchQuery = queryRX.matcher(query);
        query = matchQuery.replaceAll(" ");

        Matcher matchCollection = collectionRX.matcher(query);
        query = matchCollection.replaceAll(" ");

        Matcher matchCommunity = communityRX.matcher(query);
        query = matchCommunity.replaceAll(" ");

        Matcher matchResults = resultsRX.matcher(query);
        query = matchResults.replaceAll(" ");

        Matcher matchTypes = typeRX.matcher(query);
        query = matchTypes.replaceAll(" ");

        Matcher matchChars = excludeCharRX.matcher(query);
        query = matchChars.replaceAll(" ");

        Matcher matchWords = wordRX.matcher(query);
        query = matchWords.replaceAll(" ");

        Matcher single = singleRX.matcher(query);
        query = single.replaceAll(" ");

        // split the remaining string by whitespace, trim and stuff into an
        // array to be returned
        StringTokenizer st = new StringTokenizer(query);
        String[] words = new String[st.countTokens()];
        for (i = 0; i < words.length; i++)
        {
            words[i] = st.nextToken().trim();
        }

        // FIXME: some single characters are still slipping through the net;
        // why? and how do we fix it?
        return words;
    }


    /**
     * split the given line into it's relevant segments if applicable (i.e. the
     * line matches the required regular expression.
     *
     * @param   line    the line to be segmented
     * @return          a Log Line object for the given line
     */
    public static LogLine getLogLine(String line)
    {
        // FIXME: consider moving this code into the LogLine class.  To do this
        // we need to much more carefully define the structure and behaviour
        // of the LogLine class
        Matcher match;

        if (line.indexOf(":ip_addr") > 0)
        {
            match = valid14.matcher(line);
        }
        else
        {
            match = valid13.matcher(line);
        }

        if (match.matches())
        {
            // set up a new log line object
            LogLine logLine = new LogLine(parseDate(match.group(1).trim()),
                                          match.group(2).trim(),
                                          match.group(3).trim(),
                                          match.group(4).trim(),
                                          match.group(5).trim());

            return logLine;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the number of items in the archive which were accessioned between
     * the provided start and end dates, with the given value for the DC field
     * 'type' (unqualified)
     *
     * @param   context     the DSpace context for the action
     * @param   type        value for DC field 'type' (unqualified)
     *
     * @return              an integer containing the relevant count
     */
    public static Integer getNumItems(Context context, String type)
        throws SQLException
    {
        // FIXME: we don't yet collect total statistics, such as number of items
        // withdrawn, number in process of submission etc.  We should probably do
        // that

        ItemDAO dao = ItemDAOFactory.getInstance(context);
        List<Item> items = null;

        if ((type == null) || type.equals(""))
        {
            items = dao.getItems();
        }
        else
        {
            MetadataSchema schema = MetadataSchema.find(context, "dc");
            MetadataField field =
                new MetadataField(schema, "type", null, null);
            MetadataValue value = new MetadataValue(field);
            value.setValue(type);

            items = dao.getItems(field, value);
        }

        return items.size();
    }


    /**
     * get the total number of items in the archive at time of execution,
     * ignoring all other constraints
     *
     * @param   context     the DSpace context the action is being performed in
     *
     * @return              an Integer containing the number of items in the
     *                      archive
     */
    public static Integer getNumItems(Context context)
        throws SQLException
    {
        return getNumItems(context, null);
    }


    /**
     * print out the usage information for this class to the standard out
     */
    public static void usage()
    {
        String usage =
            "Usage Information:\n" +
            "LogAnalyser [options [parameters]]\n" +
            "-log [log directory]\n" +
                "\tOptional\n" +
                "\tSpecify a directory containing log files\n" +
                "\tDefault uses [dspace.dir]/log from dspace.cfg\n" +
            "-file [file name regex]\n" +
                "\tOptional\n" +
                "\tSpecify a regular expression as the file name template.\n" +
                "\tCurrently this needs to be correctly escaped for Java string handling (FIXME)\n" +
                "\tDefault uses dspace.log*\n" +
            "-cfg [config file path]\n" +
                "\tOptional\n" +
                "\tSpecify a config file to be used\n" +
                "\tDefault uses dstat.cfg in dspace config directory\n" +
            "-out [output file path]\n" +
                "\tOptional\n" +
                "\tSpecify an output file to write results into\n" +
                "\tDefault uses dstat.dat in dspace log directory\n" +
            "-start [YYYY-MM-DD]\n" +
                "\tOptional\n" +
                "\tSpecify the start date of the analysis\n" +
                "\tIf a start date is specified then no attempt to gather \n" +
                "\tcurrent database statistics will be made unless -lookup is\n" +
                "\talso passed\n" +
                "\tDefault is to start from the earliest date records exist for\n" +
            "-end [YYYY-MM-DD]\n" +
                "\tOptional\n" +
                "\tSpecify the end date of the analysis\n" +
                "\tIf an end date is specified then no attempt to gather \n" +
                "\tcurrent database statistics will be made unless -lookup is\n" +
                "\talso passed\n" +
                "\tDefault is to work up to the last date records exist for\n" +
            "-lookup\n" +
                "\tOptional\n" +
                "\tForce a lookup of the current database statistics\n" +
                "\tOnly needs to be used if date constraints are also in place\n" +
            "-help\n" +
                "\tdisplay this usage information\n";

        System.out.println(usage);
    }
}
