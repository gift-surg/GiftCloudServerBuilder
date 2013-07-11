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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import java.util.List;

@XnatRestlet("/services/sessions")
public class SessionCountRestlet extends SecureResource {
    private static final Log _log = LogFactory.getLog(SessionCountRestlet.class);

    public SessionCountRestlet(Context context, Request request, Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (_log.isDebugEnabled()) {
            _log.debug("Entering the session count represent() method");
        }

        int sessionCount = getSessionCount(user);
        return new StringRepresentation(Integer.toString(sessionCount));
    }

    private int getSessionCount(XDATUser user) {
        SessionRegistry sessionRegistry = XDAT.getContextService().getBean("sessionRegistry", SessionRegistryImpl.class);

        int sessionCount = 0;

        if(sessionRegistry != null){
            List<SessionInformation> l = sessionRegistry.getAllSessions(user, false);
            if(l != null){
                sessionCount = l.size();
            }
        }
        return sessionCount;
    }
}
