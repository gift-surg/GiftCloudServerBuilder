package org.nrg.xnat.turbine.modules.screens;

import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xnat.utils.AppletConfig;

/**
 * Contains basic methods used by upload applet screen classes.
 */
public abstract class UploadAppletScreen extends SecureScreen {
    protected org.nrg.config.entities.Configuration getAppletConfiguration(final XDATUser user, final String projectName) {
        //grab the applet config. Project level if it exists, otherwise, do the site-wide
        ConfigService configService = XDAT.getConfigService();
        Long projectId = null;

        if(projectName != null){
            final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectName, user, false);
            try {
                if(user.canRead(("xnat:subjectData/project").intern(), p.getId())) {
                    projectId = (long) (Integer) p.getItem().getProps().get("projectdata_info");
                }
            } catch (Exception e) {
                projectId = null;
            }
        }

        boolean enableProjectAppletScript = XDAT.getBoolSiteConfigurationProperty("enableProjectAppletScript", false);
        org.nrg.config.entities.Configuration config = enableProjectAppletScript ? configService.getConfig(AppletConfig.toolName, AppletConfig.path, projectId) : null;

        if(config == null || org.nrg.config.entities.Configuration.DISABLED_STRING.equalsIgnoreCase(config.getStatus())) {
            //try to pull a site-wide config
            config = configService.getConfig(AppletConfig.toolName, AppletConfig.path, null);
        }
        return config;
    }
}
