//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jul 16, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;

public class ModifyProject extends SecureAction {
    static Logger logger = Logger.getLogger(ModifyItem.class);
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {

        try {
            PopulateItem populater = PopulateItem.Populate(data,"xnat:projectData",true);
            XDATUser user = TurbineUtils.getUser(data);
            
            InvalidValueException error=null;
            if (populater.hasError())
            {
                error = populater.getError();
            }

            XFTItem item = populater.getItem();
            XnatProjectdata  project = new XnatProjectdata(item);
            
            if (error!=null)
            {
                data.addMessage(error.getMessage());
                TurbineUtils.SetEditItem(item, data);
                data.setScreenTemplate("XDATScreen_edit_projectData.vm");
                return;
            }

            
            try {
				project.initNewProject(user,false,true);
			} catch (Exception e2) {
				TurbineUtils.SetEditItem(item,data);
                data.addMessage(e2.getMessage());
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return;
			}
            
            
            item.save(user, false, false);
            
            XnatProjectdata postSave = new XnatProjectdata(item);
            postSave.getItem().setUser(user);

            postSave.initGroups();
            
            user.initGroups();
            user.clearLocalCache();
            //postSave.initBundles(user);
            
            String accessibility=data.getParameters().getString("accessibility");
            if (accessibility==null){
                accessibility="protected";
            }
            
            if (!accessibility.equals("private"))
                project.initAccessibility(accessibility, true);
            
           // p.initBundles((XDATUser)user);
            
            if (TurbineUtils.HasPassedParameter("destination", data)){
                this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data,"AddStep2.vm"), postSave, data);
            }else{
                this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm",(ItemI) postSave, data);
            }
                       
            user.clearLocalCache();
            ElementSecurity.refresh();
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        } catch (FieldNotFoundException e) {
            logger.error("",e);
        }
    }

}
