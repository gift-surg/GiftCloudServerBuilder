package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.automation.entities.ScriptTrigger;
import org.nrg.automation.services.ScriptTriggerService;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFTTable;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class ScriptTriggerResource extends AutomationResource {

	public ScriptTriggerResource(Context context, Request request,
			Response response) throws ResourceException {
		super(context, request, response);

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));

		_service = XDAT.getContextService().getBean(ScriptTriggerService.class);

		_event = (String) getRequest().getAttributes().get(EVENT);

		if (!user.isSiteAdmin()) {
			_log.warn(getRequestContext("User "
					+ user.getLogin()
					+ " attempted to access forbidden script trigger template resources"));
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"Only site admins can view or update script resources.");
		}

		if (request.getMethod().equals(Method.DELETE)
				&& StringUtils.isBlank(_event)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED,
					"You must specify a specific scope and event to delete a script trigger.");
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Servicing script trigger request "
					+ formatScopeEntityIdAndEvent() + " for user "
					+ user.getLogin());
		}
	}

	@Override
	protected String getResourceType() {
		return "Event";
	}

	@Override
	protected String getResourceId() {
		return _event;
	}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public Representation represent(Variant variant) throws ResourceException {
		final MediaType mediaType = overrideVariant(variant);

		if (StringUtils.isNotBlank(_event)) {
			try {
				// They're requesting a specific trigger, so return that to
				// them.
				final ScriptTrigger trigger = getScriptTrigger();
				if (trigger == null) {
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"Didn't find a script trigger for the indicated "
									+ formatScopeEntityIdAndEvent());
				}
				return new StringRepresentation(
						MAPPER.writeValueAsString(trigger), mediaType);
			} catch (JsonProcessingException e) {
				throw new ResourceException(
						Status.SERVER_ERROR_INTERNAL,
						"An error occurred marshalling the script trigger data to JSON",
						e);
			} catch (IOException e) {
				throw new ResourceException(
						Status.SERVER_ERROR_INTERNAL,
						"An error occurred marshalling the script trigger data to JSON",
						e);
			}
		} else {
			// They're asking for list of existing script triggers, so give them
			// that.
			return listScriptTriggers(mediaType);
		}
	}

	@Override
	public void handlePut() {
		try {
			if (StringUtils.isNotBlank(_event)) {
				putScriptTrigger();
			} else {
				throw new ClientException(
						Status.CLIENT_ERROR_METHOD_NOT_ALLOWED,
						"You must specify an event on the REST URL to PUT a script trigger to the server.");
			}
		} catch (ClientException e) {
			getResponse().setStatus(e.getStatus(), e.getMessage());
		} catch (ServerException e) {
			_log.error(
					"Server error occurred trying to store a script trigger resource",
					e);
			getResponse().setStatus(e.getStatus(), e.getMessage());
		}
	}

	@Override
	public void handleDelete() {
		try {
			if (_log.isDebugEnabled()) {
				_log.debug("Preparing to delete script trigger for "
						+ formatScopeEntityIdAndEvent()
						+ " and its associated triggers.");
			}
			final ScriptTrigger trigger = _service.getByAssociationAndEvent(
					getAssociation(), _event);
			if (trigger == null) {
				throw new ClientException(Status.CLIENT_ERROR_NOT_FOUND,
						"Didn't find a trigger to delete associated with "
								+ formatScopeEntityIdAndEvent());
			}
			_service.delete(trigger);
		} catch (ClientException e) {
			_log.info(e.getMessage());
			getResponse().setStatus(e.getStatus(), e.getMessage());
		}
	}

	/**
	 * Lists the script triggers with the specified scope and entity ID and
	 * event.
	 *
	 * @return A representation of the script triggers available for the
	 *         specified scope, entity ID (if specified), and event.
	 */
	private Representation listScriptTriggers(final MediaType mediaType) {
		Hashtable<String, Object> params = new Hashtable<String, Object>();
		params.put("scope", getScope());
		if (getScope() == Scope.Project) {
			params.put("projectId", getProjectId());
		}

		ArrayList<String> columns = new ArrayList<String>();
		columns.add("triggerId");
		columns.add("scope");
		columns.add("entityId");
		columns.add("event");
		columns.add("scriptId");
		columns.add("description");

		XFTTable table = new XFTTable();
		table.initTable(columns);

		final List<ScriptTrigger> triggers = getScope() == Scope.Site ? _service
				.getSiteTriggers() : _service.getByScope(getScope(),
				getProjectDataInfo());
		for (final ScriptTrigger trigger : triggers) {
			final Map<String, String> atoms = Scope.decode(trigger
					.getAssociation());
			final String scope = atoms.get("scope");
			final String entityId = scope.equals(Scope.Site.code()) ? ""
					: atoms.get("entityId");
			table.insertRowItems(trigger.getTriggerId(), scope, entityId,
					trigger.getEvent(), trigger.getScriptId(),
					trigger.getDescription());
		}

		return representTable(table, mediaType, params);
	}

	private ScriptTrigger getScriptTrigger() {
		return _service.getByScopeEntityAndEvent(getScope(),
				getProjectDataInfo(), _event);
	}

	private void putScriptTrigger() throws ClientException, ServerException {
		// TODO: this needs to properly handle a PUT to an existing script as
		// well as an existing but disabled script.
		final Representation entity = getRequest().getEntity();
		if (entity.getSize() == 0) {
			logger.warn("Unable to find script trigger parameters: no data sent?");
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to find script trigger parameters: no data sent?");
			return;
		}

		MediaType mediaType = entity.getMediaType();
		if (!mediaType.equals(MediaType.APPLICATION_WWW_FORM)
				&& !mediaType.equals(MediaType.APPLICATION_JSON)) {
			throw new ClientException(
					Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
					"This function currently only supports "
							+ MediaType.APPLICATION_WWW_FORM + " and "
							+ MediaType.APPLICATION_JSON);
		}

		final Properties properties;
		if (mediaType.equals(MediaType.APPLICATION_WWW_FORM)) {
			try {
				final List<NameValuePair> formMap = URLEncodedUtils.parse(
						entity.getText(), DEFAULT_CHARSET);
				properties = new Properties();
				for (final NameValuePair entry : formMap) {
					properties.setProperty(entry.getName(), entry.getValue());
				}
			} catch (IOException e) {
				throw new ServerException(
						Status.SERVER_ERROR_INTERNAL,
						"An error occurred trying to read the submitted form body.",
						e);
			}
		} else {
			try {
				final String text = entity.getText();
				properties = MAPPER.readValue(text, Properties.class);
			} catch (IOException e) {
				throw new ServerException(Status.SERVER_ERROR_INTERNAL,
						"An error occurred processing the script properties", e);
			}
		}

		// TODO: These remove definitions of scope, entity ID, and script ID
		// that may be passed in on the API call.
		// TODO: We may consider throwing an exception if something in the body
		// parameters contradicts the URI
		// TODO: parameters. For example, if the URL indicates site scope, but
		// the body parameters specify project and
		// TODO: ID, it may be worth throwing an exception and indicating that
		// you should only specify that stuff in the
		// TODO: URL. For now, though, we'll just ignore the payload parameters
		// for simplicity.
		if (getScope() == Scope.Project) {
			properties.setProperty("scope", Scope.Project.code());
			properties.setProperty("entityId", getProjectDataInfo());
		} else {
			properties.setProperty("scope", Scope.Site.code());
			properties.remove("entityId");
		}

		ScriptTrigger trigger = _service.getByScopeEntityAndEvent(getScope(),
				getProjectDataInfo(), _event);
		if (trigger == null) {
			if (!properties.containsKey("event")) {
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,
						"You must specify the event for your new script trigger.");
			}
			if (!properties.containsKey("scriptId")) {
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,
						"You must specify the script ID for your new script trigger.");
			}
			if (_log.isDebugEnabled()) {
				_log.debug("Creating new script trigger");
			}
			final String scriptId = properties.getProperty("scriptId");
			final String event = properties.getProperty("event");
			final String triggerId = properties.getProperty("triggerId",
					_service.getDefaultTriggerName(scriptId, getScope(),
							getProjectDataInfo(), event));
			final String description = properties.getProperty("description",
					null);
			trigger = _service.newEntity(triggerId, description, scriptId,
					getAssociation(), event);
			if (_log.isInfoEnabled()) {
				_log.info("Created a new trigger: " + trigger.toString());
			}
		} else {
			final String scriptId = properties.getProperty("scriptId");
			final String event = properties.getProperty("event");
			final String triggerId = properties.getProperty("triggerId");
			final String description = properties.getProperty("description",
					null);
			boolean isDirty = false;
			if (StringUtils.isNotBlank(scriptId)
					&& !scriptId.equals(trigger.getScriptId())) {
				trigger.setScriptId(scriptId);
				isDirty = true;
			}
			if (StringUtils.isNotBlank(event)
					&& !event.equals(trigger.getEvent())) {
				trigger.setEvent(event);
				isDirty = true;
			}
			if (StringUtils.isNotBlank(triggerId)
					&& !triggerId.equals(trigger.getTriggerId())) {
				trigger.setTriggerId(triggerId);
				isDirty = true;
			}
			// Description is a little different because you could specify an
			// empty description.
			if (description != null
					&& !description.equals(trigger.getDescription())) {
				trigger.setDescription(description);
				isDirty = true;
			}
			if (!getAssociation().equals(getAssociation())) {
				trigger.setAssociation(getAssociation());
				isDirty = true;
			}
			if (isDirty) {
				_service.update(trigger);
			}
		}
	}

	private String formatScopeEntityIdAndEvent() {
		final StringBuilder buffer = new StringBuilder();
		if (getScope() == Scope.Site) {
			buffer.append("site");
		} else {
			buffer.append("project ").append(getProjectId());
		}
		if (StringUtils.isNotBlank(_event)) {
			buffer.append(" and event ").append(_event);
		} else {
			buffer.append(", no event");
		}
		return buffer.toString();
	}

	private static final Logger _log = LoggerFactory
			.getLogger(ScriptTriggerResource.class);

	private static final String EVENT = "EVENT";
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final ScriptTriggerService _service;

	private final String _event;
}
