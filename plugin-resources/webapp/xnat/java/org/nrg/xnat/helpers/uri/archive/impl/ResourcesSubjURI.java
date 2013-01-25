package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ResourceURIA;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;

public class ResourcesSubjURI extends ResourceURIA implements ArchiveItemURI,ResourceURII,SubjectURII{
	private XnatSubjectdata subj=null;
	
	public ResourcesSubjURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populate(){
		if(subj==null){
			
			final String subjID= (String)props.get(URIManager.SUBJECT_ID);
			
			if(subj==null){
				subj=XnatSubjectdata.getXnatSubjectdatasById(subjID, null, false);
			}
		}
	}
	
	public XnatSubjectdata getSubject(){
		this.populate();
		return subj;
	}
	
	@Override
	public ArchivableItem getSecurityItem() {
		return getSubject();
	}


	@Override
	public XnatAbstractresourceI getXnatResource() {
		if(this.getSubject()!=null){
			for(XnatAbstractresourceI res:this.getSubject().getResources_resource()){
				if(StringUtils.equals(res.getLabel(), this.getResourceLabel())){
					return res;
				}
			}
		}
		
		return null;
	}

	@Override
	public XnatProjectdata getProject() {
		return this.getSubject().getProjectData();
	}
}
