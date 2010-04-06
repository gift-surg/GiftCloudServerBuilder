//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Jul 20, 2005
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.*;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xdat.om.XnatSubjectdata;

/**
 * @author Tim
 *
 */
public class VerifySubjectForExperiment extends SecureReport {

    /* (non-Javadoc)
     * @see org.cnl.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context) {
        XnatSubjectdata subject = new XnatSubjectdata(item);
        context.put("subject",subject);
        if (TurbineUtils.HasPassedParameter("destination", data)){
            context.put("destination", TurbineUtils.GetPassedParameter("destination", data));
        }else{
            context.put("destination","XDATScreen_edit_xnat_mrSessionData.vm");
        }
        
        if (TurbineUtils.HasPassedParameter("tag", data)){
            context.put("tag", TurbineUtils.GetPassedParameter("tag", data));
        }
    }

}