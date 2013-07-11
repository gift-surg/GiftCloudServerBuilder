/*
 * org.nrg.dcm.DicomSCPSiteConfigurationListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.dcm;

import org.nrg.config.interfaces.SiteConfigurationPropertyChangedListener;
import org.nrg.xdat.XDAT;

public class DicomSCPSiteConfigurationListener implements
		SiteConfigurationPropertyChangedListener {
	
	@Override
	public void siteConfigurationPropertyChanged(String propertyName,
			String newPropertyValue) {
        XDAT.getContextService().getBean(DicomSCPManager.class).startOrStopDicomSCPAsDictatedByConfiguration();
	}
}
