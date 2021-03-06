/*
 * org.nrg.xnat.restlet.services.AliasTokenRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.services;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AliasTokenRestlet extends SecureResource {
	public static final String PARAM_OPERATION = "OPERATION";
	public static final String PARAM_USERNAME = "USERNAME";
	public static final String PARAM_TOKEN = "PARAM_TOKEN";
	public static final String PARAM_SECRET = "PARAM_SECRET";
	public static final String OP_ISSUE = "issue";
	public static final String OP_VALIDATE = "validate";
	public static final String OP_INVALIDATE = "invalidate";

	public AliasTokenRestlet(Context context, Request request, Response response) {
		super(context, request, response);
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		_operation = (String) getRequest().getAttributes().get(PARAM_OPERATION);
		_username = (String) getRequest().getAttributes().get(PARAM_USERNAME);
		_token = (String) getRequest().getAttributes().get(PARAM_TOKEN);
		final String secret = (String) getRequest().getAttributes().get(
				PARAM_SECRET);
		_secret = StringUtils.isBlank(secret) ? INVALID : Long
				.parseLong(secret);
	}

	@Override
	public Representation represent() throws ResourceException {
		if (OP_ISSUE.equals(_operation)) {
			if (!StringUtils.isBlank(_username) && !user.isSiteAdmin()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Only admins can create proxy tokens.");
			}
			try {
				AliasToken token = StringUtils.isBlank(_username) ? getService()
						.issueTokenForUser(user) : getService()
						.issueTokenForUser(_username);
				return new StringRepresentation(mapToken(token));
			} catch (Exception exception) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"An error occurred retrieving the user: " + _username,
						exception);
			}
		} else if (OP_VALIDATE.equals(_operation)) {
			if (StringUtils.isBlank(_token) || _secret == INVALID) {
				throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
						"You must specify both token and secret to validate a token.");
			}
			try {
				final HashMap<String, String> results = new HashMap<String, String>();
				results.put("valid", getService()
						.validateToken(_token, _secret));
				return new StringRepresentation(
						_serializer.writeValueAsString(results));
			} catch (IOException exception) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						exception.toString());
			}
		} else if (OP_INVALIDATE.equals(_operation)) {
			getService().invalidateToken(_token);
			return new StringRepresentation("{\"result\": \"OK\"}");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unknown operation: " + _operation);
		}
	}

	private String mapToken(final AliasToken token) {
		Map<String, String> map = Maps.newHashMap();
		map.put("alias", token.getAlias());
		map.put("secret", Long.toString(token.getSecret()));
		String value = "";
		try {
			value = _serializer.writeValueAsString(map);
		} catch (IOException e) {
			//
		}
		return value;
	}

	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return represent();
	}

	private AliasTokenService getService() {
		if (_service == null) {
			_service = XDAT.getContextService()
					.getBean(AliasTokenService.class);
		}
		return _service;
	}

	private static final int INVALID = -1;
	private static final ObjectMapper _serializer = new ObjectMapper();
	private AliasTokenService _service;
	private String _operation;
	private final String _username;
	private final String _token;
	private final long _secret;
}
