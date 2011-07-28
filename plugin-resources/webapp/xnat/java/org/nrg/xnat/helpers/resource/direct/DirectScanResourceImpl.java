/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectScanResourceImpl extends ResourceModifierA{
	private final XnatImagescandata scan;
	private final XnatImagesessiondata session;

	public DirectScanResourceImpl(final XnatImagescandata scan,final XnatImagesessiondata session,final boolean overwrite, final XDATUser user){
		super(overwrite,user);
		this.scan=scan;
		this.session=session;
		
		if(session==null){
			throw new NullPointerException("Must reference a valid imaging session");
		}
	}
	
	public XnatProjectdata getProject(){
		return session.getProjectData();
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure {
		return FileUtils.AppendRootPath(session.getCurrentSessionFolder(true), "SCANS/" + scan.getId() +"/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource)
	 */
	@Override
	public boolean addResource(final XnatResource resource, final String type, final XDATUser user) throws Exception {
		if(scan.getFile().size()==0){
			if(resource.getContent()==null && scan.getType()!=null){
				resource.setContent("RAW");
			}
		}
		
		scan.setFile(resource);
		
		scan.save(user, false, false);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierA#getResourceById(java.lang.Integer)
	 */
	@Override
	public XnatAbstractresourceI getResourceById(Integer i, final String type) {
		for(XnatAbstractresourceI res: this.scan.getFile()){
			if(res.getXnatAbstractresourceId().equals(i)){
				return res;
			}
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierA#getResourceByLabel(java.lang.String)
	 */
	@Override
	public XnatAbstractresourceI getResourceByLabel(String lbl, final String type) {
		for(XnatAbstractresourceI res: this.scan.getFile()){
			if(StringUtils.isNotEmpty(res.getLabel()) && res.getLabel().equals(lbl)){
				return res;
			}
		}
		
		return null;
	}

}
