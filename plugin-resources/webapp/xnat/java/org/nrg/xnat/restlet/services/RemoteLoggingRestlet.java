/*
 * org.nrg.xnat.restlet.services.RemoteLoggingRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.services;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nrg.framework.analytics.AnalyticsEvent;
import org.nrg.framework.logging.Analytics;
import org.nrg.framework.logging.RemoteEvent;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.Map;

public class RemoteLoggingRestlet extends SecureResource {
    public RemoteLoggingRestlet(Context context, Request request, Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
    }

    @Override
    public boolean allowGet(){
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        final Request request = getRequest();
        final String tool = (String) request.getAttributes().get(Analytics.EVENT_KEY);

        if (_log.isDebugEnabled()) {
            final ClientInfo client = request.getClientInfo();
            final StringBuilder buffer = new StringBuilder("Handling POST logging request from ");
            buffer.append(client.getAddress()).append(":").append(client.getPort());
            buffer.append(" ").append(client.getAgent()).append(" ").append(request.getCookies());
            _log.debug(buffer.toString());
        }

        String expression = null;
        Representation entity = request.getEntity();
        if (entity != null && entity.isAvailable() && entity.getSize() > 0) {
            try {
                expression = entity.getText();
            } catch (IOException exception) {
                respondToException(exception, Status.CLIENT_ERROR_NOT_ACCEPTABLE);
                return;
            }
        }

        Status status = Status.SUCCESS_OK;

        if (!StringUtils.isBlank(expression)) {
            try {
                // TODO: Convert this stuff to use server-side Analytics from nrg_framework
                final boolean isAnalytics = tool.equalsIgnoreCase(Analytics.ANALYTICS);
                Log logger = isAnalytics ? _analytics : _remote;
                RemoteEvent event = isAnalytics ? getObjectMapper().readValue(expression, AnalyticsEvent.class) : processAsMap(expression);
                Level level = event.getLevel();

                if (level.equals(Level.TRACE)) {
                    logger.trace(event);
                } else if (level.equals(Level.DEBUG)) {
                    logger.debug(event);
                } else if (level.equals(Level.INFO)) {
                    logger.info(event);
                } else if (level.equals(Level.WARN)) {
                    logger.warn(event);
                } else if (level.equals(Level.ERROR)) {
                    logger.error(event);
                } else if (level.equals(Level.FATAL)) {
                    logger.fatal(event);
                }
            } catch (IOException exception) {
                respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
                status = Status.CLIENT_ERROR_BAD_REQUEST;
            }
        } else {
            status = Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY;
        }

        final Response response = getResponse();
        response.setEntity(status.toString(), MediaType.TEXT_PLAIN);
        response.setStatus(status);
    }

    private RemoteEvent processAsMap(final String expression) throws IOException {
        Map<String, Object> eventMap = getObjectMapper().readValue(expression, new TypeReference<Map<String, Object>>() {});
        return new RemoteEvent(eventMap);
    }

    private ObjectMapper getObjectMapper() {
        if (_mapper == null) {
            _mapper = new ObjectMapper();
            _mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            _mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            _mapper.getDeserializationConfig().set(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            _mapper.getDeserializationConfig().set(DeserializationConfig.Feature.WRAP_EXCEPTIONS, true);
        }
        return _mapper;
    }

    private static final Log _log = LogFactory.getLog(RemoteLoggingRestlet.class);
    private static final Log _remote = LogFactory.getLog(RemoteEvent.REMOTE_LOG);
    private static final Log _analytics = LogFactory.getLog(Analytics.ANALYTICS);
    private ObjectMapper _mapper;
}
