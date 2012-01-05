package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ProjSubjSessionURIA;
import org.nrg.xnat.helpers.uri.archive.ReconURII;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ProjSubjAssReconURI extends ProjSubjSessionURIA  implements ArchiveItemURI,AssessedURII,ReconURII{
	private XnatReconstructedimagedata recon=null;
	
	public ProjSubjAssReconURI(Map<String, Object> props, String uri) {
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