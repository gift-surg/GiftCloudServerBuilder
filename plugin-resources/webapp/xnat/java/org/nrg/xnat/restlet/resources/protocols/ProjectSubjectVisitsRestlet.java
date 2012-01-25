/**
 * ProjectSubjectVisitsRestlet
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 1/17/12 by rherri01
 */
package org.nrg.xnat.restlet.resources.protocols;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ProjectSubjectVisitsRestlet extends SecureResource {
    public ProjectSubjectVisitsRestlet(Context context, Request request, Response response) throws SQLException, DBPoolException {
        super(context, request, response);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        final Map<String, Object> attributes = request.getAttributes();
        _projectId = attributes.containsKey("PROJECT_ID") ? (String) attributes.get("PROJECT_ID") : null;
        _subjectId = attributes.containsKey("SUBJECT_ID") ? (String) attributes.get("SUBJECT_ID") : null;
        _visitId = attributes.containsKey("VISIT_ID") ? (String) attributes.get("VISIT_ID") : null;

        final boolean hasProjectID = !StringUtils.isBlank(_projectId);
        if (!hasProjectID || StringUtils.isBlank(_subjectId)) {
            response.setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED, "You must specify a " + (!hasProjectID ? "project" : "subject") + " ID for this service call.");
            _project = null;
            _subject = null;
            _actions = null;
            return;
        }

        _actions = getActions();

        _log.info(String.format("Found service call for project %s, subject %s, visit %s, actions %s.", _projectId, _subjectId, _visitId, _actions));

        _project = XnatProjectdata.getProjectByIDorAlias(_projectId, user, false);
        if (_project == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "The project ID " + _projectId + " does not result in a valid project.");
            _log.warn("Found no corresponding project for requested project ID: " + _projectId + ", requested by user: " + user.getUsername());
            _subject = null;
            return;
        }
        _subject = XnatSubjectdata.getXnatSubjectdatasById(_subjectId, user, false);
        if (_subject == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "The subject ID " + _subjectId + " does not result in a valid subject.");
            _log.warn("Found no corresponding subject for requested subject ID: " + _subjectId + ", requested by user: " + user.getUsername());
        }
    }

    /**
     * Gets a list of visit and experiment IDs for the specified user if visit ID is
     * not specified or a list of  the experiment IDs for the particular visit if the
     * visit ID is specified.
     */
    @Override
    public void handleGet() {
        if (_project.getId().startsWith("DIAN")) {
            try {
                if (StringUtils.isBlank(_visitId)) {
                    Map<String, Object> visitData = new Hashtable<String, Object>();
                    visitData.put("project", _projectId);
                    visitData.put("subject", _subjectId);
                    visitData.put("last_visit_id", getLastVisitID());
                    visitData.put("available", VISIT_LIST);
                    visitData.put("visit_data", getSubjectVisits());
                    String marshaledVisitData = _mapper.writeValueAsString(visitData);
                    String payload = String.format(VISITS, marshaledVisitData, 1);
                    getResponse().setEntity(payload, MediaType.APPLICATION_JSON);
                } else {
                    getResponse().setEntity(getSubjectVisitData(), MediaType.APPLICATION_JSON);
                }
            } catch (Exception exception) {
                respondToException(exception, Status.SERVER_ERROR_INTERNAL);
            }
        } else {
            getResponse().setEntity("{\"ResultSet\":{\"Result\":\"\"}}", MediaType.APPLICATION_JSON);
        }
    }

    /**
     * If this is a post without a visit ID, the user is requesting the next visit ID. If it's with
     * a visit ID, the user is committing the indicated visit. The request entity is only relevant
     * when committing.
     */
    @Override
    public void handlePost() {
        if (StringUtils.isBlank(_visitId)) {
            getResponse().setEntity("{\"ResultSet\":{\"Result\":\"\"}}", MediaType.APPLICATION_JSON);
        } else {
            getResponse().setEntity("{\"ResultSet\":{\"Result\":\"\"}}", MediaType.APPLICATION_JSON);
        }
    }

    private String getLastVisitID() throws ElementNotFoundException, XFTInitException, SQLException, DBPoolException {
        final String query = String.format(QUERY_MAX_VISIT_ID, _subject.getId(), _project.getId());
        XFTTable results = XFTTable.Execute(query, user.getDBName(), user.getUsername());
        if (results.getNumRows() > 0) {
            return (String) results.convertColumnToArrayList("last_visit_id").get(0);
        } else {
            return DEFAULT_VISIT_ID;
        }
    }

    private List<Map<String, Object>> getSubjectVisits() throws SQLException, DBPoolException, IOException {
        final String query = String.format(QUERY_VISIT_LIST, _subject.getId(), _project.getId());
        XFTTable results = XFTTable.Execute(query, user.getDBName(), user.getUsername());
        final int numRows = results.getNumRows();
        List<Map<String, Object>> mappedRows = new ArrayList<Map<String, Object>>();
        if (numRows > 0) {
            while(results.hasMoreRows()) {
                Object[] row = results.nextRow();
                Map<String, Object> mappedRow = new Hashtable<String, Object>();
                for (int index = 0; index < ROW_LIST.length; index++) {
                    mappedRow.put(ROW_LIST[index], row[index] == null ? "" : row[index]);
                }
                mappedRows.add(mappedRow);
            }
        }
        return mappedRows;
    }

    private String getSubjectVisitData() throws SQLException, DBPoolException, IOException {
        final String query = String.format(QUERY_VISIT_LIST + QUERY_FILTER_BY_VISIT, _subject.getId(), _project.getId(), _visitId);
        XFTTable results = XFTTable.Execute(query, user.getDBName(), user.getUsername());
        final int numRows = results.getNumRows();
        String payload;
        if (numRows > 0) {
            payload = _mapper.writeValueAsString(results.rows());
        } else {
            payload = "";
        }
        return String.format(VISITS, payload, numRows);
    }

    private static final Log _log = LogFactory.getLog(ProjectSubjectVisitsRestlet.class);
    // private static final String VISIT_LIST = "{\"ResultSet\":{\"Result\":{\"project\":\"%s\",\"subject\":\"%s\",\"first_unused\":\"%s\",\"available\":[\"v00\",\"v01\",\"v02\",\"v03\",\"v04\",\"v05\"]},\"totalRecords\":\"1\"}}";
    private static final String[] VISIT_LIST = { "v00","v01","v02","v03","v04","v05" };
    private static final String[] ROW_LIST = { "visit_id", "type", "date", "occurred" };
    private static final String VISITS = "{\"ResultSet\":{\"Result\":%s,\"totalRecords\":\"%s\"}}";
    private static final String DEFAULT_VISIT_ID = "v00";
    private static final String QUERY_MAX_VISIT_ID = "select max(visit_id) as last_visit_id from xnat_experimentdata expt " +
            "LEFT JOIN xnat_subjectassessordata sad ON expt.id=sad.id " +
            "LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id " +
            "WHERE xme.element_name='visit:visitData' AND subject_id='%s' AND project='%s'";

    private static final String QUERY_VISIT_LIST = "select visit_id, type, date, occurred from xnat_experimentdata expt " +
            "LEFT JOIN xnat_subjectassessordata sad ON expt.id=sad.id " +
            "LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id " +
            "LEFT JOIN visit_visitData visit ON expt.id=visit.id " +
            "WHERE xme.element_name='visit:visitData' AND subject_id='%s' AND project='%s'";

    private static final String QUERY_FILTER_BY_VISIT = " AND visit_id='%s'";


    private final ObjectMapper _mapper = new ObjectMapper();
    private final String _projectId;
    private final String _subjectId;
    private final XnatProjectdata _project;
    private final XnatSubjectdata _subject;
    private final List<String> _actions;
    private final String _visitId;
}
