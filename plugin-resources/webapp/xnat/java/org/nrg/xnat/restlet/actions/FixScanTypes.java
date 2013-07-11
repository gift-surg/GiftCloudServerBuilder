/*
 * org.nrg.xnat.restlet.actions.FixScanTypes
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.restlet.actions;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.utils.SaveItemHelper;


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
		}
		

		if(allowSave){
			if(SaveItemHelper.authorizedSave(expt,user,false,false,c)){
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
