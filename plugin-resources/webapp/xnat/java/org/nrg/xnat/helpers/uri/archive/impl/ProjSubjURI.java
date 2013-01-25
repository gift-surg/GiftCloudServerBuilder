package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.List;
import java.util.Map;

import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;

import com.google.common.collect.Lists;

public class ProjSubjURI extends ProjURI  implements ArchiveItemURI,SubjectURII{
	private XnatSubjectdata subject=null;
	
	public ProjSubjURI(Map<String, Object> props, String uri) {
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

	@Override
	public List<XnatAbstractresourceI> getResources(boolean includeAll) {
		List<XnatAbstractresourceI> res=Lists.newArrayList();
		final XnatSubjectdata expt=getSubject();
		res.addAll(expt.getResources_resource());
		return res;
	}
}
