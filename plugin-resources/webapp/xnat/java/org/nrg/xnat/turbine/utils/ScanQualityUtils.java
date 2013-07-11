/*
 * org.nrg.xnat.turbine.utils.ScanQualityUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.utils;

import com.google.common.base.Strings;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;

import java.util.Arrays;
import java.util.List;

public final class ScanQualityUtils {
    private ScanQualityUtils() {}

    public static List<String> getQualityLabels(final String project, final UserI user) {
        final ConfigService configService = XDAT.getConfigService();
        final Long projectId;
        if (Strings.isNullOrEmpty(project)) {
            projectId = null;
        } else {
            final XnatProjectdata projectData = XnatProjectdata.getXnatProjectdatasById(project, user, false);
            projectId = (long) (Integer) projectData.getItem().getProps().get("projectdata_info");
        }
        final String configVal = configService.getConfigContents("scan-quality", "labels", projectId);
        if (Strings.isNullOrEmpty(configVal)) {
            if (Strings.isNullOrEmpty(project)) {
                return Arrays.asList("usable", "questionable", "unusable");
            } else {
                return getQualityLabels(null, user);    // try the site config
            }
        } else {
            return Arrays.asList(configVal.trim().split(","));
        }
    }
}
