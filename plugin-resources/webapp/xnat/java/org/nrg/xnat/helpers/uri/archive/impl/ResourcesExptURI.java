package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;
import org.nrg.xnat.helpers.uri.archive.ResourceURIA;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;

public class ResourcesExptURI extends ResourceURIA implements ArchiveItemURI,ResourceURII,ExperimentURII{
	private XnatExperimentdata expt=null;
	
	public ResourcesExptURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populate(){
		if(expt==null){
			
			final String exptID= (String)props.get(URIManager.EXPT_ID);
			
			if(expt==null){
				expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
			}
		}
	}
	
	public XnatExperimentdata getExperiment(){
		this.populate();
		return expt;
	}

	@Override
	public ItemI getSecurityItem() {
		return getExperiment();
	}
}
