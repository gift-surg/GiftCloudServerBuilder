/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.resources.prearchive;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilderI;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.helpers.prearchive.SessionDataTriple;
import org.nrg.xnat.helpers.prearchive.SessionException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */
public final class PrearcSessionListResource extends SecureResource {

	private static final String PROJECT_ATTR = "PROJECT_ID";
	private final Logger logger = LoggerFactory.getLogger(PrearcSessionListResource.class);

	private String requestedProject=null;

	private final Reference prearcRef;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public PrearcSessionListResource(final Context context, final Request request,
			final Response response) {
		super(context, request, response);

		// Project is explicit in the request
		requestedProject = (String)request.getAttributes().get(PROJECT_ATTR);

		prearcRef = request.getResourceRef();
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		
	}
	
	/**
	 * Refresh all the sessions
	 */
	@Override
	public boolean allowPut () {
		return true;
	}
	
	public void handlePut () {
		try {
			PrearcDatabase.refresh();
		} catch (SQLException e) {
			logger.error("Unable to refresh sessions", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
		} catch (SessionException e) {
			logger.error("Unable to refresh sessions", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("Unable to refresh sessions", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
		} catch (IOException e) {
			logger.error("Unable to refresh sessions", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
		}
	}
	
	/**
		 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		final MediaType mt = overrideVariant(variant);

		XFTTable table = null; 
		
		if(this.getQueryVariable("tag")!=null){
			final String tag=getQueryVariable("tag");
			try {
				if(!user.checkRole(PrearcUtils.ROLE_SITE_ADMIN)){
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Non admin user's can not query by tag");
					return null;
				}
				
				table=retrieveTable(tag);
			} catch (Exception e) {
				logger.error("", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return null;
			}
		}else{		
			ArrayList<String> projects = null;
			
			try {
				projects = PrearcUtils.getProjects(user, requestedProject);
			} catch (Exception e) {
				logger.error(" Unable to get list of projects", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return null;
			}
			
			String path=prearcRef.getBaseRef().toString();
		
			if(requestedProject!=null){
				path = StringUtils.join(new String[]{path,"/",requestedProject});
			}
		
			ArrayList<String> validProjects = new ArrayList<String>();
			ArrayList<String> invalidProjects =new ArrayList<String>();
	
			for(final String project:projects){
				try {
					if (PrearcUtils.validUser(user, project,false)) {
						validProjects.add(project);
			}
					else {
						invalidProjects.add(project);
				}
						} catch (Exception e) {
							logger.error("Unable to check project permissions : ", e);
							if(projects.size()==1){
								this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
						return null;
					}
				}
			}
	
			if (invalidProjects.size() > 0) {
				Iterator<String> i = invalidProjects.iterator();
				if (projects.size()==1) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, 
							                            "user " + user.getUsername() + " does not have create permissions " +
							                     		"for the following projects : " + StringUtils.join(i, ','));
				}
				return null;
			}
						
			try {
				table = this.retrieveTable(validProjects);
			}
			catch (SQLException e) {
				logger.error("Unable to query prearchive table : ", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return null;
			} catch (SessionException e) {
				logger.error("Unable to query prearchive table : ", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return null;
			} catch (Exception e) {
				logger.error("Unable to query prearchive table : ", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return null;
			}	
		}

			
		return this.representTable(table, mt, new Hashtable<String,Object>());
	}
	
	public XFTTable retrieveTable(ArrayList<String> projects) throws SQLException, SessionException {
		String [] _proj = new String[projects.size()];

		final XFTTable table=PrearcUtils.convertArrayLtoTable(PrearcDatabase.buildRows(projects.toArray(_proj)));
		
		return table;
	}
	
	public XFTTable retrieveTable(String tag) throws SQLException, SessionException {
		final Collection<SessionData> matches=PrearcDatabase.getSessionByUID(tag);

		final List<SessionDataTriple> ss=new ArrayList<SessionDataTriple>();
		
		for(final SessionData s:matches){
			ss.add(s.getSessionDataTriple());
		}
		
		final XFTTable table=PrearcUtils.convertArrayLtoTable(PrearcDatabase.buildRows(ss));
		
		return table;
	}
	
	public PrearcTableBuilderI getPrearcBuider(){
		return new PrearcTableBuilder();
	}
}
