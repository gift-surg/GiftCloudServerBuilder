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
import org.nrg.xdat.model.XnatSubjectassessordataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.*;

public class ProjectSubjectVisitsRestlet extends SecureResource {
    public ProjectSubjectVisitsRestlet(Context context, Request request, Response response) {
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
            return;
        }

        _log.info(String.format("Found service call for project %s, subject %s, and visit %s.", _projectId, _subjectId, _visitId));

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
            return;
        }

        initializeVisits();
    }

    /**
     * Gets a list of visit and experiment IDs for the specified user if visit ID is
     * not specified or a list of  the experiment IDs for the particular visit if the
     * visit ID is specified.
     */
    @Override
    public void handleGet() {
        try {
            if (StringUtils.isBlank(_visitId)) {
                getResponse().setEntity("{\"ResultSet\":{\"Result\":" + _mapper.writeValueAsString(_visits) + "}}", MediaType.APPLICATION_JSON);
            } else {
                getResponse().setEntity("{\"ResultSet\":{\"Result\":" + _mapper.writeValueAsString(_visits.get(_visitId)) + "}}", MediaType.APPLICATION_JSON);
            }
        } catch (IOException exception) {
            respondToException(exception, Status.SERVER_ERROR_INTERNAL);
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
            getResponse().setEntity("{\"ResultSet\":{\"Result\":" + formatVisitId(_visits.size()) + "}}", MediaType.APPLICATION_JSON);
        } else {
            getRequest().getEntity();
        }
    }

    /**
     * This constructs a crude "visit map" based on visit date. Basically all experiments
     * performed on a single day are characterized as being part of the same visit. The containing
     * map and set are TreeMap and TreeSet respectively, so that dates and experiment IDs are
     * automatically sorted.
     */
    private void initializeVisits() {
        List<XnatSubjectassessordataI> experiments = _subject.getExperiments_experiment();
        TreeMap<Date, TreeSet<String>> visitsByDate = new TreeMap<Date, TreeSet<String>>();
        for (XnatSubjectassessordataI experiment : experiments) {
            Date date = (Date) experiment.getDate();
            if (!visitsByDate.containsKey(date)) {
                visitsByDate.put(date, new TreeSet<String>());
            }
            visitsByDate.get(date).add(experiment.getId());
        }
        int index = 0;
        for (TreeSet<String> experimentIDs : visitsByDate.values()) {
            _visits.put(formatVisitId(index), experimentIDs);
        }
    }

    private String formatVisitId(int index) {
        return String.format("v%03d", index);
    }

    private static final Log _log = LogFactory.getLog(ProjectSubjectVisitsRestlet.class);

    private final ObjectMapper _mapper = new ObjectMapper();
    private final XnatProjectdata _project;
    private final XnatSubjectdata _subject;
    private final String _projectId;
    private final String _subjectId;
    private final String _visitId;
    private TreeMap<String, TreeSet<String>> _visits = new TreeMap<String, TreeSet<String>>();
}
