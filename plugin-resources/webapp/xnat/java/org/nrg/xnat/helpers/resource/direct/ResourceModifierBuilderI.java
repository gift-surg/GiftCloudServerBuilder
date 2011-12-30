package org.nrg.xnat.helpers.resource.direct;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatSubjectdata;
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


}