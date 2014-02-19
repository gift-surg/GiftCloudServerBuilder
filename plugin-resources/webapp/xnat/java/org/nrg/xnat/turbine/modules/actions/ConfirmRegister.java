/*
 * org.nrg.xnat.turbine.modules.actions.ConfirmRegister
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.search.ItemSearch;

public class ConfirmRegister extends SecureAction {

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        try {
            PopulateItem populater = PopulateItem.Populate(data,org.nrg.xft.XFT.PREFIX + ":user",true);
            ItemI found = populater.getItem();
            ItemSearch search = new ItemSearch();
            search.setAllowMultiples(false);
            search.setElement("xdat:user");
            search.addCriteria("xdat:user.login",found.getProperty("login"));

            ItemI temp = search.exec().getFirst();
            
    		String nextPage = (String)TurbineUtils.GetPassedParameter("nextPage",data);
    		String nextAction = (String)TurbineUtils.GetPassedParameter("nextAction",data);
    		String par = (String)TurbineUtils.GetPassedParameter("par",data);

            if(!StringUtils.isEmpty(par)){
            	context.put("par", par);
            }
		    if (!StringUtils.isEmpty(nextAction) && nextAction.indexOf("XDATLoginUser")==-1 && !nextAction.equals(org.apache.turbine.Turbine.getConfiguration().getString("action.login"))){
            	context.put("nextAction", nextAction);
		    }else if (!StringUtils.isEmpty(nextPage) && !nextPage.equals(org.apache.turbine.Turbine.getConfiguration().getString("template.home")) ) {
            	context.put("nextPage", nextPage);
			}
            

	        SecureScreen.loadAdditionalVariables(data, context);
            
            if (temp==null)
            {
            	if(!found.getStringProperty("email").equals(AdminUtils.getAdminEmailId()))
            	{
	                search = new ItemSearch();
	                search.setAllowMultiples(false);
	                search.setElement("xdat:user");
	                search.addCriteria("xdat:user.email",found.getProperty("email"));
	
	                temp = search.exec().getFirst();
            	}
            	
                if (temp==null)
                {
                    context.put("newUser", new XdatUser(found));
                    data.setScreenTemplate("ConfirmRegistration.vm");
                }else{
                    // OLD USER
                    data.setMessage("Email (" + found.getProperty("email") + ") already exists.");
                    data.setScreenTemplate("ForgotLogin.vm");
                }
            }else{
                // OLD USER
                data.setMessage("Username (" + found.getProperty("login") + ") already exists.");
                data.setScreenTemplate("ForgotLogin.vm");
            }
        } catch (Exception e) {
        }
    }

    protected boolean isAuthorized( RunData data )  throws Exception
    {
        return true;
    }
}
