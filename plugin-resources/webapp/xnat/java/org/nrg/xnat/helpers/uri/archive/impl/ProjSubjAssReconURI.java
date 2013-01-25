package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.List;
import java.util.Map;

import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;
import org.nrg.xnat.helpers.uri.archive.ProjSubjSessionURIA;
import org.nrg.xnat.helpers.uri.archive.ReconURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;

import com.google.common.collect.Lists;

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
	public ArchivableItem getSecurityItem() {
		return getSession();
	}

	@Override
	public List<XnatAbstractresourceI> getResources(boolean includeAll) {
		List<XnatAbstractresourceI> res=Lists.newArrayList();
		final XnatReconstructedimagedata expt=getRecon();
		res.addAll(expt.getOut_file());
		return res;
	}
}