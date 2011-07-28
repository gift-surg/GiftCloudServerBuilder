package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;

public class ExptURI extends ArchiveURI implements ArchiveItemURI,ExperimentURII {
	private XnatExperimentdata expt=null;
	
	public ExptURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateExperiment(){
		if(expt==null){
			
			final String exptID= (String)props.get(URIManager.EXPT_ID);
			
			if(expt==null){
				expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
			}
		}
	}
	
	public XnatExperimentdata getExperiment(){
		this.populateExperiment();
		return expt;
	}

	public ItemI getSecurityItem() {
		return getExperiment();
	}
}
