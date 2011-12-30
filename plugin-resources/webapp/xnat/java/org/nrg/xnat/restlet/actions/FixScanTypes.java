/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xnat.utils.WorkflowUtils;


/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */
public class FixScanTypes {
	static Logger logger = Logger.getLogger(FixScanTypes.class);

	private final XnatExperimentdata expt;
	private final XDATUser user;
	private final XnatProjectdata proj;
	private final boolean allowSave;
	private final EventMetaI c;
	
	public FixScanTypes( final XnatExperimentdata expt, final XDATUser user, final XnatProjectdata proj, final Boolean allowSave, EventMetaI c){
		this.expt=expt;
		this.user=user;
		this.proj=proj;
		this.allowSave=allowSave;
		this.c=c;
	}
	
	public Boolean call() throws Exception{
		if(expt instanceof XnatImagesessiondata){
			((XnatImagesessiondata)expt).fixScanTypes();
			((XnatImagesessiondata)expt).defaultQuality("usable");
		}
		

		if(allowSave){
			if(expt.save(user,false,false,c)){
				MaterializedView.DeleteByUser(user);

				if(this.proj.getArcSpecification().getQuarantineCode()!=null && this.proj.getArcSpecification().getQuarantineCode().equals(1)){
					expt.quarantine(user);
				}
				
				return Boolean.TRUE;
			}else{
				return Boolean.FALSE;
			}
		}else{
			return Boolean.TRUE;
		}
	}
}
