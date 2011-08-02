package org.nrg.xnat.helpers.editscript;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.dcm.ScriptTableException;
import org.nrg.dcm.xnat.EditTable;
import org.nrg.dcm.xnat.EditTableDAO;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.dcm.xnat.ScriptTableDAO;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DicomEdit extends SecureResource {
	private final Logger logger = LoggerFactory.getLogger(DicomEdit.class);
	
	/**
	 * URI template variables
	 */
	private static final String PROJECT_ID = "PROJECT_ID";
	private static final String RESOURCE = "RESOURCE";
	
	/**
	 * Query string parameters
	 */
	private static final String ACTIVATE = "activate";
	
	/**
	 * Columns for creating a representation of the config scripts
	 */
	private final String[] scriptColumns = {"project","user","create_date","script", "id"};
	private final String[] editColumns = {"project","edit","create_date","user", "id"};
	
	private final String projectPassedIn;
	private final XnatProjectdata project;
	
	/**
	 * Hibernate entities
	 */
	private final ScriptTableDAO st;
	private final EditTableDAO et;
	
	/**
	 * Datatypes
	 */
	private final ResourceScope scope;     
	private final ResourceType rType;
	private final Access access;
	private final boolean expectingUpload;
	
	/**
	 * SCRIPT - A script is being uploaded or requested
	 * STATUS - The status of a script is being set or requested
	 * UNKNOWN - Don't know what is being requested
	 * @author aditya
	 *
	 */
	private enum ResourceType {
		SCRIPT, 
		STATUS, 
		UNKNOWN;
	};
	
	/**
	 * SITE_WIDE - The scope of script or script status is site-wide
	 * PROJECT - The scope of the script or status is project-specific
	 * @author aditya
	 *
	 */
	private enum ResourceScope {
		SITE_WIDE,
		PROJECT
	}
	
	/**
	 * ALL - Everyone has access to this resource
	 * PROJECT - Only project owners have access to this resource
	 * @author aditya
	 *
	 */
	private enum Access {
		ALL,
		PROJECT
	}
	
	/**
	 * Determine if something is being uploaded. The only
	 * time something is being uploaded is when a script is PUT.
	 * @param r
	 * @param m
	 * @return
	 */
	boolean determineUpload (ResourceType r, Method m) {
		if (m == Method.GET){ return false; } 
		else if (m == Method.PUT && r == ResourceType.SCRIPT) { return true;}
		else { return false;}
	}
	
	/**
	 * Determine what level of access this resource has.
	 * Everyone has access to GET the site-wide script and site-wide status,
	 * all other access requires the user to have the appropriate privileges.
	 * @param r
	 * @param s
	 * @param m
	 * @return
	 */
	Access determineAccess (ResourceType r, ResourceScope s, Method m) {
		if      (m == Method.GET && r == ResourceType.SCRIPT && s == ResourceScope.SITE_WIDE) { return Access.ALL;}
		else if (m == Method.GET && r == ResourceType.STATUS && s == ResourceScope.SITE_WIDE) { return Access.ALL;}
		else {return Access.PROJECT;}
	}
	
	/**
	 * Determine the scope of this resource. 
	 * URI's containing the segment "projects" signify that scope is project-specific, 
	 * site-wide otherwise.
	 * @param r
	 * @return
	 */
	ResourceScope determineResourceScope (Request r) {
		if (r.getOriginalRef().getSegments().contains("projects")) {
			return ResourceScope.PROJECT;
		}
		else {
			return ResourceScope.SITE_WIDE;
		}
	}
	
	/**
	 * Parse the URI to determine whether a script or status is being requested. 
	 * @param r
	 * @return
	 */
	ResourceType determineResourceType (Request r) {
		String resourceType = (String) r.getAttributes().get(DicomEdit.RESOURCE);
		if (resourceType.equals("script")) {
			return ResourceType.SCRIPT;
		}
		else if (resourceType.equals("status")) {
			return ResourceType.STATUS;
		}
		else {
			return ResourceType.UNKNOWN;
		}
	}
	
	public DicomEdit(Context context, Request request, Response response) {
		super(context, request, response);
		this.projectPassedIn = (String) request.getAttributes().get(DicomEdit.PROJECT_ID);
		this.project = XnatProjectdata.getXnatProjectdatasById(this.projectPassedIn, null, false);
		
		ContextService _c = ContextService.getInstance();
		this.st = _c.getBean(ScriptTableDAO.class);
		this.et = _c.getBean(EditTableDAO.class);

		this.scope =  this.determineResourceScope(request);
		this.rType =  this.determineResourceType(request);
		this.access = this.determineAccess(this.rType, this.scope, request.getMethod());
		this.expectingUpload = this.determineUpload(this.rType, request.getMethod());
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
	}
	
	/**
	 * Get this project's unique identifier in the database.
	 * "projectdata_info" is used by XNAT to keep track of every project
	 * ever created and so is robust to deleted projects. 
	 * @param p
	 * @return
	 */
	public static Long getDBId(XnatProjectdata p) {
		return new Long((Integer)p.getItem().getProps().get("projectdata_info"));
	}
	
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		final MediaType mt = overrideVariant(variant);
		final boolean all = this.getQueryVariable("all") == null ? false : true;
		XFTTable table = null;
		try {
			table = 
				new ScriptOp<XFTTable>(this.project,
									   this.getResponse(),
									   this.scope,
									   this.rType,	
									   this.access,
						               this.user,
						               new Callable<XFTTable>(){
					@Override
					public XFTTable call() throws Exception {
						XFTTable table = new XFTTable();
						Long project_id = project == null ? null : DicomEdit.getDBId(project);
						String project_name = project == null ? "null" : project.getId();
						if (rType == ResourceType.SCRIPT) { 
							List<ScriptTable> sts = new ArrayList<ScriptTable>();
							if (all) {
								sts.addAll(st.getByProject(project_id));
							}
							else {
								sts.add(st.get(project_id));
							}
							table.initTable(scriptColumns);
							for (ScriptTable s : sts) {
								if (s != null) {
									String[] scriptArray = {
											project_name,
											s.getXnatUser(), 
											s.getTimestamp().toString(),
											s.getScript(), 
											((Long)s.getId()).toString()
									};
									table.insertRow(scriptArray);
								}
							}
						}
						else if (rType == ResourceType.STATUS){
							List<EditTable> ets = new ArrayList<EditTable>();
							if (all) {
								ets.addAll(et.getByProject(project_id));
							}
							else {
								ets.add(et.get(project_id));
							}
							table.initTable(editColumns);
							for (EditTable e : ets) {
								if(e != null) {
									String [] editArray = {
											project_name,
											((Boolean)e.getEdit()).toString(),
											e.getTimestamp().toString(),
											e.getXnatUser(),
											((Long)e.getId()).toString()
									};
									table.insertRow(editArray);
								}
							}
						}
						else { // ResourceType.UNKNOWN
							throw new Exception ("Unknown resource type.");
						}
						return table;
					}
				}).run();
		}
		catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
		return this.representTable(table, mt, new Hashtable<String,Object>());
	}
	
	@Override
	public boolean allowGet(){
		return true;
	}
	
	@Override
	public boolean allowPost() {
		return false;
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}
	
	/**
	 * Build a closure that extracts the script from an uploaded file.
	 * @return
	 */
	Callable<String> getFile() {
		return new Callable<String>() {
			Request rq = DicomEdit.this.getRequest();
			Response rp = DicomEdit.this.getResponse();
			
			public String call () throws Exception {
				Representation entity = rq.getEntity();
				FileWriterWrapperI fw = null;
				List<FileWriterWrapperI> fws = DicomEdit.this.getFileWritersAndLoadParams(entity);
				
				if (fws.size() == 0) {
					rp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
					return null;
				}
				
				if(fws.size()>1){
					rp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
					return null;
				}
				fw = fws.get(0);
				final InputStream is = fw.getInputStream();
				String script = DicomEdit.convertStreamToString(is);
				return script;
			}
		};
	}
	
	public void handlePut(){
		try {
			new ScriptOp<java.lang.Void>(this.project,
					                     this.getResponse(),
					                     this.scope,
										 this.rType,
										 this.access,
										 this.user,
										 new Callable<java.lang.Void>(){
				@Override
				public java.lang.Void call() throws Exception {
					try {
						if (rType == ResourceType.SCRIPT) {
							String script = getFile().call();
							if (script != null) {
								if (scope == ResourceScope.SITE_WIDE) {
									st.insertScript(null, script, user.getUsername());		
								}
								else { // project specific 
									st.insertScript(DicomEdit.getDBId(project), script, user.getUsername());
								}
							}
							else {
								// something went wrong, but the error response status should have 
								// been set in the closure so do nothing.
							}
						}
						else if (rType == ResourceType.STATUS){
							String qActivate = getQueryVariable(DicomEdit.ACTIVATE);
							if (qActivate != null) {
								if (qActivate.equals("true") || qActivate.equals("false")) {
									Boolean activate = Boolean.parseBoolean(qActivate);
									if (scope == ResourceScope.SITE_WIDE) {
										et.setEdit(null, user.getUsername(), activate);
									}
									else { // project -specific
										et.setEdit(DicomEdit.getDBId(project), user.getUsername(), activate);	
									}
								}
								else {
									getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "The activate parameter should be either true or false.");
								}
							}
							else {
								getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Please set the activate query string parameter to true or false.");
							}
						}
						else { // ResourceType.UNKNOWN
							throw new Exception("Unknown resource type.");
						}
					}
					catch (ScriptTableException e){
						throw new Exception(e);
					}
					return null;
				}
			}).run();	
		}
		catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
	
	private static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line + "\n");
	    }
	    is.close();
	    return sb.toString();
	}	
	
	/**
	 * This class wraps all script and status request and modification operations. 
	 * @author aditya
	 *
	 * @param <A>
	 */
	public static class ScriptOp<A> {
		
		final Callable<A> c; // the operation to perform
		
		/**
		 * Values passed in from the outer class
		 */
		final Response resp;
		final ResourceScope scope;
		final XDATUser user;
		final XnatProjectdata d;
		final ResourceType rType;
		final Access a;
		final Logger logger = LoggerFactory.getLogger(DicomEdit.ScriptOp.class);
		
		ScriptOp(XnatProjectdata d, 
				 Response resp, 
				 ResourceScope scope, 
				 ResourceType rType, 
				 Access a,
				 XDATUser user, 
				 Callable<A> c) {
			this.a = a;
			this.d = d;
			this.user = user;
			this.rType = rType;
			this.c = c;
			this.resp = resp;
			this.scope= scope;
		}
		
		/**
		 * Perform some sanity checks and then run the operation 
		 * @return
		 * @throws Exception
		 */
		A run() throws Exception {
			if (this.rType != ResourceType.UNKNOWN) {
				if (this.scope == ResourceScope.PROJECT && this.d == null) {
					resp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "This project does not exist.");
					return null;
				}
				else {
					String project_id = this.d == null ? null : this.d.getId();
					if (hasAccessTo(project_id, user)) {
						return c.call();
					}
					else {
						resp.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
						return null;
					}
				}
			}
			else {
				resp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Resource type must be either script or status.");
				return null;
			}
		}
		
		boolean hasAccessTo(String project, XDATUser user) throws Exception {
			return a == Access.ALL || PrearcUtils.getProjects(user,null).contains(project);
		}
	}
}
