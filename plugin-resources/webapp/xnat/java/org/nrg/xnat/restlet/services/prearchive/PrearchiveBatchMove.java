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
public class PrearchiveBatchMove extends BatchPrearchiveActionsA {
    private static final String NEW_PROJECT = "newProject";
	static org.apache.log4j.Logger logger = Logger.getLogger(Importer.class);

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public PrearchiveBatchMove(Context context, Request request, Response response) {
		super(context, request, response);
	}

	private String newProject=null;

	@Override
	public void handleParam(String key, Object o) throws ClientException {
				if(key.equals(SRC)){
			srcs.add((String)o);	
				}else if(key.equals(NEW_PROJECT)){
					newProject=this.getQueryVariable(NEW_PROJECT);
				}else if(key.equals(ASYNC)) {
					if (this.getQueryVariable(ASYNC).equals("false")) {
						async = false;
					}
				}
			}				

	@Override
	public void handlePost() {
		
		try {
			loadBodyVariables();
			//maintain parameters
			loadQueryVariables();			
		} catch (ClientException e) {
			this.getResponse().setStatus(e.getStatus(),e);
			return;
		}
		
		if(newProject==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"Move operation requires 'newProject'");
			return;
		}else{
			try {
				if (!PrearcUtils.canModify(user, newProject)) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Invalid permissions for new project.");
					return;
				}
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return;
			}
		}
		
		final List<SessionDataTriple> ss=new ArrayList<SessionDataTriple>();
		
		for(final String src:srcs){
			try {
				SessionDataTriple s=buildSessionDataTriple(src);
				if (!PrearcUtils.canModify(user, s.getProject())) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Invalid permissions for new project.");
					return;
				}
				ss.add(s);
			} catch (Exception e) {
				logger.error("",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return;
			}
		}
			
		try {
			if (async) {
				PrearcDatabase.moveToProject(ss, newProject);
			}
			else {
				Iterator<SessionDataTriple> i = ss.iterator();
				while (i.hasNext()) {
					SessionDataTriple s = i.next();
					PrearcDatabase.moveToProject(s.getFolderName(), s.getTimestamp(), s.getProject(), newProject);
				}
			}
		} catch (Exception e) {
			logger.error("",e);
			//ignore failure and return current status's
		}
			
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
