// Copyright 2010-2012 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.dcm.DicomSCP;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.prearchive.PrearcConfig;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.authority.AuthorityUtils;

import com.noelios.restlet.ext.servlet.ServerServlet;

public class XNATRestletServlet extends ServerServlet {
    private static final long serialVersionUID = -4149339105144231596L;

    public static ServletConfig REST_CONFIG=null;

    private final Logger logger = LoggerFactory.getLogger(XNATRestletServlet.class);
    private DicomSCP dicomSCP = null;

    /**
     * Get the username of the site administrator. If there are multiple
     * site admins, just get the first one. If none are found, return null.
     * @return
     */
    private String getAdminUser() throws Exception {
        String admin = null;
        Collection<String> logins = (Collection<String>) XDATUser.getAllLogins();
        for (String login : logins) {
            XDATUser user = new XDATUser(login);
            if (user.checkRole(PrearcUtils.ROLE_SITE_ADMIN)) {
                admin = login;
                break;
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
                logger.info("Creating Script Table.");
                String site_wide = FileUtils.readFileToString(AnonUtils.getDefaultScript());
                String adminUser = this.getAdminUser();
                if (adminUser != null) {
                    AnonUtils.getService().setSiteWideScript(adminUser, path,site_wide);
                } else {
                    throw new Exception("Site administrator not found.");
                }
            } else {
                // there is a default site-wide script, nothing to do here.
            }
        } catch (Throwable e){
            logger.error("Unable to either find or initialize script database", e);
        }

        PrearcConfig prearcConfig = XDAT.getContextService().getBean(PrearcConfig.class);
        try {
            PrearcDatabase.initDatabase(prearcConfig.isReloadPrearcDatabaseOnApplicationStartup());
        } catch (Throwable e) {
            logger.error("Unable to initialize prearchive database", e);
        }

        final ContextService context = XDAT.getContextService();
        dicomSCP = context.getBean("dicomSCP", DicomSCP.class);
        if (null != dicomSCP) {
            try {
                logger.debug("starting {}", dicomSCP);
                dicomSCP.start();
            } catch (Throwable t) {
                throw new ServletException("unable to start DICOM SCP", t);
            }
        }
    }

    /**
     * Adds users from /old xdat_user table to new user authentication table if they are not already there. New local database users now get added to both automatically, but this is necessary
     * so that those who upgrade from an earlier version will still have their users be able to log in. Password expiry times are also added so that pre-existing users still have their passwords expire.
     */
    private void updateAuthTable(){
        JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
        List<XdatUserAuth> unmapped = template.query("SELECT login, enabled FROM xdat_user WHERE login NOT IN (SELECT xdat_username FROM xhbm_xdat_user_auth WHERE auth_method='"+XdatUserAuthService.LOCALDB+"')", new RowMapper<XdatUserAuth>() {
            @Override
            public XdatUserAuth mapRow(final ResultSet resultSet, final int i) throws SQLException {
                final String login = resultSet.getString("login");
                final boolean enabled = resultSet.getInt("enabled") == 1;
                return new XdatUserAuth(login, XdatUserAuthService.LOCALDB, enabled, true, true, true, AuthorityUtils.NO_AUTHORITIES, login,0);
            }
        });
        for (XdatUserAuth userAuth : unmapped) {
            XDAT.getXdatUserAuthService().create(userAuth);
        }
        template.execute("UPDATE xhbm_xdat_user_auth SET password_updated=current_timestamp WHERE auth_method='"+XdatUserAuthService.LOCALDB+"' AND password_updated IS NULL");   
    }

    @Override
    public void destroy() {
        logger.debug("stopping {}", dicomSCP);
        dicomSCP.stop();
        dicomSCP = null;
    }

    public DicomSCP getDicomSCP() { return dicomSCP; }
}