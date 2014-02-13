/*
 * org.nrg.xnat.restlet.extensions.SessionCountRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:16 PM
 */
package org.nrg.xnat.restlet.extensions;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.XnatRestlet;
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
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import java.util.List;

@XnatRestlet({"/services/sessions", "/services/sessions/{USERNAME}"})
public class SessionCountRestlet extends SecureResource {
    public static final String PARAM_USERNAME = "USERNAME";

    private static final Log _log = LogFactory.getLog(SessionCountRestlet.class);

    private final UserI _user;

    public SessionCountRestlet(Context context, Request request, Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));

        final String username = (String) getRequest().getAttributes().get(PARAM_USERNAME);

        // You can't request another user's session count unless you're a site admin.
        if (!StringUtils.isBlank(username)) {
            // But if it's just you, no harm no foul.
            if (username.equals(user.getLogin())) {
                _user = user;
            } else if (!user.isSiteAdmin()) {
                // If it's NOT you and you're not an admin, you are banished.
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Only site admins can request the session count for another user.");
                _user = null;
            } else {
                // If you are an admin and this isn't you, then let's get that account.
                final XdatUser xdatUser = XDATUser.getXdatUsersByLogin(username, user, false);
                if (xdatUser == null) {
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "The user identified by " + username + " can not be found in the system.");
                    _user = null;
                } else {
                    XDATUserDetails temp = null;
                    try {
                        temp = new XDATUserDetails(xdatUser);
                    } catch (Exception e) {
                        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Something went wrong retrieving the user " + username);
                    }
                    _user = temp;
                }
            }
        } else {
            _user = user;
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (_log.isDebugEnabled()) {
            _log.debug("Entering the session count represent() method");
        }

        int sessionCount = getSessionCount(_user);
        return new StringRepresentation(Integer.toString(sessionCount));
    }

    private int getSessionCount(UserI user) {
        SessionRegistry sessionRegistry = XDAT.getContextService().getBean("sessionRegistry", SessionRegistryImpl.class);

        int sessionCount = 0;

        if (sessionRegistry != null) {
            List<SessionInformation> l = sessionRegistry.getAllSessions(user, false);
            if (l != null) {
                sessionCount = l.size();
            }
        }
        return sessionCount;
    }
}
