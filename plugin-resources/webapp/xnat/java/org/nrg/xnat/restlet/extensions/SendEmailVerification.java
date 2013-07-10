package org.nrg.xnat.restlet.extensions;
import java.util.Date;

import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.*;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.mail.services.EmailRequestLogService;
import org.apache.commons.lang.StringEscapeUtils;

@XnatRestlet(value = {"/services/sendEmailVerification"}, secure = false)
public class SendEmailVerification extends Resource {

    private final EmailRequestLogService requests = XDAT.getContextService().getBean(EmailRequestLogService.class);

    public SendEmailVerification(Context context, Request request, Response response) throws Exception{
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
    }
    
    @Override public boolean allowDelete() { return false; }
    @Override public boolean allowPut()    { return false; }
    @Override public boolean allowGet()    { return false; }
    @Override public boolean allowPost()   { return true;  }
    
    @Override public void handlePost(){
       String email = SecureResource.getQueryVariable("email", getRequest());
       if((email != null) && (!email.equals(""))){ 
          try{
             if (requests.isEmailBlocked(email)){ 
                throw new Exception("Exceeded maximum number of email requests."); 
             }
             AdminUtils.sendNewUserVerificationEmail(getXDATUser(StringEscapeUtils.escapeSql(email)));
             requests.logEmailRequest(email, new Date ());
          } catch(Exception e) { 
            this.getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, "Unable to send Verification Email.");
          }
       }
    }

    private ItemI getXDATUser(String email) throws Exception{
       ItemSearch search = new ItemSearch();
       search.setAllowMultiples(false);
       search.setElement("xdat:user");
       search.addCriteria("xdat:user.email",email);
       return search.exec().getFirst();
    }
}