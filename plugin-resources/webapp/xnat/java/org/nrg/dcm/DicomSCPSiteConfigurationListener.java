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
