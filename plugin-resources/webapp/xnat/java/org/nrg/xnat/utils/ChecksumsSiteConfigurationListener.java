/**
 * ChecksumsSiteConfigurationListener
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 11/1/12 by rherri01
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
