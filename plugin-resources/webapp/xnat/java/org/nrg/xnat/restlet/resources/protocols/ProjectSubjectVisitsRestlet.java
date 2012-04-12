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
import org.codehaus.jackson.type.TypeReference;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ProjectSubjectVisitsRestlet extends SecureResource {
    public ProjectSubjectVisitsRestlet(Context context, Request request, Response response) throws SQLException, DBPoolException {
        super(context, request, response);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        final Map<String, Object> attributes = request.getAttributes();
        _projectId = attributes.containsKey("PROJECT_ID") ? (String) attributes.get("PROJECT_ID") : null;
        _subjectId = attributes.containsKey("SUBJECT_ID") ? (String) attributes.get("SUBJECT_ID") : null;
        _visitId = attributes.containsKey("VISIT_ID") ? (String) attributes.get("VISIT_ID") : null;
        _type = attributes.containsKey("TYPE") ? (String) attributes.get("TYPE") : null;

        final boolean hasProjectID = !StringUtils.isBlank(_projectId);
        if (!hasProjectID || StringUtils.isBlank(_subjectId)) {
            response.setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED, "You must specify a " + (!hasProjectID ? "project" : "subject") + " ID for this service call.");
            _project = null;
            _subject = null;
            return;
        }

        _log.info(String.format("Found service call for project %s, subject %s, visit %s.", _projectId, _subjectId, _visitId));

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
                Map<String, Object> visitData;
                if (StringUtils.isBlank(_visitId)) {
                    final List<Map<String, Object>> visits = getSubjectVisits();
                    final String visitId = getCurrentVisitId(visits);
                    visitData = new Hashtable<String, Object>();
                    visitData.put("project", _projectId);
                    visitData.put("subject", _subjectId);
                    visitData.put("visit_id", visitId);
                    visitData.put("available", VISIT_LIST);
                    visitData.put("visit_data", visits);
                } else {
                    visitData = getSubjectVisitData();
                }
                String marshaledVisitData = _mapper.writeValueAsString(visitData);
                String payload = String.format(RESULT_SET_JSON_WRAPPER, marshaledVisitData, 1);
                getResponse().setEntity(payload, MediaType.APPLICATION_JSON);
            } catch (Exception exception) {
                respondToException(exception, Status.SERVER_ERROR_INTERNAL);
            }
        } else {
            getResponse().setEntity(EMPTY_RESULT_SET, MediaType.APPLICATION_JSON);
        }
    }

    private String getCurrentVisitId(final List<Map<String, Object>> visits) {
        List<String> eligibleVisits = new ArrayList<String>();
        List<String> allVisits = new ArrayList<String>();
        for (Map<String, Object> visit : visits) {
            final String visitId = (String) visit.get(VISITS_COLUMNS[0]);
            allVisits.add(visitId);
            @SuppressWarnings("unchecked")
            final List<String> scans = (List<String>) visit.get(VISITS_COLUMNS[2]);
            if (scans == null || scans.size() < 3) {
                eligibleVisits.add(visitId);
            }
        }
        // If there are ANY eligible visits...
        if (eligibleVisits.size() > 0) {
            // Sort them and return the lowest visit number.
            Collections.sort(eligibleVisits);
            return eligibleVisits.get(0);
        } else if (allVisits.size() > 0) {
            // But if no eligible visits, take the full list of visits, get the last one, and increment the number.
            // Technically this should never happen: there should ALWAYS be a visit:visitData before a scan can be
            // uploaded, but this is a just-in-case exception.
            Collections.sort(allVisits);
            Collections.reverse(allVisits);
            return String.format("v%02d", Integer.parseInt(allVisits.get(0).substring(1)) + 1);
        } else {
            return "v00";
        }
    }

    /**
     * If this is a post without a visit ID, the user is requesting the next visit ID. If it's with
     * a visit ID, the user is committing the indicated visit. The request entity is only relevant
     * when committing.
     */
    @Override
    public void handlePost() {
        try {
            if (!StringUtils.isBlank(_type)) {
                generateId();
            } else if (StringUtils.isBlank(_visitId)) {
                getResponse().setEntity(EMPTY_RESULT_SET, MediaType.APPLICATION_JSON);
            } else {
                getResponse().setEntity(EMPTY_RESULT_SET, MediaType.APPLICATION_JSON);
            }
        } catch (Exception exception) {
            respondToException(exception, Status.SERVER_ERROR_INTERNAL);
        }
    }

    /**
     * Generates a session identifier based on the requested type and the project's protocol for ID generation.
     * @throws IOException Can occur where there are errors serializing or deserializing REST parameters.
     */
    private void generateId() throws IOException {
        if (!StringUtils.isBlank(_type)) {
            if (_type.equalsIgnoreCase("sessionId")) {
                if (!this.getRequest().isEntityAvailable()) {
                    throw new IllegalArgumentException("There's no request entity available, session information must be posted as request entity.");
                }
                Representation entity = this.getRequest().getEntity();
                Map<String, String> properties = _mapper.readValue(entity.getText(), TYPE_REFERENCE_STRING_MAP);
                if (properties.containsKey("visitId") && properties.containsKey("scanType")) {
                    String visitId = properties.get("visitId");
                    String scanType = properties.get("scanType");
                    if (scanType.equalsIgnoreCase("PET-PIB")) {
                        scanType = "pib";
                    } else if (scanType.equalsIgnoreCase("PET-FDG")) {
                        scanType = "fdg";
                    } else if (scanType.equalsIgnoreCase("MR")) {
                        scanType = "mr";
                    } else {
                        throw new IllegalArgumentException("The indicated scan type is invalid: " + scanType + ". Must be one of MR, PET-PIB, or PET-FDG.");
                    }
                    final String sessionId = String.format(DIAN_VISIT_FORMAT, _subjectId, visitId, scanType);
                    getResponse().setEntity(_mapper.writeValueAsString(new HashMap<String, String>() {{ put("sessionId", sessionId); }}), MediaType.APPLICATION_JSON);
                }
            } else {
                throw new IllegalArgumentException("Session ID generation requested for an unknown entity type: " + _type);
            }
        }
    }

    private List<Map<String, Object>> getSubjectVisits() throws SQLException, DBPoolException, IOException {
        return processVisitQuery(String.format(QUERY_VISIT_LIST + QUERY_VISIT_LIST_ORDER, _subject.getId(), _project.getId()));
    }

    private Map<String, Object> getSubjectVisitData() throws SQLException, DBPoolException, IOException {
        final List<Map<String, Object>> visits = processVisitQuery(String.format(QUERY_VISIT_LIST + QUERY_FILTER_BY_VISIT + QUERY_VISIT_LIST_ORDER, _subject.getId(), _project.getId(), _visitId));
        if (visits.size() == 0) {
            return new Hashtable<String, Object>();
        }
        return visits.get(0);
    }

    private List<Map<String, Object>> processVisitQuery(final String query) throws SQLException, DBPoolException {
        XFTTable results = XFTTable.Execute(query, user.getDBName(), user.getUsername());
        final int numRows = results.getNumRows();
        Map<String, Map<String, Object>> mappedRows = new Hashtable<String, Map<String, Object>>();
        if (numRows > 0) {
            while(results.hasMoreRows()) {
                Object[] row = results.nextRow();
                processVisitRows(mappedRows, row);
            }
        }
        return new ArrayList<Map<String, Object>>(mappedRows.values());
    }

    private void processVisitRows(final Map<String, Map<String, Object>> mappedRows, final Object[] row) {
        final String visitId = (String) row[0];
        final Date date = (Date) row[1];
        final String visitType = (String) row[2];
        final String tracer  = (String) row[3];
        final boolean rowExists = mappedRows.containsKey(visitId);
        Map<String, Object> mappedRow;
        if (!rowExists) {
            mappedRows.put(visitId, mappedRow = new Hashtable<String, Object>());
            mappedRow.put(VISITS_COLUMNS[0], visitId);
        } else {
            mappedRow = mappedRows.get(visitId);
        }
        if (visitType.equalsIgnoreCase(ELEMENT_VISIT)) {
            mappedRow.put(VISITS_COLUMNS[1], date);
        } else {
            List<String> scans;
            if (mappedRow.containsKey(VISITS_COLUMNS[2])) {
                //noinspection unchecked
                scans = (List<String>) mappedRow.get(VISITS_COLUMNS[2]);
            } else {
                mappedRow.put(VISITS_COLUMNS[2], scans = new ArrayList<String>());
            }
            if (visitType.equalsIgnoreCase(ELEMENT_MR_SESSION)) {
                scans.add("MR");
            } else if (visitType.equalsIgnoreCase(ELEMENT_PET_SESSION)) {
                if (tracer.equalsIgnoreCase(TRACER_FDG)) {
                    scans.add("PET-FDG");
                } else if (tracer.equalsIgnoreCase(TRACER_PIB)) {
                    scans.add("PET-PIB");
                } else if (StringUtils.isBlank(tracer)) {
                    throw new IllegalArgumentException("No tracer specified for PET imaging session on " + _projectId + " " + _subjectId + " visit: " + visitId);
                } else {
                    throw new IllegalArgumentException("Unknown tracer " + tracer + " specified for PET imaging session on " + _projectId + " " + _subjectId + " visit: " + visitId);
                }
            }
        }
    }

    private static final Log _log = LogFactory.getLog(ProjectSubjectVisitsRestlet.class);
    private static final String[] VISIT_LIST = { "v00","v01","v02","v03","v04","v05" };
    private static final String[] VISITS_COLUMNS = { "visit_id", "date", "scans" };
    private static final String DIAN_VISIT_FORMAT = "%s_%s_%s";
    private static final String RESULT_SET_JSON_WRAPPER = "{\"ResultSet\":{\"Result\":%s,\"totalRecords\":\"%s\"}}";
    private static final String EMPTY_RESULT_SET = String.format(RESULT_SET_JSON_WRAPPER, "[]", 0);

    private static final String QUERY_VISIT_LIST = "SELECT visit_id, date, element_name as visit_type, tracer_name FROM xnat_experimentdata expt " +
            "LEFT JOIN xnat_subjectassessordata sad ON expt.id = sad.id " +
            "LEFT JOIN xdat_meta_element xme ON expt.extension = xme.xdat_meta_element_id " +
            "LEFT JOIN visit_visitData visit ON expt.id = visit.id " +
            "LEFT JOIN xnat_petsessiondata xpsd ON expt.id = xpsd.id " +
            "WHERE (xme.element_name = 'visit:visitData' OR xme.element_name = 'xnat:mrSessionData' OR xme.element_name = 'xnat:petSessionData') AND subject_id = '%s' AND project = '%s' ";

    private static final String ELEMENT_VISIT = "visit:visitData";
    private static final String ELEMENT_MR_SESSION = "xnat:mrSessionData";
    private static final String ELEMENT_PET_SESSION = "xnat:petSessionData";
    private static final String TRACER_FDG = "FDG";
    private static final String TRACER_PIB = "PIB";

    private static final String QUERY_VISIT_LIST_ORDER = "ORDER BY visit_id, date";

    private static final String QUERY_FILTER_BY_VISIT = "AND visit_id='%s' ";
    public static final TypeReference<Map<String,String>> TYPE_REFERENCE_STRING_MAP = new TypeReference<Map<String, String>>() {
    };

    private final ObjectMapper _mapper = new ObjectMapper();
    private final String _projectId;
    private final String _subjectId;
    private final XnatProjectdata _project;
    private final XnatSubjectdata _subject;
    private final String _visitId;
    private final String _type;
}
