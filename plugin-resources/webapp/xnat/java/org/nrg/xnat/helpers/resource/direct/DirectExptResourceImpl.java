/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectExptResourceImpl extends DirectResourceModifierA {
	private final XnatProjectdata proj;
	private final XnatExperimentdata expt;
	
	public DirectExptResourceImpl(final XnatProjectdata proj,final XnatExperimentdata expt){
		this.proj=proj;
		this.expt=expt;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure {
		return FileUtils.AppendRootPath(proj.getRootArchivePath(), expt.getId() + "/RESOURCES/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource, org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public boolean addResource(XnatResource resource, XDATUser user) throws Exception {
		expt.setResources_resource(resource);
		
		expt.save(user, false, false);
		
		return true;
	}

}
