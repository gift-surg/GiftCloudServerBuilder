/**
 * RemoteLoggingRestlet
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 11/30/11 by rherri01
 */
package org.nrg.xnat.restlet.services;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nrg.framework.logging.Analytics;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.HashMap;
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
        final String tool = (String) request.getAttributes().get(Analytics.TOOL_TAG);

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
                Map<String, Object> map = getObjectMapper().readValue(expression, new TypeReference<Map<String, Object>>() {});

                // TODO: Convert this stuff to use server-side Analytics from nrg_framework
                Log logger;
                if (tool.equalsIgnoreCase(Analytics.ANALYTICS)) {
                    logger = _analytics;
                } else {
                    logger = _log;
                }

                RemoteEvent event = getRemoteEvent(map, request.getClientInfo());

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

    private RemoteEvent getRemoteEvent(final Map<String, Object> map, ClientInfo clientInfo) {
        return new RemoteEvent(map, clientInfo);
    }

    private ObjectMapper getObjectMapper() {
        if (_mapper == null) {
            _mapper = new ObjectMapper();
        }
        return _mapper;
    }

    private static final Log _log = LogFactory.getLog(RemoteLoggingRestlet.class);
    private static final Log _analytics = LogFactory.getLog(Analytics.ANALYTICS);
    private ObjectMapper _mapper;

    private class RemoteEvent extends HashMap<String, Object> {
        public RemoteEvent(Map<String, Object> map) {
            putAll(map);
        }

        public RemoteEvent(Map<String, Object> map, ClientInfo clientInfo) {
            this(map);
            put("address", clientInfo.getAddress());
            put("port", clientInfo.getPort());
            put("agent", clientInfo.getAgent());
            put("agentName", clientInfo.getAgentName());
            Map<String, String> attributes = clientInfo.getAgentAttributes();
            if (attributes != null && attributes.size() >  0) {
                putAll(attributes);
            }
        }

        public Level getLevel() {
            if (containsKey("level")) {
                return Level.toLevel((String) get("level"));
            }
            if (containsKey("LEVEL")) {
                return Level.toLevel((String) get("LEVEL"));
            }
            return Level.TRACE;
        }

        @Override
        public String toString() {
            try {
                // TODO: The replaceAll() call converts single quotes to work properly in escaped SQL queries. This really only needs to be done at the JDBC insert level.
                // There's a modified JDBCAppender which handles this at http://sourceforge.net/projects/jdbcappender, but it hasn't been updated since 2005. That may be OK.
                return getObjectMapper().writeValueAsString(this).replaceAll("'", "\\\\'");
            } catch (IOException exception) {
                return "Error occurred while converting to string: " + exception.getMessage();
            }
        }
    }
}
