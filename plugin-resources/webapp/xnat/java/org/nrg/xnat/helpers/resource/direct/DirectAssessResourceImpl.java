/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectAssessResourceImpl extends DirectResourceModifierA {
	private final XnatImageassessordata expt;
	private final XnatImagesessiondata session;
	private final String type;
	
	public DirectAssessResourceImpl(final XnatImageassessordata expt, final XnatImagesessiondata session, final String type){
		this.expt=expt;
		this.session=session;
		this.type=type;
		
		if(session==null){
			throw new NullPointerException("Must reference a valid imaging session");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure {
		String path=FileUtils.AppendRootPath(((XnatImagesessiondata)session).getCurrentSessionFolder(true), "ASSESSORS/" + expt.getId() +"/");
		
		if(type!=null){
			path=FileUtils.AppendRootPath(path,type);
		}
		
		return path;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource, org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public boolean addResource(XnatResource resource, XDATUser user) throws Exception {
		XnatImageassessordata iad = (XnatImageassessordata)expt;
		if(type!=null){
			if(type.equals("in")){
				iad.setIn_file(resource);
			}else{
				iad.setOut_file(resource);
			}
		}else{
			iad.setOut_file(resource);
		}
		
		iad.save(user, false, false);
		return true;
	}

}
