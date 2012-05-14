//Copyright 2012 Radiologics, Inc.  All Rights Reserved
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;

public class ModifyImageAssessorData extends ModifyItem {

	@Override
	public void postProcessing(XFTItem item, RunData data, Context context)
			throws Exception {
		super.postProcessing(item, data, context);
//		
//		XnatImageassessordata assessor= (XnatImageassessordata) BaseElement.GetGeneratedItem(item);
//
//		final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, TurbineUtils.getUser(data), assessor.getImageSessionData().getItem(),"Added image assessment");
//    	EventMetaI c=wrk.buildEvent();
//        PersistentWorkflowUtils.save(wrk,c);
	}
	
}
