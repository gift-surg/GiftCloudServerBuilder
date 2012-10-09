package org.nrg.xnat.turbine.modules.actions;

import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;

import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.modules.ActionLoader;
import org.apache.turbine.modules.actions.VelocityAction;
import org.apache.turbine.modules.actions.VelocitySecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xdat.security.PasswordValidatorChain;
import org.nrg.xdat.security.RegExpValidator;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.search.ItemSearch;
import org.springframework.security.authentication.AuthenticationManager;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.nrg.xdat.security.Authenticator;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xdat.turbine.utils.AccessLogger;

public class XDATChangePassword extends VelocitySecureAction {
	
	/** CGI Parameter for the user name */
	public static final String CGI_USERNAME = "xdat:user.login";

	/** CGI Parameter for the password */
	public static final String CGI_PASSWORD = "xdat:user.primary_password";
	
	/** CGI Parameter for the alias */
	public static final String CGI_ALIAS = "xdat:user.a";

	/** CGI Parameter for the secret */
	public static final String CGI_SECRET = "xdat:user.s";
	
  static Logger logger = Logger.getLogger(XDATChangePassword.class);

  @Override
  public void doPerform(RunData data, Context context) throws Exception {
	  
  	String username = TurbineUtils.getUser(data).getUsername();
	String password = (String)TurbineUtils.GetPassedParameter(CGI_PASSWORD, data);
	if((username!=null) &&!StringUtils.isEmpty(username)){
		if(!username.equals("guest")){
			if (StringUtils.isEmpty(username))
			{
				return;
			}else{
				if(username.contains("/")){
					username=username.substring(username.lastIndexOf("/")+1);
				}
				if(username.contains("\\")){
					username=username.substring(username.lastIndexOf("\\")+1);
				}
			}
		
			try
			{
				XDATUser oldUser = TurbineUtils.getUser(data);
				
                XFTItem toSave = XFTItem.NewItem("xdat:user", oldUser);
                toSave.setProperty("login", oldUser.getLogin());
                toSave.setProperty("primary_password", password);
                toSave.setProperty("email", oldUser.getProperty("email"));
				try {
					XDATUser.ModifyUser(oldUser, toSave, EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.WEB_FORM, "Modified user password"));
				}
				catch (Exception e) {
					invalidInformation(data, context, e.getMessage());
					logger.error("Error Storing User", e);
					return;
				}
				XdatUserAuth auth = XDAT.getXdatUserAuthService().getUserByNameAndAuth(oldUser.getUsername(), XdatUserAuthService.LOCALDB, "");
				auth.setPasswordUpdated(new java.util.Date());
				XDAT.getXdatUserAuthService().update(auth);
				data.getSession().setAttribute("expired",new Boolean(false));
				data.getSession().setAttribute("forcePasswordChange",false);
				data.setMessage("Password changed.");

			}
			catch (Exception e)
			{
	            log.error("",e);
	
	            AccessLogger.LogActionAccess(data, "Failed Login by '" + username +"': " +e.getMessage());
	            
	            if(username.toLowerCase().contains("script"))
	            {
	            	e= new Exception("Illegal username &lt;script&gt; usage.");
					AdminUtils.sendAdminEmail("Possible Cross-site scripting attempt blocked", StringEscapeUtils.escapeHtml(username));
	            	logger.error("",e);
	                data.setScreenTemplate("Error.vm");
	                data.getParameters().setString("exception", e.toString());
	                return;
	            }
	
					// Set Error Message and clean out the user.
	            if(e instanceof SQLException){
					data.setMessage("An error has occurred.  Please contact a site administrator for assistance.");
	            }else{
					data.setMessage(e.getMessage());
	            }
	            
				String loginTemplate =  org.apache.turbine.Turbine.getConfiguration().getString("template.login");
	
				if (StringUtils.isNotEmpty(loginTemplate))
				{
					// We're running in a templating solution
					data.setScreenTemplate(loginTemplate);
				}
				else
				{
					data.setScreen(org.apache.turbine.Turbine.getConfiguration().getString("screen.login"));
				}
			}
		}
		else{
			invalidInformation(data, context, "Guest account password must be managed in the administration section.");
		}
	}
	else{
		invalidInformation(data, context, "You must must be authenticated or have a token to change this password.");
	}
  }
  
  public void invalidInformation(RunData data,Context context, String message){
  	try {
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
			data.setMessage(message);
		} catch (Exception e) {
          logger.error(message,e);
			data.setMessage(message);
		}finally{
			data.setScreenTemplate("ChangePassword.vm");
		}
  }
  
	public void doRedirect(RunData data, Context context,XDATUser user) throws Exception{
		String nextPage = (String)TurbineUtils.GetPassedParameter("nextPage",data);
		String nextAction = (String)TurbineUtils.GetPassedParameter("nextAction",data);
		/*
		 * If the setPage("template.vm") method has not
		 * been used in the template to authenticate the
		 * user (usually Login.vm), then the user will
		 * be forwarded to the template that is specified
		 * by the "template.home" property as listed in
		 * TR.props for the webapp.
		 */
		 if (!StringUtils.isEmpty(nextAction) && nextAction.indexOf("XDATLoginUser")==-1 && !nextAction.equals(org.apache.turbine.Turbine.getConfiguration().getString("action.login"))){
			data.setAction(nextAction);
          VelocityAction action = (VelocityAction) ActionLoader.getInstance().getInstance(nextAction);
          action.doPerform(data, context);
		 }else if (!StringUtils.isEmpty(nextPage) && !nextPage.equals(org.apache.turbine.Turbine.getConfiguration().getString("template.home")) ) {
			data.setScreenTemplate(nextPage);
		 }

       if (data.getScreenTemplate().indexOf("Error.vm")!=-1)
       {
           data.setMessage("<b>Previous session expired.</b><br>If you have bookmarked this page, please redirect your bookmark to: " + TurbineUtils.GetFullServerPath());
           data.setScreenTemplate("Index.vm");
       }
	}

  @Override
  protected boolean isAuthorized(RunData data) throws Exception {
      return true;
  }
  
  public void directRequest(RunData data,Context context,XDATUser user) throws Exception{
		String nextPage = (String)TurbineUtils.GetPassedParameter("nextPage",data);
		String nextAction = (String)TurbineUtils.GetPassedParameter("nextAction",data);

      data.setScreenTemplate("Index.vm");
      
       if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("par",data))!=null){
       	AcceptProjectAccess action = new AcceptProjectAccess();
       	context.put("user", user);
       	action.doPerform(data, context);
       }else if (!StringUtils.isEmpty(nextAction) && nextAction.indexOf("XDATLoginUser")==-1 && !nextAction.equals(org.apache.turbine.Turbine.getConfiguration().getString("action.login"))){
      	 if (XFT.GetUserRegistration() && !XDAT.verificationOn()){
          	 data.setAction(nextAction);
               VelocityAction action = (VelocityAction) ActionLoader.getInstance().getInstance(nextAction);
               action.doPerform(data, context);
      	 }
		 }else if (!StringUtils.isEmpty(nextPage) && !nextPage.equals(org.apache.turbine.Turbine.getConfiguration().getString("template.home")) ) {
			 if (XFT.GetUserRegistration() && !XDAT.verificationOn()){
          	 data.setScreenTemplate(nextPage);
			 }
		 }
       
  }

}
