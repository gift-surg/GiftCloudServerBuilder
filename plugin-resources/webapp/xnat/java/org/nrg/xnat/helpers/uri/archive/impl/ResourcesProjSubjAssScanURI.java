package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.ArrayList;
import java.util.Map;

import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.ResourcesProjSubjSessionURIA;
import org.nrg.xnat.helpers.uri.archive.ScanURII;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;

public class ResourcesProjSubjAssScanURI extends ResourcesProjSubjSessionURIA  implements AssessedURII,ResourceURII,ArchiveItemURI,ScanURII{
	private XnatImagescandata scan=null;
	
	public ResourcesProjSubjAssScanURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateScan() {
		super.populateSession();

		if(scan==null){
			final String scanID= (String)props.get(URIManager.SCAN_ID);
			final XnatImagesessiondata session=this.getSession();
			
			if(scan==null&& scanID!=null){
				if(scan==null && session!=null){
					CriteriaCollection cc= new CriteriaCollection("AND");
					cc.addClause("xnat:imageScanData/ID", scanID);
					cc.addClause("xnat:imageScanData/image_session_ID", session.getId());
					ArrayList<XnatImagescandata> scans=XnatImagescandata.getXnatImagescandatasByField(cc, null, false);
					if(scans.size()>0){
						scan=scans.get(0);
					}
				}
			}
		}
	}

	public XnatImagescandata getScan(){
		this.populateScan();
		return this.scan;
	}

	@Override
	public ItemI getSecurityItem() {
		return getSession();
	}
}
