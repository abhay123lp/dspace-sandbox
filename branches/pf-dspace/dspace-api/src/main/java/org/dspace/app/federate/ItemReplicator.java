/*
 * ItemReplicator.java
 *
 * Version: $Revision: 1.10 $
 *
 * Date: $Date: 2006/07/30 08:26:57 $
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

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.app.federate.dao.RemoteRepositoryDAO;
import org.dspace.app.federate.dao.postgres.RemoteRepositoryDAOPostgres;
import org.dspace.app.federate.harvester.HarvestException;
import org.dspace.app.federate.harvester.HarvestResults;
import org.dspace.app.federate.harvester.Harvester;
import org.dspace.app.mets.BitstreamChecksumException;
import org.dspace.app.mets.BitstreamRetrievalException;
import org.dspace.app.mets.ImportException;
import org.dspace.app.mets.MetadataValidationException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ItemReplicator is responsible for harvesting content from remote
 * repositories. Note that this class delegates the acual mechanics of
 * importing Items elsewhere. Here, we take care of higher-level matters such
 * as determining if there are any failed imports to be resumed, etc.
 * 
 * @author Weihua Huang
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision: 1.10 $
 */

public class ItemReplicator
{
	/** log4j logger */
	private static Logger log = Logger.getLogger(ItemReplicator.class);

	/** Content object we are using */
	private Context context;

	/** An OAI-PMH harvester */
	private Harvester harvester;

	/** The most recent midnight */
	private Date midnight;

	private RemoteRepositoryDAO dao;

	/**
	 * Create an item replicator that will use the given Context to replicate
	 * items from remote repositories. The Context needs to have a valid current
	 * user.
	 * 
	 * @param c
	 *			Context object, initialised with an e-person
	 */
	public ItemReplicator(Context c) throws SAXException
	{
		context = c;
		dao = new RemoteRepositoryDAOPostgres(c);

		// Check the context has an e-person
		if (context.getCurrentUser() == null)
		{
			log.warn("ItemReplicator initialised with context that has no current eperson");
			throw new IllegalStateException(
					"Context has no current user e-person");
		}

		harvester = new Harvester();

		// Get the most recent midnight (UTC)
		// GregorianCalendar gc = new GregorianCalendar(TimeZone
		//		 .getTimeZone("UTC"));
		GregorianCalendar gc = new GregorianCalendar(TimeZone
				   .getDefault());
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);

		midnight = gc.getTime();
	}

	/**
	 * Attempt to replicate objects from the given repository, bringing the
	 * local repository up to date. The remote repository's record will reflect
	 * the success or otherwise of this process. An e-mail will be sent to the
	 * administrator of the local repository as well as the remote repository
	 * indicating any errors that have occurred.
	 * 
	 * @param repo
	 *			the remote repository
	 * @throws SQLException
	 *			 thrown if there is a problem accessing the local database.
	 *			 This is a local server problem, and not a problem relating to
	 *			 the connection between the local and remote repository, or
	 *			 any metadata validation problems.
	 */
	public void replicateFrom(RemoteRepository repo, Date date)
		throws IOException, SQLException, AuthorizeException, MessagingException
	{
		// Records currently known to be 'bad'
		Set<String> currentBadRecords = new HashSet<String>(repo.getFailedImports());

		// Map record IDs (OAI identifiers) to the exceptions that caused the
		// problem
		Map newBadRecords = new HashMap();

		boolean more = true;
		boolean fullHarvest = true;
		String resumptionToken = null;
		String metadataPrefix = "qdc";

		try
		{
			/*
			 * Harvest new and changed items. We harvest, then import, and if a
			 * resumption token is present, continue harvesting until resumption
			 * tokens are exhausted.
			 */
			System.out.println("Harvesting from " + repo.getBaseURL() +
					" at " + (date == null? midnight: date));
			log.info("Harvesting from " + repo.getBaseURL() +
					" at " + (date == null? midnight: date));
			
			HarvestResults results;

			Collection collection = null;

			while (more)
			{
				// harvest the METS documents
				if (resumptionToken == null)
				{
					results = harvester.harvestDateRange(repo.getBaseURL(),
							metadataPrefix, repo.getDateLastHarvested(),
							(date == null? midnight: date));
					log.info("found " + results.getResultCount() + " items");
				}
				else
				{
					System.out.println("Encountered resumption token, continuing with another harvest");
					System.out.println(" token is: " + resumptionToken);
					results = harvester.resumeHarvest(repo.getBaseURL(),
							resumptionToken);
				}
				
				if (results.getResultCount() > 0)
				{
					collection = findDestinationCollection(repo, date);

					// import them
					for (int i = 0; i < results.getResultCount(); i++)
					{
						log.info("importing from " + results.getMetadataFile(i).toString());
						importFromFile(results.getMetadataFile(i), collection,
								metadataPrefix, results.getIdentifier(i),
								currentBadRecords, newBadRecords);
					}
				}

				/*
				 * Do a commit after each cache, to prevent everything being one
				 * huge transaction
				 */
				context.commit();

				if (results.hasResumptionToken())
				{
					resumptionToken = results.getResumptionToken();
				}
				else
				{
					more = false;
				}
			}

			// Attempt to harvest bad records not fixed during the above harvest
			List<String> badRecordsToRetry = new LinkedList<String>(currentBadRecords);

			for (String id : badRecordsToRetry)
			{
				log.info("apparently there are some failed imports to resume...");

				// Try a one-off harvest
				results = harvester
						.harvestSingle(repo.getBaseURL(), metadataPrefix, id);

				if (results.getResultCount() > 0)
				{
					if (collection == null)
					{
						log.info("finding destination collection for failed imports");
						collection = findDestinationCollection(repo, date);
					}
					// Import if we retrieved a record
					importFromFile(results.getMetadataFile(0), collection,
							metadataPrefix, results.getIdentifier(0),
							currentBadRecords, newBadRecords);
				}
			}
		}
		catch (CrosswalkException ce)
		{
			log.info("Error harvesting from " + repo.getBaseURL(), ce);
			fullHarvest = false;
		}
		catch (PackageValidationException pve)
		{
			log.info("Error harvesting from " + repo.getBaseURL(), pve);
			fullHarvest = false;
		}
		catch (HarvestException he)
		{
			log.info("Error harvesting from " + repo.getBaseURL(), he);
			fullHarvest = false;
		}
		catch (MetadataValidationException mve)
		{
			log.info("Error harvesting from " + repo.getBaseURL(), mve);
			fullHarvest = false;
		}

		// Update the repository record
		if (fullHarvest)
		{
			repo.setDateLastHarvested(date == null ? midnight: date);
		}

		// In any case, update the list of bad records
		currentBadRecords.addAll(newBadRecords.keySet());
		repo.setFailedImports(new LinkedList(currentBadRecords));
		dao.update(repo);
		context.commit();

		/*
		 * We now create the error message if required. This will send an email
		 * if there are any outstanding bad records, regardless of when they
		 * originally failed.
		 */
		if (!fullHarvest || 
			currentBadRecords.size() > 0 ||
			newBadRecords.size() > 0)
		{
			Email errorEmail = ConfigurationManager
					.getEmail("validation-error");

			errorEmail.addArgument(ConfigurationManager
					.getProperty("dspace.name"));
			errorEmail.addArgument(ConfigurationManager
					.getProperty("dspace.url"));
			errorEmail.addArgument(ConfigurationManager
					.getProperty("mail.admin"));
			errorEmail.addArgument(repo.getBaseURL());

			// Add list of broken records
			StringBuffer badRecordText = new StringBuffer();

			for (String id : currentBadRecords)
			{
				ImportException ie = (ImportException) newBadRecords.get(id);

				badRecordText.append("\n ").append(id);

				if (ie != null)
				{
					// We have an exception so we can give information about
					// the error. FIXME: Not internationalised
					if (ie instanceof BitstreamChecksumException)
					{
						badRecordText.append(" - checksum mismatch on ")
								.append(ie.getMessage());
					}
					else if (ie instanceof BitstreamRetrievalException)
					{
						badRecordText.append(" - could not retrieve ").append(
								ie.getMessage());
					}
					else
					{
						badRecordText.append(" - ").append(ie.getMessage());
					}
				}
			}

			errorEmail.addArgument(badRecordText.toString());

			if (fullHarvest)
			{
				errorEmail.addArgument("");
			}
			else
			{
				errorEmail.addArgument(
						"Harvesting error (connection problem or malformed response)");
			}

			// Send to repo's admin as well as local DSpace admin
			errorEmail.addRecipient(repo.getAdminEmail());
			errorEmail.addRecipient(ConfigurationManager
					.getProperty("mail.admin"));
			errorEmail.send();
		}
    }

	/**
	 * @deprecated Use importFromFile instead
	 */
	/*
	private void importFromMETS(File f, Collection c, String oaiID,
			Set currentBadRecords, Map newBadRecords)
		throws PackageValidationException, CrosswalkException, IOException,
			AuthorizeException, SQLException, MetadataValidationException
	{
		importFromFile(f, c, oaiID, currentBadRecords, newBadRecords);
	}
	*/

	/**
	 * Attempt to import the given file. If there is a problem with the import
	 * that is not an 'internal' or local problem (for example database
	 * failure), the record is added to the set of new bad records. If the
	 * import is successful, the record is removed from the set of current bad
	 * records.
	 *
	 * FIXME: We currently don't offer the option of passing the incoming Item
	 * through a workflow. I'm not sure if this is going to be a problem or
	 * not, but I doubt it.
	 *
	 * FIXME: Need to make sure that the incoming item is actually an Item
	 * FIXME: Need to think about preserving provenance of incoming Items
	 * FIXME: Need a more robust way of checking whether an item already
	 * exists. This will probably be an easy fix once we have a Handle prefix
	 * to play with.
	 * FIXME: Currently don't import bitstreams.
	 * 
	 * @param file
	 *			the harvested METS XML record
	 * @param oaiIdentifier
	 * @param currentBadRecords
	 *			the current list of known bad records
	 * @param newBadRecords
	 *			the list of records found to be bad during current harvest
	 * @throws IOException
	 *			 if there is a <strong>local</strong> I/O problem
	 * @throws SQLException
	 *			 if there is a local database problem
	 * @throws AuthorizeException
	 *			 if there is an authorisation problem
	 */
	private void importFromFile(File file, Collection collection,
			String metadataPrefix, String oaiIdentifier,
			Set currentBadRecords, Map newBadRecords)
		throws PackageValidationException, CrosswalkException, IOException,
			AuthorizeException, SQLException, MetadataValidationException
	{
		log.info("Importing item from " + file.toURL() + 
				" into collection " + collection.getMetadata("name"));

		try
		{
			/*
			 * Remove the item from the list of *old* bad records. This is so
			 * that we don't try and re-harvest it twice in one session. If this
			 * import is unsuccessful the item will be added to the list of
			 * 'new' bad records.
			 */
			currentBadRecords.remove(oaiIdentifier);

			// Not validating yet
			SAXBuilder builder = new SAXBuilder(false);
			Document document = builder.build(file);
			Element root = document.getRootElement();

			WorkspaceItem wi = WorkspaceItem.create(context, collection, false);
			Item tempItem = wi.getItem();

			// Crosswalk the metadata into the Item
			IngestionCrosswalk xwalk = null;
			xwalk = (IngestionCrosswalk) PluginManager.getNamedPlugin(
					IngestionCrosswalk.class, metadataPrefix.toUpperCase());
            log.warn(metadataPrefix.toUpperCase() + " " + xwalk);
            xwalk.ingest(context, tempItem, root);

			// FIXME: I think we have to do this, since oai_dc doesn't allow
			// qualifiers. This will need to be abstracted to allow for
			// alternate (ie: non-DC) metadata formats.
//			DCValue values[] = tempItem.getMetadata("dc", "identifier", Item.ANY, Item.ANY);
			DCValue values[] = tempItem.getMetadata("dc", "identifier", "uri", Item.ANY);
			Item original = null;

			for (int x = 0; x < values.length; x++)
			{
				String identifier = values[x].value;
				TableRow row = DatabaseManager.findByUnique(
					context, "metadatavalue", "text_value", identifier);
				log.info("searching for dc.identifier.* = " + identifier);
				if (row != null)
				{
					int id = row.getIntColumn("item_id");
					if (id > 0)
					{
						original = Item.find(context, id);
						break;
					}
				}
			}

            ItemDAO dao = ItemDAOFactory.getInstance(context);

            if (original != null)
			{
				log.info("item already exists");
				wi = null;
				original.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
				xwalk.ingest(context, original, root);
                
                dao.update(original);
			}
			else
			{
				log.info("item doesn't already exist");
                Item i = InstallItem.installItem(context, wi);
                dao.update(i);
                // InstallItem.installItem(context, wi, tempItem.getIdentifier().getCanonicalForm(), true);
			}
		}
		catch (JDOMException ie)
		{
			// An error was encountered during the import, so this must be
			// added to the list of new bad records
			log.error("Problem harvesting record with ID: " + oaiIdentifier, ie);
			newBadRecords.put(oaiIdentifier, ie);
		}
    }

	/**
	 * 
	 */
	private Collection findDestinationCollection(RemoteRepository rr, Date date)
		throws SQLException, AuthorizeException, IOException
	{
		Community parent = rr.getCommunity();
        
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);
        List<Collection> collections = collectionDAO.getChildCollections(parent);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String name = sdf.format(date);

		for (Collection collection : collections)
		{
			if (collection.getMetadata("name").equals(name))
			{
				log.info("decided to import items into existing collection \""
						+ collection.getMetadata("name") + "\"");
				return collection;
			}
		}

		// If we get to here, the collection doesn't already exist, and so we
		// need to create it.
		Collection destination = parent.createCollection();
		destination.setMetadata("name", name);
		collectionDAO.update(destination);

		log.info("decided to import items into new collection \""
				+ destination.getMetadata("name") + "\"");

		return destination;
	}

	/**
	 * Command-line interface, which runs through all of the active
	 * repositories and attempts to update them.
	 * 
	 * @param args  command-line args
	 */
	public static void main(String[] args) throws Exception
	{
		// create an options object and populate it
		CommandLineParser parser = new PosixParser();

		Options options = new Options();

		options.addOption("h", "help", false, "show help");
		options.addOption("e", "eperson", true,
				"email address of eperson to use for replicating");

		CommandLine line = parser.parse(options, args);

		// Print usage if help requested or arg/option missing
		if (line.hasOption('h') || !line.hasOption('e'))
		{
			HelpFormatter help = new HelpFormatter();
			help.printHelp("ItemReplicator [OPTIONS]",
					options);

			// Exit code is 0 if help requested, 1 if missing option/argument
			System.exit(line.hasOption('h') ? 0 : 1);
		}

		// Connect to DSpace system
		Context context = new Context();
		
		// Find the user specified in the command line
        EPersonDAO epersonDAO = EPersonDAOFactory.getInstance(context);
        EPerson eperson =
                epersonDAO.retrieve(EPerson.EPersonMetadataField.EMAIL,
                        line.getOptionValue('e'));

        if (eperson == null)
		{
			System.err.println("No eperson with email address '"
					+ line.getOptionValue('e') + "'");
			context.abort();
			System.exit(1);
		}

		context.setCurrentUser(eperson);
		
		ItemReplicator replicator = new ItemReplicator(context);

		// Iterate through all of the active remote repositories
		RemoteRepositoryDAO dao = new RemoteRepositoryDAOPostgres(context);
		List<RemoteRepository> allActive = dao.getActiveRemoteRepositories();
		
		for (RemoteRepository repo : allActive)
		{
			System.out.println("Replicating from " + repo.getBaseURL());
			replicator.replicateFrom(repo, new Date());
		}
	}
}
