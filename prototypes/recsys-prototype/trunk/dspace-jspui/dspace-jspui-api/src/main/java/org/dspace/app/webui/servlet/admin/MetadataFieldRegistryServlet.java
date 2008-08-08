/*
 * MetadataFieldRegistryServlet.java
 *
 * Version: $Revision: 2417 $
 *
 * Date: $Date: 2007-12-10 17:00:07 +0000 (Mon, 10 Dec 2007) $
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
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.core.Context;

/**
 * Servlet for editing the Dublin Core registry
 * 
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision: 2417 $
 */
public class MetadataFieldRegistryServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(MetadataFieldRegistryServlet.class);
    private String clazz = "org.dspace.app.webui.servlet.admin.MetadataFieldRegistryServlet";

    /**
     * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSGet(org.dspace.core.Context,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of type
        int schemaID = getSchemaID(request);
        showTypes(context, request, response, schemaID);
    }

    /**
     * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSPost(org.dspace.core.Context,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        MetadataFieldDAO dao =
            MetadataFieldDAOFactory.getInstance(context);

        String button = UIUtil.getSubmitButton(request, "submit");
        int schemaID = getSchemaID(request);

        // Get access to the localized resource bundle
        Locale locale = context.getCurrentLocale();
        ResourceBundle labels = ResourceBundle.getBundle("Messages", locale);

        if (button.equals("submit_update"))
        {
            // The sanity check will update the request error string if needed
            if (!sanityCheck(request, labels))
            {
                showTypes(context, request, response, schemaID);
                context.abort();
                return;
            }

            int id = UIUtil.getIntParameter(request, "dc_type_id");
            String element = request.getParameter("element");
            String qualifier = request.getParameter("qualifier");
            String scopeNote = request.getParameter("scope_note");

            // Let's see if this field already exists
            MetadataField dc = dao.retrieve(schemaID, element, qualifier);

//            try
//            {
            if (dc == null)
            {
                // Update the metadata for a DC type
                dc = dao.retrieve(id);

                dc.setElement(element);

                if (qualifier.equals(""))
                {
                    qualifier = null;
                }

                dc.setQualifier(qualifier);
                dc.setScopeNote(scopeNote);
                dao.update(dc);
                showTypes(context, request, response, schemaID);
                context.complete();
            }
//            catch (NonUniqueMetadataException e)
            else
            {
                context.abort();
//                log.error(e);
                log.error("metadata field " + element + "." + qualifier +
                        " already exists in schema " + schemaID);
            }
        }
        else if (button.equals("submit_add"))
        {

            // The sanity check will update the request error string if needed
            if (!sanityCheck(request, labels))
            {
                showTypes(context, request, response, schemaID);
                context.abort();
                return;
            }

            String element = request.getParameter("element");
            String qualifier = request.getParameter("qualifier");
            String scopeNote = request.getParameter("scope_note");

            // Let's see if this field already exists
            MetadataField dc = dao.retrieve(schemaID, element, qualifier);

            // Add a new DC type - simply add to the list, and let the user
            // edit with the main form
//            try
            if (dc == null)
            {
                dc = dao.create();
                dc.setSchemaID(schemaID);
                dc.setElement(element);

                if (qualifier.equals(""))
                {
                    qualifier = null;
                }

                dc.setQualifier(qualifier);
                dc.setScopeNote(scopeNote);
                dao.update(dc);
                showTypes(context, request, response, schemaID);
                context.complete();
            }
//            catch (NonUniqueMetadataException e)
            else
            {
                // Record the exception as a warning
//                log.warn(e);
                log.warn("metadata field " + element + "." + qualifier +
                        " already exists in schema " + schemaID);

                // Show the page again but with an error message to inform the
                // user that the metadata field was not created and why
                request.setAttribute("error", labels.getString(clazz
                        + ".createfailed"));
                showTypes(context, request, response, schemaID);
                context.abort();
            }
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            MetadataField dc = MetadataField.find(context, UIUtil
                    .getIntParameter(request, "dc_type_id"));
            request.setAttribute("type", dc);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/confirm-delete-mdfield.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            MetadataField dc = MetadataField.find(context, UIUtil
                    .getIntParameter(request, "dc_type_id"));
            dc.delete(context);
            showTypes(context, request, response, schemaID);
            context.complete();
        }
        else if (button.equals("submit_move"))
        {
            // User requests that one or more metadata elements be moved to a
            // new metadata schema. Note that we change the default schema ID to
            // be the destination schema.
//            try
//            {
                schemaID = Integer.parseInt(request
                        .getParameter("dc_dest_schema_id"));
                String[] param = request.getParameterValues("dc_field_id");
                if (schemaID == 0 || param == null)
                {
                    request.setAttribute("error", labels.getString(clazz
                            + ".movearguments"));
                    showTypes(context, request, response, schemaID);
                    context.abort();
                }
                else
                {
                    boolean error = false;

                    for (int ii = 0; ii < param.length; ii++)
                    {
                        int fieldID = Integer.parseInt(param[ii]);
                        MetadataField field = dao.retrieve(fieldID);
                        MetadataField existenceTest = dao.retrieve(schemaID,
                                field.getElement(), field.getQualifier());

                        if (existenceTest != null)
                        {
                            error = true;
                            log.warn("metadata field " + field.getElement() +
                                    "." + field.getQualifier() +
                                    " already exists in schema " + schemaID);
                            break;
                        }
                        else
                        {
                            field.setSchemaID(schemaID);
                            field.update(context);
                        }

                    }

                    if (error)
                    {
                        // Record the exception as a warning
//                        log.warn(e);

                        // Show the page again but with an error message to inform the
                        // user that the metadata field could not be moved
                        request.setAttribute("error", labels.getString(clazz
                                + ".movefailed"));
                        showTypes(context, request, response, schemaID);
                        context.abort();
                    }

                    context.complete();
                     
                    // Send the user to the metadata schema in which they just moved
                    // the metadata fields
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/metadata-schema-registry?dc_schema_id=" + schemaID);
                }
//            }
//            catch (NonUniqueMetadataException e)
//            {
//                // Record the exception as a warning
//                log.warn(e);
//
//                // Show the page again but with an error message to inform the
//                // user that the metadata field could not be moved
//                request.setAttribute("error", labels.getString(clazz
//                        + ".movefailed"));
//                showTypes(context, request, response, schemaID);
//                context.abort();
//            }
        }
        else
        {
            // Cancel etc. pressed - show list again
            showTypes(context, request, response, schemaID);
        }
    }

    /**
     * Get the schema that we are currently working in from the HTTP request. If
     * not present then default to the DSpace Dublin Core schema (schemaID 1).
     *
     * @param request
     * @return the current schema ID
     */
    private int getSchemaID(HttpServletRequest request)
    {
        int schemaID = MetadataSchema.DC_SCHEMA_ID;
        if (request.getParameter("dc_schema_id") != null)
        {
            schemaID = Integer.parseInt(request.getParameter("dc_schema_id"));
        }
        return schemaID;
    }

    /**
     * Show list of DC type
     * 
     * @param context
     *            Current DSpace context
     * @param request
     *            Current HTTP request
     * @param response
     *            Current HTTP response
     * @param schemaID
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void showTypes(Context context, HttpServletRequest request,
            HttpServletResponse response, int schemaID)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Find matching metadata fields
        MetadataField[] types = MetadataField
                .findAllInSchema(context, schemaID);
        request.setAttribute("types", types);

        // Pull the metadata schema object as well
        MetadataSchema schema = MetadataSchema.find(context, schemaID);
        request.setAttribute("schema", schema);

        // Pull all metadata schemas for the pulldown
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        request.setAttribute("schemas", schemas);

        JSPManager
                .showJSP(request, response, "/dspace-admin/list-metadata-fields.jsp");
    }

    /**
     * Return false if the metadata field fail to pass the constraint tests. If
     * there is an error the request error String will be updated with an error
     * description.
     *
     * @param request
     * @param labels
     * @return true of false
     */
    private boolean sanityCheck(HttpServletRequest request,
            ResourceBundle labels)
    {
        String element = request.getParameter("element");
        if (element.length() == 0)
        {
            return error(request, labels.getString(clazz + ".elemempty"));
        }
        for (int ii = 0; ii < element.length(); ii++)
        {
            if (element.charAt(ii) == '.' || element.charAt(ii) == '_'
                    || element.charAt(ii) == ' ')
            {
                return error(request, labels.getString(clazz + ".badelemchar"));
            }
        }
        if (element.length() > 64)
        {
            return error(request, labels.getString(clazz + ".elemtoolong"));
        }

        String qualifier = request.getParameter("qualifier");
        if (qualifier == "")
        {
            qualifier = null;
        }
        if (qualifier != null)
        {
            if (qualifier.length() > 64)
            {
                return error(request, labels.getString(clazz + ".qualtoolong"));
            }
            for (int ii = 0; ii < qualifier.length(); ii++)
            {
                if (qualifier.charAt(ii) == '.' || qualifier.charAt(ii) == '_'
                        || qualifier.charAt(ii) == ' ')
                {
                    return error(request, labels.getString(clazz
                            + ".badqualchar"));
                }
            }
        }

        return true;
    }

    /**
     * Bind the error text to the request object.
     *
     * @param request
     * @param text
     * @return false
     */
    private boolean error(HttpServletRequest request, String text)
    {
        request.setAttribute("error", text);
        return false;
    }
}
