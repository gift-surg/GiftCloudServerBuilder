package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ReconURII;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.ResourcesProjSubjSessionURIA;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ResourcesProjSubjAssReconURI extends ResourcesProjSubjSessionURIA  implements AssessedURII,ResourceURII,ArchiveItemURI,ReconURII{
	private XnatReconstructedimagedata recon=null;
	
	public ResourcesProjSubjAssReconURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateRecon() {
		super.populateSession();

		if(recon==null){
			final String reconID= (String)props.get(URIManager.RECON_ID);
			
			if(recon==null&& reconID!=null){
				recon=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasById(reconID, null, false);
			}
		}
	}

	public XnatReconstructedimagedata getRecon(){
		this.populateRecon();
		return this.recon;
	}

	@Override
	public ItemI getSecurityItem() {
		return getSession();
	}
}