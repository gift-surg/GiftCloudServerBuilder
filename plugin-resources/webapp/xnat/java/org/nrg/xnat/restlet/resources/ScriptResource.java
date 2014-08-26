package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.automation.services.ScriptRunnerService;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class ScriptResource extends SecureResource {

    private static final Logger _log = LoggerFactory.getLogger(ScriptResource.class);

    private static final String COMPOSITE_ID = "COMPOSITE_ID";
    private static final String SCRIPT_ID = "SCRIPT_ID";

    private final Scope _scope;
    private final String _entityId;
    private final String _scriptId;
    private final String _path;

    private final ScriptRunnerService _service;

    public ScriptResource(Context context, Request request, Response response) throws ResourceException {
        super(context, request, response);

        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        getVariants().add(new Variant(MediaType.TEXT_XML));
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));

        _service = XDAT.getContextService().getBean(ScriptRunnerService.class);

        final String compositeId = (String) getRequest().getAttributes().get(COMPOSITE_ID);
        if (StringUtils.isBlank(compositeId)) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify a valid composite ID (for site-wide, use \"site\", otherwise use a composite ID (e.g. prj:1) to indicate the associated entity. Possible scopes include: " + Scope.Site.code() + " and " + Scope.Project.code());
        }
        final String[] atoms = compositeId.split(":");
        if (atoms.length == 0 || atoms.length > 2) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify a valid composite ID (for site-wide, use \"site\", otherwise use a composite ID (e.g. prj:1) to indicate the associated entity. Possible scopes include: " + Scope.Site.code() + " and " + Scope.Project.code());
        }
        if (StringUtils.isBlank(atoms[0])) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify the scope for the entity with which the requested script is associated. The default is \"site\".");
        }
        _scope = Scope.getScope(atoms[0]);
        _entityId = atoms.length == 1 ? null : atoms[1];
        _scriptId = (String) getRequest().getAttributes().get(SCRIPT_ID);
        _path = request.getResourceRef().getRemainingPart();

        if (_scope == Scope.Site && !StringUtils.isBlank(_entityId)) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You can't specify an entity ID for the site scope.");
        }
        if (_scope != Scope.Site && StringUtils.isBlank(_entityId)) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify an entity ID for any scope that isn't site scope.");
        }
        if (_scope != Scope.Site && _scope != Scope.Project) {
            throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "Support for scopes other than site or project is not yet implemented.");
        }
        if (_log.isDebugEnabled()) {
            _log.debug("Servicing script request for user: " + user.getLogin() + "\n * Scope: " + _scope + "\n * Entity ID: " + _entityId + "\n * Script ID: " + _scriptId + "\n * Path: " + _path);
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        final MediaType mediaType = overrideVariant(variant);

        final Properties properties;
        switch (_scope) {
            case Site:
                if (StringUtils.isBlank(_path)) {
                    properties = _service.getSiteScript(_scriptId, _path);
                } else {
                    properties = _service.getSiteScript(_scriptId, _path);
                }
                break;
            case Project:
                if (StringUtils.isBlank(_path)) {
                    properties = _service.getScopedScript(_scope, _entityId, _scriptId);
                } else {
                    properties = _service.getScopedScript(_scope, _entityId, _scriptId, _path);
                }
                break;
            default:
                properties = new Properties();
                properties.setProperty("script", "");
        }

        if (properties == null) {
            final StringBuilder message = new StringBuilder("Unable to find script ID[").append(_scriptId).append("] ");
            if (!StringUtils.isBlank(_path)) {
                message.append("path[").append(_path).append("]");
            }
            if (_scope == Scope.Site) {
                message.append(" for the site");
            } else {
                message.append(" for the project with ID ").append(_entityId);
            }
            _log.info(message.toString());
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message.toString());
            return null;
        }

        try {
            return new StringRepresentation(MAPPER.writeValueAsString(properties), mediaType);
        } catch (JsonProcessingException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "An error occurred marshalling the script data to JSON", e);
        } catch (IOException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "An error occurred marshalling the script data to JSON", e);
        }
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        final Representation entity = getRequest().getEntity();
        if (entity.getSize() == 0) {
            logger.warn("Unable to find script parameters: no data sent?");
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to find script parameters: no data sent?");
            return;
        }
        if (entity.getMediaType().equals(MediaType.APPLICATION_JSON)) {
            final Properties properties;
            try {
                final String text = entity.getText();
                properties = MAPPER.readValue(text, Properties.class);
            } catch (IOException e) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e, "An error occurred processing the script properties");
                return;
            }
            final String script = (String) properties.remove("script");
            try {
                _service.setScopedScript(user.getLogin(), _scope, _entityId, _scriptId, _path, script, properties);
            } catch (ConfigServiceException e) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e, "An error occurred saving the script " + _scriptId + (StringUtils.isBlank(_path) ? "" : " at path " + _path));
            }
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, "This function currently only supports the media type " + MediaType.APPLICATION_JSON);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}


