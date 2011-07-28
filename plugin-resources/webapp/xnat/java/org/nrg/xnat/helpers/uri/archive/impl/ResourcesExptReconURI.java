package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ReconURII;
import org.nrg.xnat.helpers.uri.archive.ResourceURIA;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ResourcesExptReconURI extends ResourceURIA implements ArchiveItemURI,AssessedURII,ResourceURII,ReconURII{
	private XnatReconstructedimagedata recon=null;
	private XnatImagesessiondata session=null; 
	
	public ResourcesExptReconURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populate() {
		if(recon==null){
			final String exptID= (String)props.get(URIManager.ASSESSED_ID);
			
			if(session==null){
				session=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(exptID, null, false);
			}
			
			final String reconID= (String)props.get(URIManager.RECON_ID);
			
			if(recon==null&& reconID!=null){
				recon=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasById(reconID, null, false);
			}
		}
	}

	public XnatImagesessiondata getSession(){
		this.populate();
		return this.session;
	}

	public XnatReconstructedimagedata getRecon(){
		this.populate();
		return this.recon;
	}

	@Override
	public ItemI getSecurityItem() {
		return getSession();
	}
}
