/*
 * org.nrg.xnat.helpers.resource.direct.DirectExptResourceImpl
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.resource.direct;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectExptResourceImpl extends ResourceModifierA {
	private final XnatProjectdata proj;
	private final XnatExperimentdata expt;
	
	public DirectExptResourceImpl(final XnatProjectdata proj,final XnatExperimentdata expt,final boolean overwrite, final XDATUser user, final EventMetaI ci){
		super(overwrite,user,ci);
		this.proj=proj;
		this.expt=expt;
	}
	
	public XnatProjectdata getProject(){
		return proj;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure,UnknownPrimaryProjectException {
		return FileUtils.AppendRootPath(expt.getCurrentSessionFolder(true),"RESOURCES/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource, org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public boolean addResource(XnatResource resource, final String type, XDATUser user) throws Exception {
		expt.setResources_resource(resource);
		
		SaveItemHelper.authorizedSave(expt,user, false, false,ci);
		
		return true;
	}



	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierA#getResourceById(java.lang.Integer)
	 */
	@Override
	public XnatAbstractresourceI getResourceById(Integer i, final String type) {
		for(XnatAbstractresourceI res: this.expt.getResources_resource()){
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
		for(XnatAbstractresourceI res: this.expt.getResources_resource()){
			if(StringUtils.isNotEmpty(res.getLabel()) && res.getLabel().equals(lbl)){
				return res;
			}
		}
		
		return null;
	}
}
