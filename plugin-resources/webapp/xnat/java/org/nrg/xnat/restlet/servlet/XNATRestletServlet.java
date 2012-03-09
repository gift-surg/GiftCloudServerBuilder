// Copyright 2010,2011 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import com.noelios.restlet.ext.servlet.ServerServlet;
import org.apache.commons.io.FileUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.dcm.DicomSCP;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class XNATRestletServlet extends ServerServlet {
    private static final long serialVersionUID = 1592366035839385170L;

    public static ServletConfig REST_CONFIG=null;

    /**
     * Get the username of the site administrator. If there are multiple
     * site admins, just get the first one. If none are found, return null.
     * @return
     */
    private String getAdminUser() throws Exception {
    	String admin = null;
        List<String> logins = (List<String>) XDATUser.getAllLogins();
    	for (String login : logins) {
    		XDATUser user = new XDATUser(login);
    		if (user.checkRole(PrearcUtils.ROLE_SITE_ADMIN)) {
    			admin = login;
    		}
    	}
    	return admin;
    }
    @Override
    public void init() throws ServletException {
        super.init();
		
		updateAuthTable();
		
        XNATRestletServlet.REST_CONFIG=this.getServletConfig();
        try {
        	String path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.SITE_WIDE, "");
        	Configuration init_config = AnonUtils.getService().getScript(path, null);
        	if (init_config == null) {
        		logger().info("Creating Script Table.");
        		String site_wide = FileUtils.readFileToString(AnonUtils.getDefaultScript());
        		String adminUser = this.getAdminUser();
        		if (adminUser != null) {
        			AnonUtils.getService().setSiteWideScript(adminUser, path,site_wide);
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
    }

    private static Logger logger() { return LoggerFactory.getLogger(XNATRestletServlet.class); }

    /**
     * Adds users from old xdat_user table to new user authentication table if they are not already there. New local database users now get added to both automatically, but this is necessary
     * so that those who upgrade from an earlier version will still have their users be able to log in. 
     */
    private void updateAuthTable(){
        JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
        List<XdatUserAuth> unmapped = template.query("SELECT login, enabled FROM xdat_user WHERE login NOT IN (SELECT xdat_username FROM xhbm_xdat_user_auth WHERE auth_method='localdb')", new RowMapper<XdatUserAuth>() {
            @Override
            public XdatUserAuth mapRow(final ResultSet resultSet, final int i) throws SQLException {
                final String login = resultSet.getString("login");
                final boolean enabled = resultSet.getInt("enabled") == 1;
                return new XdatUserAuth(login, "localdb", enabled, true, true, true, AuthorityUtils.NO_AUTHORITIES, login);
            }
        });
        for (XdatUserAuth userAuth : unmapped) {
            XDAT.getXdatUserAuthService().create(userAuth);
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }

    private DicomSCP dicomSCP;
}
