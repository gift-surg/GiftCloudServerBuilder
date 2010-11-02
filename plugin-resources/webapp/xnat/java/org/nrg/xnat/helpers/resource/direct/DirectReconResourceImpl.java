/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectReconResourceImpl extends DirectResourceModifierA {
	private final XnatReconstructedimagedata recon;
	private final XnatImagesessiondata session;
	private final String type;
	
	public DirectReconResourceImpl(final XnatReconstructedimagedata recon, final XnatImagesessiondata session, final String type){
		this.recon=recon;
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
		return FileUtils.AppendRootPath(session.getCurrentSessionFolder(true), "PROCESSED/" + recon.getId() +"/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource, org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public boolean addResource(XnatResource resource, XDATUser user) throws Exception {		
		if(type!=null){
			if(type.equals("in")){
				recon.setIn_file(resource);
			}else{
				recon.setOut_file(resource);
			}
		}else{
			recon.setOut_file(resource);
		}
		
		recon.save(user, false, false);
		return true;
	}

}
