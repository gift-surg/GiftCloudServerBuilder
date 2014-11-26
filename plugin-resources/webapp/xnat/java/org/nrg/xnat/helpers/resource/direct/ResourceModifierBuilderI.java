/*
 * org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.*;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;

public interface ResourceModifierBuilderI {

	public abstract ResourceModifierA buildResourceModifier(final boolean overwrite, final XDATUser user,final EventMetaI ci) throws Exception;

	public abstract void setProject(XnatProjectdata project);

	public abstract XnatProjectdata getProject();

	public abstract void setSubject(XnatProjectdata project, XnatSubjectdata subject);

	public abstract XnatSubjectdata getSubject();

	public abstract void setExpt(XnatProjectdata project, XnatExperimentdata expt);

	public abstract XnatExperimentdata getExpt();

	public abstract void setAssess(XnatImagesessiondata assessed, XnatImageassessordata assess, String type);

	public abstract XnatImageassessordata getAssess();

	public abstract void setScan(XnatImagesessiondata assessed, XnatImagescandata scan);

	public abstract XnatImagescandata getScan();

	public abstract void setRecon(XnatImagesessiondata assessed, XnatReconstructedimagedata recon, String type);

	public abstract XnatReconstructedimagedata getRecon();

	public abstract void setType(String type);

	public abstract String getType();
	
	public abstract void setPseudonymizedsubject(XnatProjectdata project, ExtPseudonymizedsubjectdata subject);

	public abstract ExtPseudonymizedsubjectdata getPseudonymizedsubject();
	
	public abstract void setPseudonym(ExtSubjectpseudonym pseudonym);

	public abstract ExtSubjectpseudonym getPseudonym();
	
}