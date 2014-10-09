package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.util.HashMap;
import java.util.Map;

public abstract class AutomationResource extends SecureResource {

    public AutomationResource(final Context context, final Request request, final Response response) throws ResourceException {
        super(context, request, response);
        final String projectId = (String) getRequest().getAttributes().get(PROJECT_ID);
        if (StringUtils.isBlank(projectId)) {
            _scope = Scope.Site;
            _projectDataInfo = null;
            _projectId = null;
        } else {
            _scope = Scope.Project;
            Map<String, String> values = validateEntityId(projectId);
            _projectDataInfo = values.get(KEY_PROJECTDATAINFO);
            _projectId = values.get(KEY_PROJECTID);
        }
        _path = request.getResourceRef().getRemainingPart();
    }

    protected abstract String getResourceType();

    protected abstract String getResourceId();

    protected Scope getScope() {
        return _scope;
    }

    protected String getProjectId() {
        return _projectId;
    }

    protected String getProjectDataInfo() {
        return _projectDataInfo;
    }

    protected String getAssociation() {
        return Scope.encode(getScope(), getProjectDataInfo());
    }

    protected String getPath() {
        return _path;
    }

    protected String getRequestContext(final String header) {
        final StringBuilder buffer = new StringBuilder(header).append(":\n");
        buffer.append(" * Scope: ").append(_scope.toString());
        if (_scope == Scope.Project) {
            buffer.append("\n * Project ID: ").append(_projectId);
        }
        if (StringUtils.isNotBlank(getResourceId())) {
            buffer.append("\n * ").append(getResourceType()).append(" ID: ").append(getResourceId());
        }
        if (StringUtils.isNotBlank(_path)) {
            buffer.append("\n * Path: ").append(_path);
        }
        return buffer.toString();
    }

    private Map<String, String> validateEntityId(final String entityId) throws ResourceException {
        switch (getScope()) {
            case Site:
                return null;

            case Project:
                if (StringUtils.isBlank(entityId)) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify an ID for the project scope.");
                }
                final Map<String, String> ids = new HashMap<String, String>();
                // Check to see if entityId is actually a project ID. If so, convert it to a Long.
                final Long resolved = XnatProjectdata.getProjectInfoIdFromStringId(entityId);
                if (resolved != null) {
                    ids.put(KEY_PROJECTDATAINFO, resolved.toString());
                    ids.put(KEY_PROJECTID, entityId);
                }
                else {
                    try {
                        XFTTable table = XFTTable.Execute("SELECT id FROM xnat_projectdata WHERE projectdata_info = " + entityId, user.getDBName(), userName);
                        if (table.size() != 1) {
                            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Couldn't find a project with the ID or alias of " + entityId);
                        }
                        ids.put(KEY_PROJECTDATAINFO, entityId);
                        ids.put(KEY_PROJECTID, (String) table.convertColumnToArrayList("id").get(0));
                    } catch (Exception e) {
                        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "An error occurred trying to access the database.", e);
                    }
                }
                return ids;

            default:
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The specified scope " + getScope().code() + " is not currently supported. Supported scopes include: " + Scope.Site.code() + " and " + Scope.Project.code());
        }
    }

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String KEY_PROJECTDATAINFO = "projectDataInfo";
    private static final String KEY_PROJECTID = "projectId";

    private final Scope _scope;
    private final String _projectId;
    private final String _projectDataInfo;
    private final String _path;
}
