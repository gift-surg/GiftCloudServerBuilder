//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 27, 2005
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatMrassessordata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author Tim
 *
 */
public class Viewer extends SecureReport {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context)
    {
        if (om instanceof XnatMrsessiondata)
        {
            
        }else if (om instanceof XnatSubjectdata)
        {
            om = ((XnatSubjectdata)om).getLastSession();
            item = om.getItem();
            
            context.put("om",om);
            context.put("item",item);
        }else if (om instanceof XnatMrassessordata)
        {
            om = ((XnatMrassessordata)om).getMrSessionData();
            item = om.getItem();
            
            context.put("om",om);
            context.put("item",item);
        }
        context.put("appletPath",TurbineUtils.GetRelativeServerPath(data) + "/applet");
    }

    public void noItemError(RunData data, Context context)
    {
        if (context.containsKey("skipq"))
        {
            try {
		 	    XnatMrsessiondata mr = XNATUtils.getLastSessionForParticipant((String)context.get("id"),TurbineUtils.getUser(data));
		
		 	    if (mr ==null)
		 	    {
		 	       try {
		                this.doRedirect(data,"ClosePage.vm");
		            } catch (Exception e) {
		            }
		 	    }else{
			 	    context.put("item",mr.getItem());
					context.put("om",mr);
			        context.put("appletPath",TurbineUtils.GetRelativeServerPath(data) + "/applet");
		 	    }
		 	} catch (Exception e){
				e.printStackTrace();
			}
        }else{
            data.setMessage("No results were found for your search.");
            try {
                this.doRedirect(data,"ClosePage.vm");
            } catch (Exception e) {
            }
        }
    }
}
