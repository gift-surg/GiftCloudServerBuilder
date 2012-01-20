// Copyright 2010,2011 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.dcm.DicomSCP;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        	String path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.SITE_WIDE, "");
        	Configuration init_config = AnonUtils.getInstance().getScript(path, null); 
        	if (init_config == null) {
        		logger().info("Creating Script Table.");
        		String site_wide = FileUtils.readFileToString(AnonUtils.getDefaultScript());
        		String adminUser = this.getAdminUser();
        		if (adminUser != null) {
        			AnonUtils.getInstance().setSiteWideScript(adminUser, path,site_wide);
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
