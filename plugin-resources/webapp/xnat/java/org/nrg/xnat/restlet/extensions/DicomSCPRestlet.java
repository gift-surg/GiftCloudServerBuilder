/*
 * org.nrg.xnat.restlet.extensions.DicomSCPRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */

package org.nrg.xnat.restlet.extensions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dcm.DicomSCPManager;
import org.nrg.xdat.XDAT;
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

import com.google.common.base.Joiner;

@XnatRestlet({"/services/dicomscp", "/services/dicomscp/{ACTION}"})
public class DicomSCPRestlet extends SecureResource {
    private static final Log _log = LogFactory.getLog(DicomSCPRestlet.class);
    private List<String> allowedActions;

    public DicomSCPRestlet(Context context, Request request, Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
        allowedActions = new ArrayList<String>(2);
        allowedActions.add("start");
        allowedActions.add("stop");
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (_log.isDebugEnabled()) {
            _log.debug("Passing a representation of the verify extensions restlet.");
        }

        DicomSCPManager dicomSCPManager = XDAT.getContextService().getBean(DicomSCPManager.class);
        if (null == dicomSCPManager) {
            getResponse().setStatus(Status.CLIENT_ERROR_FAILED_DEPENDENCY, "DicomSCP was not properly initialized.");
            return new StringRepresentation("ERROR: DicomSCP was not properly initialized.");
        }
        else if(dicomSCPManager.isDicomSCPStarted()) {
            return new StringRepresentation("DicomSCP service is enabled (listening).");
        }
        else {
            return new StringRepresentation("DicomSCP service is disabled (not listening).");
        }
    }
    
    public void performActionOnDicomSCP() {
        String action = (String) getRequest().getAttributes().get("ACTION");
        if(! allowedActions.contains(action)) {
        	respondToInvalidAction(action);
        }
        else if(action.equals("start")) {
        	XDAT.getContextService().getBean(DicomSCPManager.class).startDicomSCP();
            returnDefaultRepresentation();
        }
        else if(action.equals("stop")) {
        	XDAT.getContextService().getBean(DicomSCPManager.class).stopDicomSCP();
            returnDefaultRepresentation();
        }
    }
    
    private void respondToInvalidAction(String action) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, 
        		String.format("Action '%s' is not supported by this resource.  Valid actions are: %s", action, Joiner.on(",").join(allowedActions))
        );
    }
    
    @Override
    public boolean allowPost() {
    	return true;
    }

    @Override
    public void handlePost() {
    	performActionOnDicomSCP();
    }

    @Override
    public boolean allowPut() {
    	return true;
    }

    @Override
    public void handlePut() {
    	performActionOnDicomSCP();
    }
}
