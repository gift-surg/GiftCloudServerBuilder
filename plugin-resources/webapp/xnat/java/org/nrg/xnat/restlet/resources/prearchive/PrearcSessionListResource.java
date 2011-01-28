/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.resources.prearchive;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilderI;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI;
import org.nrg.xnat.helpers.prearchive.SessionData;
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
import org.xml.sax.SAXException;

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

		if (invalidProjects.size() > 0) {
			Iterator<String> i = invalidProjects.iterator();
			if (projects.size()==1) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, 
						                            "user " + user.getUsername() + " does not have create permissions " +
						                     		"for the following projects : " + StringUtils.join(i, ','));
			}
			return null;
		}
		
		XFTTable table = null; 
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
	
	public XFTTable retrieveTable(ArrayList<String> projects) throws SQLException, SessionException {
//		try {
//			PrearcDatabase.refresh();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			PrearcDatabase.setStatus("F063TE", "Test2", PrearcUtils.PrearcStatus.ERROR);
//		} catch (UniqueRowNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (EmptyArgumentException e){
//			
//		}
//		try {
//			PrearcDatabase.moveToProject("F063TE", "Test2", "Test4");
//		} catch (UniqueRowNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (EmptyArgumentException e) {
//			
//		}

		String [] _proj = new String[projects.size()];
		XFTTable table = new XFTTable();
		ArrayList<ArrayList<Object>> rows = PrearcDatabase.buildRows(projects.toArray(_proj));
		table.initTable(PrearcDatabase.getCols());
		Iterator<ArrayList<Object>> i = rows.iterator();
		while(i.hasNext()) {
			table.insertRow(i.next().toArray());
		}
		
//		PrearcDatabase.moveToProject("F063TE","20101129_133041","Test2","Test4");
//		List<SessionData> ss = PrearcDatabase.getProjects("prearchive/projects/Unassigned,Test2");
		return table;
	}
	
	public PrearcTableBuilderI getPrearcBuider(){
		return new PrearcTableBuilder();
	}
}
