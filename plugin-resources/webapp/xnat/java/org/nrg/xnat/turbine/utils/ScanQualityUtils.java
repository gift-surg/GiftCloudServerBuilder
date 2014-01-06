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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public final class ScanQualityUtils {
    private static final List<String> DEFAULT_LABELS = new ArrayList<String>();
    static {
        DEFAULT_LABELS.add("usable");
        DEFAULT_LABELS.add("questionable");
        DEFAULT_LABELS.add("unusable");
    }

    private static Logger _log = LoggerFactory.getLogger(ScanQualityUtils.class);

    private ScanQualityUtils() {}

    public static List<String> getQualityLabels(final String project, final UserI user) {
        final ConfigService configService = XDAT.getConfigService();
        final Long projectId;
        if (Strings.isNullOrEmpty(project) || StringUtils.equals("Unassigned",project)) {
            projectId = null;
        } else {
            final XnatProjectdata projectData = XnatProjectdata.getXnatProjectdatasById(project, user, false);
            projectId = (long) (Integer) projectData.getItem().getProps().get("projectdata_info");
        }
        final String configVal = configService.getConfigContents("scan-quality", "labels", projectId);
        if (Strings.isNullOrEmpty(configVal)) {
            if (Strings.isNullOrEmpty(project)) {
                try {
                    XDAT.getConfigService().replaceConfig("admin", "Set default scan quality labels configuration", "scan-quality", "labels", true, Joiner.on(", ").join(DEFAULT_LABELS));
                } catch (ConfigServiceException e) {
                    _log.error("An error occurred trying to save the default scan quality labels", e);
                }
                return DEFAULT_LABELS;
            } else {
                return getQualityLabels(null, user);    // try the site config
            }
        } else {
            return Arrays.asList(configVal.trim().split("\\s*,\\s*"));
        }
    }
}
