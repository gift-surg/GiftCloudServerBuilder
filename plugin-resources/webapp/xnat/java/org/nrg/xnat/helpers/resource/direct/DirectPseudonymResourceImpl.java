/*
 * org.nrg.xnat.helpers.resource.direct.DirectScanResourceImpl
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.resource.direct;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.ExtPseudonymizedsubjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
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
public class DirectPseudonymResourceImpl extends ResourceModifierA{
	private final ExtSubjectpseudonym pseudonym;
	private final XnatSubjectdata subject;

	public DirectPseudonymResourceImpl(final XnatSubjectdata subject,final ExtSubjectpseudonym pseudonym,final boolean overwrite, final XDATUser user, final EventMetaI ci){
		super(overwrite,user,ci);
		this.pseudonym=pseudonym;
		this.subject=subject;
		
		if(subject==null){
			throw new NullPointerException("Must reference a valid subject");
		}
	}
	
	public XnatProjectdata getProject(){
		return subject.getProjectData();
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.DirectResourceModifierA#buildDestinationPath()
	 */
	@Override
	public String buildDestinationPath() throws InvalidArchiveStructure, UnknownPrimaryProjectException {
		return FileUtils.AppendRootPath(subject.getArchiveDirectoryName(), "PSEUDONYMS/" + pseudonym.getId() +"/");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.DirectResourceModifierA#addResource(org.nrg.xdat.om.XnatResource)
	 */
	@Override
	public boolean addResource(final XnatResource resource, final String type, final XDATUser user) throws Exception {
		// TODO ___ pseudonym-related
		return true;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierA#getResourceById(java.lang.Integer)
	 */
	@Override
	public XnatAbstractresourceI getResourceById(Integer i, final String type) {
		// TODO ___ pseudonym-related
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierA#getResourceByLabel(java.lang.String)
	 */
	@Override
	public XnatAbstractresourceI getResourceByLabel(String lbl, final String type) {
		// TODO ___ pseudonym-related
		return null;
	}

}
