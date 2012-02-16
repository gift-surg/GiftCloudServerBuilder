//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Dec 11, 2006
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.modules.ActionLoader;
import org.apache.turbine.modules.actions.VelocityAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class XDATRegisterUser extends org.nrg.xdat.turbine.modules.actions.XDATRegisterUser {
    static Logger logger = Logger.getLogger(XDATRegisterUser.class);

    public void directRequest(RunData data,Context context,XDATUser user) throws Exception{
		String nextPage = data.getParameters().getString("nextPage","");
		String nextAction = data.getParameters().getString("nextAction","");

        data.setScreenTemplate("Index.vm");
        
         if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("par",data))!=null){
         	AcceptProjectAccess action = new AcceptProjectAccess();
         	context.put("user", user);
         	action.doPerform(data, context);
         }else if (!StringUtils.isEmpty(nextAction) && nextAction.indexOf("XDATLoginUser")==-1 && !nextAction.equals(org.apache.turbine.Turbine.getConfiguration().getString("action.login"))){
        	 if (XFT.GetUserRegistration()){
            	 data.setAction(nextAction);
                 VelocityAction action = (VelocityAction) ActionLoader.getInstance().getInstance(nextAction);
                 action.doPerform(data, context);
        	 }
		 }else if (!StringUtils.isEmpty(nextPage) && !nextPage.equals(org.apache.turbine.Turbine.getConfiguration().getString("template.home")) ) {
			 if (XFT.GetUserRegistration()){
            	 data.setScreenTemplate(nextPage);
			 }
		 }
         
    }

	@Override
    public String getAutoApprovalTextMsg(RunData data, XDATUser newUser){
    	String msg="New User Created: " + newUser.getUsername();
        msg +="<br>Firstname: " + newUser.getFirstname();
        msg +="<br>Lastname: " + newUser.getLastname();
        msg +="<br>Email: " + newUser.getEmail();
        if (TurbineUtils.HasPassedParameter("comments", data))
            msg +="<br>Comments: " + TurbineUtils.GetPassedParameter("comments", data);
        
        String phone = "";
        if (TurbineUtils.HasPassedParameter("phone", data))
            msg +="<br>Phone: " + TurbineUtils.GetPassedParameter("phone", data);
        
        String lab = "";
        if (TurbineUtils.HasPassedParameter("lab", data))
            msg +="<br>Lab: " + TurbineUtils.GetPassedParameter("lab", data);
        
        
        String parID = data.getParameters().getString("par","");
		
		if(!StringUtils.isEmpty(parID)){ 
			ProjectAccessRequest par =ProjectAccessRequest.RequestPARById(Integer.valueOf(parID), null);
			if(par!=null){
				msg +="<br>Project: " + par.getProjectID();
			}
		}
		
        return msg;
    }

	@Override
	public boolean autoApproval(RunData data, Context context) throws Exception {
		boolean autoApproval=super.autoApproval(data, context);

		if(autoApproval){
			return true;
		}
		
		String parID = data.getParameters().getString("par","");
		
		if(!StringUtils.isEmpty(parID)){ 
			ProjectAccessRequest par =ProjectAccessRequest.RequestPARById(Integer.valueOf(parID), null);
			if(par==null || par.getApproved()!=null || par.getApprovalDate()!=null){
				return false;
			}else{
				return true;
			}
		}
		
		return autoApproval;
	}

    
}
