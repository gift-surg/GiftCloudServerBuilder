package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.AssessorURII;
import org.nrg.xnat.helpers.uri.archive.ResourceURIA;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ResourcesExptAssessorURI  extends ResourceURIA implements ArchiveItemURI,AssessedURII,ResourceURII,AssessorURII{
	private XnatImageassessordata expt=null;
	private XnatImagesessiondata session=null; 
	
	public ResourcesExptAssessorURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}
	
	protected void populate(){		
		if(expt==null){
			final String sessID= (String)props.get(URIManager.ASSESSED_ID);
			
			if(session==null){
				session=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(sessID, null, false);
			}
			
			final String exptID= (String)props.get(URIManager.EXPT_ID);
			
			if(expt==null){
				expt=(XnatImageassessordata)XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
			}
		}
	}

	public XnatImagesessiondata getSession(){
		this.populate();
		return this.session;
	}
	
	public XnatImageassessordata getAssessor(){
		this.populate();
		return expt;
	}

	@Override
	public ItemI getSecurityItem() {
		return getAssessor();
	}
}
