// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.nrg.dcm.DicomSCP;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.noelios.restlet.ext.servlet.ServerServlet;

public class XNATRestletServlet extends ServerServlet {
    private static final long serialVersionUID = 1035552647328611333L;
    
    public static ServletConfig REST_CONFIG=null;
    @Override
    public void init() throws ServletException {
        super.init();

        XNATRestletServlet.REST_CONFIG=this.getServletConfig();

        try {
            PrearcDatabase.initDatabase();
        } catch (Exception e) {
        	logger().error("Unable to initialize prearchive database : ", e);
        }

        startDicomSCP();
    }
    
    private static Logger logger() { return LoggerFactory.getLogger(XNATRestletServlet.class); }
    
    /**
     * Create and start a DICOM SCP
     */
    public static void startDicomSCP() {
        try {
            final ArcArchivespecification arcspec = ArcSpecManager.GetInstance();

            final Properties properties = DicomSCP.getProperties();
            final XDATUser user;
            try {
                user = DicomSCP.getUser(properties); 
            } catch (Throwable t) {
                logger().error("Not starting DICOM C-STORE SCP: unable to get user", t);
                return;
            }
            final int port;
            try {
                port = DicomSCP.getPort(properties, arcspec.getDcm_dcmPort());
            } catch (Throwable t) {
                logger().error("Not starting DICOM C-STORE SCP: unable to get port", t);
                return;
            }

            final String aetitle = arcspec.getDcm_dcmAe();
            if (Strings.isNullOrEmpty(aetitle)) {
                logger().info("Not starting DICOM C-STORE SCP: no DICOM AE title defined");
            } else {
                DicomSCP.makeSCP(user, aetitle, port).start();
            }
        } catch (IOException e) {
            logger().error("error starting DICOM SCP", e);
        } catch (Throwable t) {
            logger().error("unexpected error starting DICOM SCP", t);
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }


}
