/*
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 * Released under the Simplified BSD License
 */

/**
 * VerifyExtensionsRestlet
 * Created on 11/9/11 by rherri01
 */
package org.nrg.xnat.restlet.extensions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dcm.DicomSCP;
import org.nrg.framework.services.ContextService;
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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

@XnatRestlet("/services/dicomscp")
public class DicomSCPRestlet extends SecureResource {
    private static final Log _log = LogFactory.getLog(DicomSCPRestlet.class);

    public DicomSCPRestlet(Context context, Request request, Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
        DicomSCP dicomSCP = null;
        try {
            final ContextService contextService = XDAT.getContextService();
            dicomSCP = contextService.getBean(DicomSCP.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            //
        } finally {
            _dicomSCP = dicomSCP;
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (_log.isDebugEnabled()) {
            _log.debug("Passing a representation of the verify extensions restlet.");
        }

        if (_dicomSCP == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_FAILED_DEPENDENCY, "DicomSCP was not properly initialized.");
            return new StringRepresentation("DicomSCP was not properly initialized.");
    }

        return new StringRepresentation("DicomSCP service was properly initialized.");
}
    
    private final DicomSCP _dicomSCP;
}
