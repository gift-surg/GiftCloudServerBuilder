/**
 * 
 */
package org.nrg.xnat.restlet.services.prearchive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionDataTriple;
import org.nrg.xnat.restlet.services.Importer;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

/**
 * @author tolsen01
 *
 */
public class PrearchiveBatchDelete extends BatchPrearchiveActionsA {
    static org.apache.log4j.Logger logger = Logger.getLogger(Importer.class);
    
	public PrearchiveBatchDelete(Context context, Request request, Response response) {
		super(context, request, response);
				
	}

	@Override
	public void handlePost() {
		Representation entity = this.getRequest().getEntity();
		
		try {
			if (RequestUtil.isMultiPartFormData(entity)) {
				loadParams(new Form(entity));
			}

			//maintain parameters
			loadParams(getQueryVariableForm());
		} catch (ClientException e) {
			this.getResponse().setStatus(e.getStatus(),e);
			return;
		}
		
		List<SessionDataTriple> ss=new ArrayList<SessionDataTriple>();
		
		for(final String src:srcs){
			try {
				SessionDataTriple s=buildSessionDataTriple(src);
				if (!PrearcUtils.canModify(user, s.getProject())) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Invalid permissions for new project.");
					return;
				}
				ss.add(s);
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return;
			}
		}
			
			
		try {
			if (async) {
				PrearcDatabase.deleteSession(ss);
			}
			else {
				Iterator<SessionDataTriple> i = ss.iterator();
				while (i.hasNext()) {
					SessionDataTriple s = i.next();
					PrearcDatabase.deleteSession(s.getFolderName(), s.getTimestamp(), s.getProject());
				}
			}
			
		} catch (Exception e) {
			logger.error("",e);
			//ignore failure and return current status's
		}
			
//		} catch (Exception e) {
//			logger.error("",e);
//			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
//			return;
//		}
		
		final Response response = getResponse();
		try {
			response.setEntity(updatedStatusRepresentation(ss,overrideVariant(getPreferredVariant())));
		} catch (Exception e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
			return;
		}
	}
}
