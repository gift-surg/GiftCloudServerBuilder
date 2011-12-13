// Copyright 2010,2011 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.nrg.dcm.DicomSCP;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.noelios.restlet.ext.servlet.ServerServlet;

public class XNATRestletServlet extends ServerServlet {
    private static final long serialVersionUID = 1592366035839385170L;

    private DicomSCP dicomSCP = null;

    public static ServletConfig REST_CONFIG=null;
    @Override
    public void init() throws ServletException {
        super.init();

        XNATRestletServlet.REST_CONFIG=this.getServletConfig();

        try {
            PrearcDatabase.initDatabase();
        } catch (Exception e) {
            logger().error("Unable to initialize prearchive database", e);
        }

        final ContextService context = XDAT.getContextService();
        dicomSCP = context.getBean("dicomSCP", DicomSCP.class);
        if (null != dicomSCP) {
            try {
                dicomSCP.start();
            } catch (Throwable t) {
                throw new ServletException("unable to start DICOM SCP", t);
            }
        }
    }

    @Override
    public void destroy() {
        if (null != dicomSCP) {
            dicomSCP.stop();
            dicomSCP = null;
        }
        super.destroy();
    }

    private static Logger logger() { return LoggerFactory.getLogger(XNATRestletServlet.class); }


    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }
}
