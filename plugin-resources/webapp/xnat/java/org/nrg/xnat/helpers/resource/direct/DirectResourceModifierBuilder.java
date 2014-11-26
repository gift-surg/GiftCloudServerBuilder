/*
 * org.nrg.xnat.helpers.resource.direct.DirectResourceModifierBuilder
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

public class DirectResourceModifierBuilder implements ResourceModifierBuilderI {
	private XnatReconstructedimagedata recon;
	private XnatImagescandata scan;
	private XnatImageassessordata assess;
	private XnatImagesessiondata assessed;
	private XnatExperimentdata expt;
	private XnatSubjectdata subject;
	private XnatProjectdata project;
	private ExtSubjectpseudonym pseudonym;
	
	private String type;
	
	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public XnatReconstructedimagedata getRecon() {
		return recon;
	}

	@Override
	public void setRecon(XnatImagesessiondata assessed, XnatReconstructedimagedata recon, String type) {
		this.type = type;
		this.assessed=assessed;
		this.recon = recon;
	}

	@Override
	public XnatImagescandata getScan() {
		return scan;
	}

	@Override
	public void setScan(XnatImagesessiondata assessed, XnatImagescandata scan) {
		this.assessed=assessed;
		this.scan = scan;
	}

	@Override
	public XnatImageassessordata getAssess() {
		return assess;
	}

	@Override
	public void setAssess(XnatImagesessiondata assessed, XnatImageassessordata assess, String type) {
		this.type = type;
		this.assessed=assessed;
		this.assess = assess;
	}

	@Override
	public XnatExperimentdata getExpt() {
		return expt;
	}

	@Override
	public void setExpt(XnatProjectdata project,XnatExperimentdata expt) {
		this.project = project;
		this.expt = expt;
	}

	@Override
	public XnatSubjectdata getSubject() {
		return subject;
	}

	@Override
	public void setSubject(XnatProjectdata project,XnatSubjectdata subject) {
		this.project = project;
		this.subject = subject;
	}

	@Override
	public XnatProjectdata getProject() {
		return project;
	}

	@Override
	public void setProject(XnatProjectdata project) {
		this.project = project;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.DirectResourceBuilderI#buildResourceModifier()
	 */
	@Override
	public ResourceModifierA buildResourceModifier(final boolean overwrite, final XDATUser user,EventMetaI ci) throws Exception{        
		if(recon!=null){
			//reconstruction			
			if(assessed==null){
				throw new Exception("Invalid session id");
			}
			
			return new DirectReconResourceImpl(recon, assessed, type,overwrite,user,ci);
		}else if(scan!=null){
			//scan
			if(assessed==null){
				throw new Exception("Invalid session id");
			}
			
			return new DirectScanResourceImpl(scan, assessed,overwrite,user,ci);
		}else if(assess!=null){
			if(assessed==null){
				throw new Exception("Invalid session id");
			}
		
			return new DirectAssessResourceImpl((XnatImageassessordata)assess,(XnatImagesessiondata)assessed,type,overwrite,user,ci);
		}else if(pseudonym!=null){
			return new DirectPseudonymResourceImpl(subject, pseudonym,overwrite,user,ci);
		}else if(expt!=null){
			return new DirectExptResourceImpl(project, expt,overwrite,user,ci);
		}else if(subject!=null){
			return new DirectSubjResourceImpl(project, subject,overwrite,user,ci);
		}else if(project!=null){
			return new DirectProjResourceImpl(project,overwrite,user,ci);
		}else{
			throw new Exception("Unknown resource");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI#setPseudonymizedsubject()
	 */
	@Override
	public void setPseudonymizedsubject(XnatProjectdata project, ExtPseudonymizedsubjectdata subject) {
		this.project = project;
		this.subject = subject;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI#getPseudonymizedsubject()
	 */
	@Override
	public ExtPseudonymizedsubjectdata getPseudonymizedsubject() {
		return (this.subject instanceof ExtPseudonymizedsubjectdata) ? (ExtPseudonymizedsubjectdata)this.subject : null; // TODO ___ more elegant implementation ?
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI#setPseudonym()
	 */
	@Override
	public void setPseudonym(ExtSubjectpseudonym pseudonym) {
//		if (this.subject instanceof ExtPseudonymizedsubjectdata) TODO ___ checking subject instanceof ExtPseudonymizedsubjectdata
		this.pseudonym = pseudonym;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI#getPseudonym()
	 */
	@Override
	public ExtSubjectpseudonym getPseudonym() {
		return pseudonym;
	}
}
