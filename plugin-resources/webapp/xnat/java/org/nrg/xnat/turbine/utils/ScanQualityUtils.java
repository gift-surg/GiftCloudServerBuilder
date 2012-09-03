/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.xnat.turbine.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;

import com.google.common.base.Strings;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class ScanQualityUtils {
    private ScanQualityUtils() {}

    public static List<String> getQualityLabels(final String project, final UserI user) {
        final ConfigService configService = XDAT.getConfigService();
        final Callable<Long> getProjectId;
        if (Strings.isNullOrEmpty(project)) {
            getProjectId = new Callable<Long>() { public Long call() { return null; }};
        } else {
            final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(project, user, false);
            getProjectId = new Callable<Long>() {
                public Long call() {
                    return Long.valueOf((Integer)p.getItem().getProps().get("projectdata_info"));
                }
            };
        }
        final String configVal = configService.getConfigContents("quality-labels", "/labels", getProjectId);
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
