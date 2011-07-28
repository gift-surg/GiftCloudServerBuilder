package org.nrg.xnat.helpers.uri.archive;

import java.util.Map;

import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.impl.ProjSubjURI;

public abstract class ProjSubjSessionURIA extends ProjSubjURI  implements ArchiveItemURI{
	private XnatImagesessiondata assessed=null;

	public ProjSubjSessionURIA(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateSession(){
		super.populateSubject();
		
		if(assessed==null){
			final XnatProjectdata proj=getProject();
			
			final String exptID= (String)props.get(URIManager.ASSESSED_ID);
			
			if(proj!=null){
				assessed=(XnatImagesessiondata)XnatImagesessiondata.GetExptByProjectIdentifier(proj.getId(), exptID,null, false);
			}
			
			if(assessed==null){
				assessed=(XnatImagesessiondata)XnatImagesessiondata.getXnatExperimentdatasById(exptID, null, false);
				if(assessed!=null && (proj!=null && !assessed.hasProject(proj.getId()))){
					assessed=null;
				}
			}
		}
	}
	
	public XnatImagesessiondata getSession(){
		populateSession();
		return assessed;
	}
}
