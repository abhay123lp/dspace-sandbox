/*
 * HistoryConsumer.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/04/10 04:11:09 $
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

package org.dspace.history;

import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.storage.rdf.RDFRepository;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Add a History record for each content event consumed. This implements a "new"
 * (ca. November 2006) RDF-based history schema, and an RDF triple-store
 * provided by the RDFRepository class.
 * <p>
 * WARNING: This is a prototype. It was developed for one project and has not
 * been reviewed or tested substantially. Although it is an improvement over the
 * old HistoryManager class, there is no guarantee the data collected will be
 * adequate for future preservation activities.
 * <p>
 * See the HistorySystemPrototype page in the DSpace Wiki for more details about
 * this implementation: http://wiki.dspace.org/index.php/HistorySystemPrototype
 * 
 * @author Larry Stone
 * @version $Revision: 1.0 $
 * @see HistoryRepository
 * @see RDFRepository
 */
public class HistoryConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(HistoryConsumer.class);

    // repository to which we hand off RDF statements, set for
    // duration of event processing.
    private HistoryRepository rep;

    /**
     * Translate name into camel-caps path element for RDF URI. Underscore
     * counts as word break; (arg can be anything, we lcase it first) e.g.
     * "item" -> "Item", "modify_metadata" -> "ModifyMetadata"
     */
    private String camelCapify(String name)
    {
        String result = name.toLowerCase();
        result = String.valueOf(Character.toUpperCase(result.charAt(0)))
                .concat(result.substring(1));

        // Turn "Modify_metadata" into "ModifyMetadata"..
        int uscore = result.indexOf('_');
        if (uscore >= 0)
            result = result.substring(0, uscore).concat(
                    String.valueOf(
                            Character.toUpperCase(result.charAt(uscore + 1)))
                            .concat(result.substring(uscore + 2)));
        return result;
    }

    /**
     * Prepare to record RDF from the event stream.
     */
    public void initialize() throws Exception
    {
        rep = HistoryRepository.getInstance();
    }

    /**
     * Translate a DSpace object model change Event into an entry in the history
     * record and add it to the RDF repository.
     * <p>
     * NOTE: The RDF generated here is based on the ABC Harmony ongology (see
     * http://metadata.net/harmony/index.html ) but it does not conform to any
     * documented schema or ontology. Documentation may be forthcoming; check
     * the DSpace Wiki page
     * http://wiki.dspace.org/index.php/HistorySystemPrototype
     * <p>
     * NOTE: This only attempts to record "archival" events, i.e. events that
     * can be associated with _persistent_ identifiers. Any event affecting a
     * DSpace Object that has no persistent ID (i.e. Handle) does not get
     * recorded. This includes the early submission and workflow of Items
     * submitted through the Web UI, since they do not get a Handle until the
     * Item is installed at the end of the workflow. Delete operations are also
     * difficult, since by the time the event is processed the persistent
     * identifier is no longer available.
     * <p>
     * One exception: when event is an Add or Remove on a Bundle, "promote" the
     * Bundle to its owning Item so we can record it as an operation between
     * Bitstream and Item.
     * 
     * @param context
     *            DSpace context
     * @param event
     *            Content event
     */
    public void consume(Context context, Event event) throws Exception
    {
        int etypeInt = event.getEventType();
        String etype = camelCapify(event.getEventTypeAsString());
        int stype = event.getSubjectType();
        if (stype == Constants.SITE || stype == Constants.COMMUNITY
                || stype == Constants.COLLECTION || stype == Constants.ITEM
                || stype == Constants.BUNDLE || stype == Constants.BITSTREAM)
        {
            try
            {
                // key for storing all the triples in RDF repository -- this
                // names the object which will want to retrieve them.
                URI historyKey = null;

                // URI of the data model object, subject of event.
                URI eventSubject = null;

                // bundle name is bitstream's "type", could be significant in
                // preservation
                String bundleName = null;

                // DSpaceObject of subject, if available
                DSpaceObject subject = event.getSubject(context);

                // DELETE event can't get subject DSO, since it's gone
                // by the time consumer runs, but we still want to record it;
                // fortunately, event mech puts Handle in the Detail field.
                if (subject == null
                        && etypeInt == Event.DELETE
                        && (stype == Constants.COMMUNITY
                                || stype == Constants.COLLECTION || stype == Constants.ITEM))
                {
                    String subjectHandle = event.getDetail();
                    historyKey = rep.makeKey(stype, event.getSubjectID());
                    if (subjectHandle != null)
                        eventSubject = HistoryRepository
                                .makePersistentObjectURI(subjectHandle);
                    else
                        log
                                .debug("Handle not found in detail for ITEM/COLL/COMM.");
                }

                // otherwise expect to find the object.
                else if (subject != null)
                {
                    // if subject is a Bundle, _and_ if bitstream is added
                    // or removed, "promote" the subject to its parent Item
                    // so the bitstream appears to get added directly to Item.
                    if (stype == Constants.BUNDLE)
                    {
                        if (etypeInt == Event.ADD || etypeInt == Event.REMOVE)
                        {
                            bundleName = ((Bundle) subject).getName();
                            Item i[] = ((Bundle) subject).getItems();
                            if (i.length > 0)
                            {
                                subject = i[0];
                                stype = Constants.ITEM;
                            }
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                                log
                                        .debug("SKIPPING: insignificant event on Bundle, type="
                                                + etype);
                            return;
                        }
                    }

                    // sometimes makeKey fails, e.g. for a Bitstream that
                    // does not belong to an Item and so has no Handle.
                    try
                    {
                        historyKey = rep.makeKey(context, subject);
                    }
                    catch (SQLException e)
                    {
                        if (log.isDebugEnabled())
                            log
                                    .debug("SKIPPING: "
                                            + etype
                                            + " event on subject for which makeKey() fails, perhaps a disconnected Bitstream: "
                                            + subject.toString());
                        return;
                    }
                    eventSubject = HistoryRepository.makePersistentObjectURI(
                            context, subject);
                }

                // If we could not get a subject object, skip event UNLESS
                // Handle is available, e.g. in the detail field of DELETE
                // above.
                else
                {
                    if (log.isDebugEnabled())
                        log
                                .debug("SKIPPING: "
                                        + etype
                                        + " event on subject which could not be found (was deleted?), type="
                                        + event.getSubjectTypeAsString()
                                        + ", ID="
                                        + String.valueOf(event.getSubjectID()));
                    return;
                }

                // Skip unless we could concoct a URI for Subject (i.e. from
                // Handle)
                // note makePersistentObjectURI() creates Handles for Bitstreams
                // that include the sequenceID as fragment.
                if (eventSubject == null)
                {
                    if (log.isDebugEnabled())
                        log.debug("SKIPPING: an " + etype
                                + " event on Subject with no Handle, type="
                                + Constants.typeText[stype] + ", dbID="
                                + String.valueOf(event.getSubjectID()));
                }
                else
                {
                    // Check if event has Object, and does it have a Handle;
                    // if both are not true, don't mention the Object.
                    DSpaceObject objDso = null;
                    Resource eventObject = null;
                    ValueFactory vf = new ValueFactoryImpl();
                    int otype = event.getObjectType();
                    if (otype >= 0)
                    {
                        objDso = event.getObject(context);
                        if (objDso != null)
                            eventObject = HistoryRepository
                                    .makePersistentObjectURI(context, objDso);

                        // event detail in a REMOVE event includes the Handle of
                        // remove-ee:
                        else if (etypeInt == Event.REMOVE
                                && (otype == Constants.COMMUNITY
                                        || otype == Constants.COLLECTION || otype == Constants.ITEM))
                        {
                            String objectHandle = event.getDetail();
                            if (objectHandle != null)
                                eventObject = HistoryRepository
                                        .makePersistentObjectURI(objectHandle);
                            else
                                log.warn("REMOVE event on "
                                        + event.getObjectTypeAsString()
                                        + " is missing Detail field.");
                        }

                        if (eventObject == null)
                        {
                            if (log.isDebugEnabled())
                                log.debug("SKIPPING: Not mentioning " + etype
                                        + " event because EventObject (type="
                                        + event.getObjectTypeAsString()
                                        + ") does not have a URI..");
                            return;
                        }
                    }

                    rep
                            .addStatement(eventSubject, RDF.TYPE, vf.createURI(
                                    HistoryRepository.DSPACE_OBJECT_NS_URI,
                                    camelCapify(Constants.typeText[stype])),
                                    historyKey);

                    URI dcTitle = vf.createURI(HistoryRepository.DC_NS_URI,
                            "title");
                    URI manifestationURI = vf.createURI(
                            HistoryRepository.HARMONY_NS_URI, "Manifestation");
                    Literal eventTimeLiteral = vf.createLiteral(Utils
                            .formatISO8601Date(new Date(event.getTimeStamp())),
                            XMLSchema.DATETIME);

                    rep.addStatement(eventSubject, RDF.TYPE, manifestationURI,
                            historyKey);

                    // attach name of object as dc:title if available.
                    if (subject != null)
                    {
                        String name = subject.getName();
                        if (name != null)
                            rep.addStatement(eventSubject, dcTitle, vf
                                    .createLiteral(name, XMLSchema.STRING),
                                    historyKey);
                    }

                    // Statements about the "abc:Action" to describe event:
                    // Give subject URI a unique name that includes action and
                    // subject-type as decoration.
                    Resource actionSubject = vf.createURI(
                            HistoryRepository.DSPACE_HISTORY_NS_URI,
                            camelCapify(Constants.typeText[stype]) + "-"
                                    + etype + "-" + Utils.generateKey());

                    // set RDF type .. it is a subtype of abc:Action
                    // but declare the abc: type anyway
                    rep.addStatement(actionSubject, RDF.TYPE, vf.createURI(
                            HistoryRepository.DSPACE_HISTORY_NS_URI, etype),
                            historyKey);
                    rep.addStatement(actionSubject, RDF.TYPE, vf.createURI(
                            HistoryRepository.HARMONY_NS_URI, "Action"),
                            historyKey);

                    // set ABC's subject of action - hasPatient/creates/destroys
                    URI patientVerb;
                    if (etypeInt == Event.CREATE)
                        patientVerb = vf.createURI(
                                HistoryRepository.HARMONY_NS_URI, "creates");
                    else if (etypeInt == Event.DELETE)
                        patientVerb = vf.createURI(
                                HistoryRepository.HARMONY_NS_URI, "destroys");
                    else
                        patientVerb = vf.createURI(
                                HistoryRepository.HARMONY_NS_URI, "hasPatient");
                    rep.addStatement(actionSubject, patientVerb, eventSubject,
                            historyKey);

                    // if there's an event "object", it's relation is
                    // "abc:involves"
                    if (eventObject != null)
                    {
                        // Give object properties that define its type and DSO
                        // type
                        rep.addStatement(eventObject, RDF.TYPE, vf.createURI(
                                HistoryRepository.DSPACE_OBJECT_NS_URI,
                                camelCapify(event.getObjectTypeAsString())),
                                historyKey);
                        rep.addStatement(eventObject, RDF.TYPE,
                                manifestationURI, historyKey);
                        rep.addStatement(actionSubject, vf.createURI(
                                HistoryRepository.HARMONY_NS_URI, "involves"),
                                eventObject, historyKey);

                        // if there's an Event-Object object, check for details:
                        if (objDso != null)
                        {
                            // attach name of object as dc:title
                            String oname = objDso.getName();
                            if (oname != null)
                                rep.addStatement(eventObject, dcTitle,
                                        vf.createLiteral(oname,
                                                XMLSchema.STRING), historyKey);

                            // if obj is a Bitstream, give it dc:type of Bundle
                            // name
                            if (objDso.getType() == Constants.BITSTREAM)
                            {
                                // extract bundle name if we didn't get it above
                                if (bundleName == null)
                                {
                                    Bundle bn[] = ((Bitstream) objDso)
                                            .getBundles();
                                    if (bn.length > 0)
                                        bundleName = bn[0].getName();
                                }
                                if (bundleName != null)
                                    rep
                                            .addStatement(
                                                    eventObject,
                                                    vf
                                                            .createURI(
                                                                    HistoryRepository.DC_NS_URI,
                                                                    "type"),
                                                    vf.createLiteral(
                                                            bundleName,
                                                            XMLSchema.STRING),
                                                    historyKey);
                            }
                        }
                    }

                    // atTime on the abc:Action
                    rep.addStatement(actionSubject, vf.createURI(
                            HistoryRepository.DSPACE_HISTORY_NS_URI, "atTime"),
                            eventTimeLiteral, historyKey);

                    // if EPerson is availble, make it the "participant":
                    EPerson cu = context.getCurrentUser();
                    if (cu != null)
                    {
                        Resource userRes = HistoryRepository
                                .makePersistentObjectURI(context, cu);
                        rep.addStatement(userRes, RDF.TYPE, vf.createURI(
                                HistoryRepository.DSPACE_OBJECT_NS_URI,
                                "EPerson"), historyKey);
                        rep.addStatement(userRes, RDF.TYPE, vf.createURI(
                                HistoryRepository.HARMONY_NS_URI, "Agent"),
                                historyKey);
                        rep.addStatement(actionSubject, vf.createURI(
                                HistoryRepository.HARMONY_NS_URI,
                                "hasParticipant"), userRes, historyKey);
                    }

                    // this SHOULD be the application name (gets recorded in
                    // Context).
                    String app = context.getExtraLogInfo();
                    if (app != null && app.length() > 0)
                        rep.addStatement(actionSubject, vf.createURI(
                                HistoryRepository.DSPACE_HISTORY_NS_URI,
                                "usesTool"), vf.createLiteral(app,
                                XMLSchema.STRING), historyKey);

                    // detail record from DSpace event
                    String detail = event.getDetail();
                    if (detail != null && detail.length() > 0)
                        rep.addStatement(actionSubject, vf.createURI(
                                HistoryRepository.DSPACE_HISTORY_NS_URI,
                                "detail"), vf.createLiteral(detail,
                                XMLSchema.STRING), historyKey);

                    // transaction ID - to bind together actions from one
                    // Context.commit()
                    String tid = event.getTransactionID();
                    if (tid != null)
                        rep.addStatement(actionSubject, vf.createURI(
                                HistoryRepository.DSPACE_HISTORY_NS_URI,
                                "transactionID"), vf.createLiteral(tid,
                                XMLSchema.STRING), historyKey);

                    // inArchive, mark the DSpace archive where this happened.
                    DSpaceObject theSite = Site.find(context, 0);
                    URI theArchive = HistoryRepository.makePersistentObjectURI(
                            context, theSite);
                    rep.addStatement(theArchive, RDF.TYPE, vf.createURI(
                            HistoryRepository.HARMONY_NS_URI, "Manifestation"),
                            historyKey);
                    rep.addStatement(theArchive, dcTitle, vf.createLiteral(
                            theSite.getName(), XMLSchema.STRING), historyKey);

                    rep.addStatement(actionSubject, vf.createURI(
                            HistoryRepository.DSPACE_HISTORY_NS_URI,
                            "inArchive"), theArchive, historyKey);
                }
            }
            catch (SQLException e)
            {
                log.error("Error getting Handle of subject:", e);
            }
        }
    }

    /**
     * Finish recording RDF, signal the repository there are no more adds.
     */
    public void end(Context context) throws Exception
    {
        rep.finishAdds();
    }

    /**
     * 
     */
    public void finish(Context ctx) throws Exception
    {

    }

}
