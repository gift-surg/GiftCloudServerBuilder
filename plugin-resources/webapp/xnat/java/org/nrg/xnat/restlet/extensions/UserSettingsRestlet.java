/**
 * UserSettingsRestlet
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 12/21/12 by rherrick
 */
package org.nrg.xnat.restlet.extensions;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Variant;

import java.util.List;

@XnatRestlet({"/user/actions/{ACTION}", "/user/actions/{USER_ID}/{ACTION}"})
public class UserSettingsRestlet extends SecureResource {
    public UserSettingsRestlet(Context context, Request request, Response response) throws Exception {
        super(context, request, response);
        if (!user.isSiteAdmin()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project.");
            _action = null;
            _auths = null;
        } else {
            this.getVariants().add(new Variant(MediaType.ALL));
            _action = (String) getRequest().getAttributes().get("ACTION");
            if (StringUtils.isBlank(_action)) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "You must specify an action to perform.");
                _auths = null;
                return;
            }
            String userId = (String) getRequest().getAttributes().get("USER_ID");
            if (StringUtils.isBlank(userId)) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "As of this release, you must specify a user on which to perform.");
                _auths = null;
                return;
            }
            _auths = XDAT.getXdatUserAuthService().getUsersByName(userId);
            if (_auths == null || _auths.size() == 0) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "As of this release, you must specify a user on which to perform.");
            }
        }
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        handleAction();
    }

    @Override
    public void handlePost() {
        handleAction();
    }

    private void handleAction() {
        if (_action.equals("reset")) {
            for (XdatUserAuth auth : _auths) {
                auth.setFailedLoginAttempts(0);
                XDAT.getXdatUserAuthService().update(auth);
            }
        } else {
            throw new RuntimeException("Unknown action: " + _action);
        }
    }

    private final String _action;
    private final List<XdatUserAuth> _auths;
}
