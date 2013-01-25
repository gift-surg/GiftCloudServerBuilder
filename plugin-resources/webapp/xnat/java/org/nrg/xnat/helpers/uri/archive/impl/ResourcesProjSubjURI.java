package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;

public class ResourcesProjSubjURI extends ResourcesProjURI implements ResourceURII,ArchiveItemURI,SubjectURII{
	private XnatSubjectdata subject=null;
	
	public ResourcesProjSubjURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	protected void populateSubject(){
		super.populateProject();
		
		if(subject==null){
			final XnatProjectdata proj=getProject();
			
			final String subID= (String)props.get(URIManager.SUBJECT_ID);
			
			if(proj!=null){
				subject=XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID,null, false);
			}
			
			if(subject==null){
				subject=XnatSubjectdata.getXnatSubjectdatasById(subID, null, false);
				if(subject!=null && (proj!=null && !subject.hasProject(proj.getId()))){
					subject=null;
				}
			}
		}
	}
	
	public XnatSubjectdata getSubject(){
		this.populateSubject();
		return subject;
	}

	@Override
	public ArchivableItem getSecurityItem() {
		return getSubject();
	}
}
