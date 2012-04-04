/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.db.MaterializedView;
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
	
	public FixScanTypes( final XnatExperimentdata expt, final XDATUser user, final XnatProjectdata proj, final Boolean allowSave){
		this.expt=expt;
		this.user=user;
		this.proj=proj;
		this.allowSave=allowSave;
	}
	
	public Boolean call() throws Exception{
		if(expt instanceof XnatImagesessiondata){
			((XnatImagesessiondata)expt).fixScanTypes();
		}

		if(allowSave){
			if(SaveItemHelper.authorizedSave(expt,user,false,false)){
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
