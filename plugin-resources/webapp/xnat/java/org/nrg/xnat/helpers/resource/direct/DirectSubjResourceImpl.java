/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectSubjResourceImpl extends DirectResourceModifierA {
	private final XnatProjectdata proj;
	private final XnatSubjectdata sub;
	
	public DirectSubjResourceImpl(final XnatProjectdata proj, final XnatSubjectdata sub){
		this.proj=proj;
		this.sub=sub;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure {
		return FileUtils.AppendRootPath(proj.getRootArchivePath(), "subjects/" + sub.getArchiveDirectoryName() +"/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource, org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public boolean addResource(XnatResource resource, XDATUser user) throws Exception {		
		sub.setResources_resource(resource);
		
		sub.save(user, false, false);
		return true;
	}

}
