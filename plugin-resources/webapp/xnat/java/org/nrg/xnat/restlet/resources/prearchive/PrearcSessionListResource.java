/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.resources.prearchive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilderI;
import org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI;
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
	}/*
		 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		final MediaType mt = overrideVariant(variant);
	
		//I would much rather create an interface that the Representation implementations use so that I wouldn't 
		//have to keep using this ugly XFTTable thing.  But, that is beyond the scope here, and this will work.

		List<String> projects=null;

		if(requestedProject!=null){
			if(requestedProject.contains(",")){
				projects=Arrays.asList(StringUtils.split(requestedProject,','));
			} else {
				projects=Arrays.asList(new String[]{requestedProject});
			}
		}else{
			projects=new ArrayList<String>();
			for (final List<String> row : user.getQueryResults("xnat:projectData/ID", "xnat:projectData")) {
				final String id = row.get(0);
				if (projects.contains(id))
					continue;
			try {
					if (user.canAction("xnat:mrSessionData/project", id, SecurityManager.CREATE)) {
						projects.add(id);
					}
		} catch (Exception e) {
					logger.error("Exception caught testing prearchive access", e);
				}
		}
		}
		
		String path=prearcRef.getBaseRef().toString();
	
		if(requestedProject!=null){
			path = StringUtils.join(new String[]{path,"/",requestedProject});
		}
	
		final XFTTable table = new XFTTable();
		table.initTable(this.getPrearcBuider().getColumns());

		for(final String project:projects){
			try {
				XFTTable t=retrieveTable(project,path);

				if(t!=null){
					table.rows().addAll(t.rows());
		}
			} catch (InvalidPermissionException e) {
				logger.error("Unable to build prearchive session list representation for " + project, e);
				if(projects.size()==1){
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,e.getMessage());
				return null;
			}
			} catch (Exception e) {
				logger.error("Unable to build prearchive session list representation for " + project, e);
				if(projects.size()==1){
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
			return null;
		}
	}
}

		if(this.getQueryVariable("sortBy")!=null){
			final String sortBy=this.getQueryVariable("sortBy");
			table.sort(Arrays.asList(StringUtils.split(sortBy, ',')));
			if(this.isQueryVariable("sortOrder","DESC",false)){
				table.reverse();
					}
				}
		
		return this.representTable(table, mt, new Hashtable<String,Object>());
			}
	
	public XFTTable retrieveTable(final String project, final String path) throws IOException, InvalidPermissionException, Exception{
		ProjectPrearchiveI t=getPrearcBuider().buildTable(project, user, path);
		return (t!=null)?t.getContent():null;
		}
	
	public PrearcTableBuilderI getPrearcBuider(){
		return new PrearcTableBuilder();
	}
}
