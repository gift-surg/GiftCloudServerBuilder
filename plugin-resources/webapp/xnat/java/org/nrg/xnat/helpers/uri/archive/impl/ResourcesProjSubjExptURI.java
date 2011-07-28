package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;

public class ResourcesProjSubjExptURI extends ResourcesProjSubjURI implements ResourceURII,ArchiveItemURI,ExperimentURII{
	private XnatExperimentdata expt=null;
	
	public ResourcesProjSubjExptURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateExperiment(){
		super.populateSubject();
		
		if(expt==null){
			final XnatProjectdata proj=getProject();
			
			final String exptID= (String)props.get(URIManager.EXPT_ID);
			
			if(proj!=null){
				expt=XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), exptID,null, false);
			}
			
			if(expt==null){
				expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
				if(expt!=null && (proj!=null && !expt.hasProject(proj.getId()))){
					expt=null;
				}
			}
		}
	}
	
	public XnatExperimentdata getExperiment(){
		this.populateExperiment();
		return expt;
	}

	@Override
	public ItemI getSecurityItem() {
		return getExperiment();
	}
}
