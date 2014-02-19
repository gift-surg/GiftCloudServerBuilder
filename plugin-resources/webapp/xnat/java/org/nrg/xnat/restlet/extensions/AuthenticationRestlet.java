/*
 * org.nrg.xnat.restlet.extensions.AuthenticationRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.extensions;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.security.XnatAuthenticationFilter;
import org.nrg.xnat.security.XnatProviderManager;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@XnatRestlet(value = "/services/auth", secure = false)
public class AuthenticationRestlet extends Resource {
    private static final Log _log = LogFactory.getLog(AuthenticationRestlet.class);

    public AuthenticationRestlet(Context context, Request request, Response response) throws Exception {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
        if (request.getMethod().equals(Method.GET)) {
            throw new Exception("You must POST or PUT authentication credentials in the request body.");
        }
        if (request.isEntityAvailable()) {
            extractCredentials(request.getEntity().getText());
        } else {
            throw new Exception("You must provide authentication credentials in the request body.");
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
        runAuthenticate();
    }

    @Override
    public void handlePost() {
        runAuthenticate();
    }

    private void runAuthenticate() {
        if (_log.isDebugEnabled()) {
            _log.debug("Passing a representation of the verify extensions restlet.");
        }

        if (StringUtils.isBlank(_username) || StringUtils.isBlank(_password)) {
            fail();
            return;
        }

        if(StringUtils.isEmpty(_authMethod) && !StringUtils.isEmpty(_username)){
            //try to guess the auth method
            _authMethod = XnatAuthenticationFilter.retrieveAuthMethod(_username);
            if(StringUtils.isEmpty(_authMethod)) {
                throw new BadCredentialsException("Missing login method parameter.");
            }
        }

        UsernamePasswordAuthenticationToken authRequest = XnatAuthenticationFilter.buildUPTokenForAuthMethod(_authMethod, _username, _password);
        XnatProviderManager manager = XDAT.getContextService().getBean(XnatProviderManager.class);
        Authentication authentication = manager.doAuthentication(authRequest);
        if (authentication.isAuthenticated()) {
            succeed();
        } else {
            fail();
        }
    }

    private void succeed() {
        getResponse().setStatus(Status.SUCCESS_OK, "OK");
    }

    private void fail() {
        getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Authentication failed.");
    }

    private void extractCredentials(String text) {
        String[] entries = text.split("&");
        for (String entry : entries) {
            String[] atoms = entry.split("=", 2);
            if (atoms == null || atoms.length < 2) {
                // TODO: Just ignoring for now, should we do something here?
            } else {
                try {
                    if (atoms[0].equals("username") || atoms[0].equals("j_username")) {
                        _username = URLDecoder.decode(atoms[1], "UTF-8");
                    } else if (atoms[0].equals("password") || atoms[0].equals("j_password")) {
                        _password = URLDecoder.decode(atoms[1], "UTF-8");
                    } else if (atoms[0].equals("provider") || atoms[0].equals("login_method")) {
                        _authMethod = URLDecoder.decode(atoms[1], "UTF-8");
                    } else {
                        // TODO: Just ignoring for now, should we do something here?
                    }
                } catch (UnsupportedEncodingException e) {
                    // This is the dumbest exception in the history of humanity: the form of this method that doesn't
                    // specify an encoding is deprecated, so you have to specify an encoding. But the form of the method
                    // that takes an encoding (http://bit.ly/yX56fe) has an note that emphasizes that you should only
                    // use UTF-8 because "[n]ot doing so may introduce incompatibilities." Got it? You have to specify
                    // it, but it should always be the same thing. Oh, and BTW? You have to catch an exception for
                    // unsupported encodings because you may specify that one acceptable encoding or... something.
                    //
                    // I hate them.
                }
            }
        }
    }

    private String _authMethod;
    private String _username;
    private String _password;
}
