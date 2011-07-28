package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ResourceURIA;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;

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
	public ItemI getSecurityItem() {
		return getSubject();
	}
}
