/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author timo
 *
 */
public class DirectScanResourceImpl extends DirectResourceModifierA{
	private final XnatImagescandata scan;
	private final XnatImagesessiondata session;

	public DirectScanResourceImpl(final XnatImagescandata scan,final XnatImagesessiondata session){
		this.scan=scan;
		this.session=session;
		
		if(session==null){
			throw new NullPointerException("Must reference a valid imaging session");
		}
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
	public boolean addResource(final XnatResource resource, final XDATUser user) throws Exception {
		if(scan.getFile().size()==0){
			if(resource.getContent()==null && scan.getType()!=null){
				resource.setContent("RAW");
			}
		}
		
		scan.setFile(resource);
		
		scan.save(user, false, false);
		return true;
	}

}
