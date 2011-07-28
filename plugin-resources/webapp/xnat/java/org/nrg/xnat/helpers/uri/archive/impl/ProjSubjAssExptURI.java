package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.AssessorURII;
import org.nrg.xnat.helpers.uri.archive.ProjSubjSessionURIA;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ProjSubjAssExptURI extends ProjSubjSessionURIA  implements ArchiveItemURI,AssessedURII,AssessorURII{
	private XnatImageassessordata expt=null;
	
	public ProjSubjAssExptURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}
	
	protected void populateExperiment(){
		super.populateSession();
		
		if(expt==null){
			final XnatProjectdata proj=getProject();
			
			final String exptID= (String)props.get(URIManager.EXPT_ID);
			
			if(proj!=null){
				expt=(XnatImageassessordata)XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), exptID,null, false);
			}
			
			if(expt==null){
				expt=(XnatImageassessordata)XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
				if(expt!=null && (proj!=null && !expt.hasProject(proj.getId()))){
					expt=null;
				}
			}
		}
	}
	
	public XnatImageassessordata getAssessor(){
		this.populateExperiment();
		return expt;
	}

	@Override
	public ItemI getSecurityItem() {
		return getAssessor();
	}
}