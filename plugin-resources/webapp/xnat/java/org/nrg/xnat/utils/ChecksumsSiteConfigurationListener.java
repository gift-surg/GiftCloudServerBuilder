/*
 * org.nrg.xnat.utils.ChecksumsSiteConfigurationListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:16 PM
 */
package org.nrg.xnat.utils;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.interfaces.SiteConfigurationPropertyChangedListener;

public class ChecksumsSiteConfigurationListener implements SiteConfigurationPropertyChangedListener {
    @Override
    public void siteConfigurationPropertyChanged(final String propertyName, final String newPropertyValue) {
        if (!StringUtils.isBlank(newPropertyValue) && (Boolean.TRUE.toString().equalsIgnoreCase(newPropertyValue) || Boolean.FALSE.toString().equalsIgnoreCase(newPropertyValue))) {
            CatalogUtils.DEFAULT_CHECKSUM = Boolean.parseBoolean(newPropertyValue);
        }
    }
}
