// Copyright 2010,2011 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.io.FileUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.services.ConfigService;
import org.nrg.dcm.DicomSCP;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.dcm.xnat.ScriptTableDAO;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.noelios.restlet.ext.servlet.ServerServlet;

public class XNATRestletServlet extends ServerServlet {
    private static final long serialVersionUID = 1592366035839385170L;

    private DicomSCP dicomSCP = null;
    public static ServletConfig REST_CONFIG=null;
    
    /**
     * Get the username of the site administrator. If there are multiple 
     * site admins, just get the first one. If none are found, return null.
     * @return
     */
    private String getAdminUser() throws Exception {
    	String admin = null;
    	Iterator<String> logins = XDATUser.getAllLogins().iterator();
    	while(logins.hasNext()) {
    		String l = logins.next();
    		XDATUser u = new XDATUser(l);
    		if (u.checkRole(PrearcUtils.ROLE_SITE_ADMIN)) {
    			admin = l;
    		}
    	}
    	return admin;
    }
    @Override
    public void init() throws ServletException {
        super.init();

        XNATRestletServlet.REST_CONFIG=this.getServletConfig();
        
        try {
        	ConfigService configService = XDAT.getConfigService();
        	String path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.SITE_WIDE, "");
        	Configuration init_config = configService.getConfig(DicomEdit.ToolName,path);
        	if (init_config == null) {
        		logger().info("Creating Script Table.");
        		String site_wide = FileUtils.readFileToString(AnonUtils.getDefaultScript());
        		String adminUser = this.getAdminUser();
        		if (adminUser != null) {
        			configService.replaceConfig(adminUser, 
        										"", 
        										DicomEdit.ToolName, 
        										path,
        										site_wide);
        		}
        		else {
        			throw new Exception("Site administrator not found.");
        		}
        	}
        	else {
        		// there is a default site-wide script, nothing to do here.
        	}
        }
        catch (Throwable e){
        	logger().error("Unable to either find or initialize script database: " + e.getMessage());
        }

        try {
            PrearcDatabase.initDatabase();
        } catch (Exception e) {
            logger().error("Unable to initialize prearchive database", e);
        }

        dicomSCP = startDicomSCP();
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

    /**
     * Create and start a DICOM SCP
     */
    public static DicomSCP startDicomSCP() {
        try {
            final ArcArchivespecification arcspec = ArcSpecManager.GetInstance();

            final Properties properties = DicomSCP.getProperties();
            final XDATUser user;
            try {
                user = DicomSCP.getUser(properties); 
            } catch (Throwable t) {
                logger().error("Not starting DICOM C-STORE SCP: unable to get user", t);
                return null;
            }
            final int port;
            try {
                port = DicomSCP.getPort(properties, arcspec.getDcm_dcmPort());
            } catch (Throwable t) {
                logger().error("Not starting DICOM C-STORE SCP: unable to get port", t);
                return null;
            }

            final String aetitle = arcspec.getDcm_dcmAe();
            if (Strings.isNullOrEmpty(aetitle)) {
                logger().info("Not starting DICOM C-STORE SCP: no DICOM AE title defined");
                return null;
            } else {
                final DicomSCP scp = DicomSCP.makeSCP(user, aetitle, port);
                scp.start();
                return scp;
            }
        } catch (IOException e) {
            logger().error("error starting DICOM SCP", e);
            return null;
        } catch (Throwable t) {
            logger().error("unexpected error starting DICOM SCP", t);
            return null;
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }


}
