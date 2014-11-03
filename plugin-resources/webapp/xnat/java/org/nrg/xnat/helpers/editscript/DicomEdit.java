/*
 * org.nrg.xnat.helpers.editscript.DicomEdit
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 12/12/13 5:27 PM
 */
package org.nrg.xnat.helpers.editscript;

import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

public final class DicomEdit extends SecureResource {
	
	/**
     * This declares the tool name to be used when storing anonymization data in the configuration service.
	 */
	public static final String ToolName = "anon";
	
	/**
	 * SITE_WIDE - The scope of script or script status is site-wide
	 * PROJECT - The scope of the script or status is project-specific
	 * @author aditya
	 *
	 */
	public enum ResourceScope {
		SITE_WIDE,
		PROJECT
	}
	
	/**
	 * Return a path unique to the given scope (either project-specific or site-wide) and project.
	 * 
	 * The reason I pass in an Object instead of a better type is as follows:
	 * Originally I needed to have the two overloaded methods, that differ on the type of the project variable, 
	 * with one responding to an XnatProjectdata object and the other a String holding the project_id. 
	 * 
	 * Unfortunately I also need to pass in null for the project for the site-wide case but I can't
	 * do that if there are two overloaded methods because the compiler gets confused. 
	 * 
	 * So I do the weird looking thing below which uses reflection to determine if the project is 
	 * XnatProjectdata or String. This breaks all readability but allows me to pass in a null project. 
	 * 
     * @param scope   The request scope.
	 * @param project Must be String, XnatProjectdata or null
     * @return The path for the script storage.
	 */
	public static String buildScriptPath(ResourceScope scope, Object project) {
		String project_id = null;
		if (project != null) {
			if (project.getClass() == XnatProjectdata.class) {
				project_id = ((XnatProjectdata)project).getId();
			}
			else if (project.getClass() == String.class) {
				project_id = (String)project;
			}
		}
		switch (scope) {
            case PROJECT:
                return "/projects/" + project_id;
            case SITE_WIDE:
                return "script";
            default:
                return "";
		}
	}
	
	public DicomEdit(Context context, Request request, Response response) {
		super(context, request, response);

        String projectId = (String) request.getAttributes().get(DicomEdit.PROJECT_ID);
        this.project = XnatProjectdata.getXnatProjectdatasById(projectId, null, false);

		this.scope =  this.determineResourceScope(request);
		this.rType =  this.determineResourceType(request);
		this.access = this.determineAccess(this.rType, this.scope, request.getMethod());
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
	}
	
	/**
	 * Get this project's unique identifier in the database.
	 * "projectdata_info" is used by XNAT to keep track of every project
	 * ever created and so is robust to deleted projects. 
     * @param p    The project to get the DB ID
     * @return The DB ID for the submitted project.
	 */
	public static Long getDBId(XnatProjectdata p) {
        return (long) (Integer) p.getItem().getProps().get("projectdata_info");
	}
	
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		final MediaType mt = overrideVariant(variant);
        final boolean all = this.getQueryVariable("all") != null;
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
						if (rType == ResourceType.SCRIPT) { 
							List<Configuration> cs = new ArrayList<Configuration>();
							if (all) {
								cs.addAll(AnonUtils.getService().getAllScripts(project_id));
							}
							else {
								cs.add(AnonUtils.getService().getScript(DicomEdit.buildScriptPath(scope, project), project_id));
							}
							table.initTable(scriptColumns);
							for (Configuration c : cs) {
								if (c != null) {
									String[] scriptArray = {
											c.getProject() == null ? "-1" : c.getProject().toString(),
											c.getXnatUser(),
											c.getCreated().toString(),
											c.getContents(),
											((Long)c.getId()).toString()
									};
									table.insertRow(scriptArray);
								}
							}
						}
						else if (rType == ResourceType.STATUS){
							List<Configuration> cs = new ArrayList<Configuration>();
							if (all) {
								cs.addAll(AnonUtils.getService().getAllScripts(project_id));
							}
							else {
								cs.add(AnonUtils.getService().getScript(DicomEdit.buildScriptPath(scope, project), project_id));
							}
							table.initTable(editColumns);
							for (Configuration c : cs) {
								if(c != null) {
									String [] editArray = {
											c.getProject() == null ? "-1" : c.getProject().toString(),
											((Boolean) c.getStatus().equals(Configuration.ENABLED_STRING)).toString(),
											c.getCreated().toString(),
											c.getXnatUser(),
											((Long)c.getId()).toString()
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
        catch (Exception exception) {
            logger.error("Internal server error for user " + user.getUsername(), exception);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, exception.getMessage());
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
	
    @Override
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
                                    String script = getFile();
							if (script != null) {
								if (scope == ResourceScope.SITE_WIDE) {
									AnonUtils.getService().setSiteWideScript(user.getLogin(), 
																	  DicomEdit.buildScriptPath(scope, project), 
																	  script); 
								}
								else { // project specific 
									AnonUtils.getService().setProjectScript(user.getLogin(), 
																	  		 DicomEdit.buildScriptPath(scope, project), 
																	  		 script, 
																	  		 DicomEdit.getDBId(project));
								}
							}
							else {
								// something went wrong, but the error response status should have 
								// been set in the closure so do nothing.
                                        if (logger.isWarnEnabled()) {
                                            logger.warn("An error occurred, check error response status or logging from anon service.");
                                        }
							}
						}
						else if (rType == ResourceType.STATUS){
							String qActivate = getQueryVariable(DicomEdit.ACTIVATE);
							if (qActivate != null) {
								if (qActivate.equals("true") || qActivate.equals("false")) {
									Boolean activate = Boolean.parseBoolean(qActivate);
									if (scope == ResourceScope.SITE_WIDE) {
										if (activate) {
											AnonUtils.getService().enableSiteWide(user.getLogin(),
																		   	       DicomEdit.buildScriptPath(scope, project));
										} 
										else {
											AnonUtils.getService().disableSiteWide(user.getLogin(), 
										             			  					DicomEdit.buildScriptPath(scope, project));
										}
									}
									else { // project -specific
										if (activate) {
											AnonUtils.getService().enableProjectSpecific(user.getLogin(), 
																		   				  DicomEdit.buildScriptPath(scope, project), 
																		   				  DicomEdit.getDBId(project));
										}
										else {
											AnonUtils.getService().disableProjectSpecific(user.getLogin(), 
																				 		   DicomEdit.buildScriptPath(scope, project), 
																				 		   DicomEdit.getDBId(project));
										}
									}
								}
								else {
                                            logger.warn("The activate parameter should be either true or false for user " + user.getUsername());
									getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "The activate parameter should be either true or false.");
								}
							}
							else {
                                        logger.warn("The activate query string parameter should be either true or false for user " + user.getUsername());
								getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Please set the activate query string parameter to true or false.");
							}
						}
						else { // ResourceType.UNKNOWN
							throw new Exception("Unknown resource type.");
						}
					}
					catch (ConfigServiceException e){
						throw new Exception(e);
					}
					return null;
				}
			}).run();	
        } catch (Exception exception) {
            logger.error("Internal server error for user " + user.getUsername(), exception);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, exception.getMessage());
		}
		}

    /**
     * Determine what level of access this resource has.
     * Everyone has access to GET the site-wide script and site-wide status,
     * all other access requires the user to have the appropriate privileges.
     * @param type      The resource type (script or status)
     * @param scope     The resource scope (site-wide or project-specific)
     * @param method    The HTTP method
     * @return Indicates the script access level.
     */
    private Access determineAccess(ResourceType type, ResourceScope scope, Method method) {
        if (method == Method.GET && type == ResourceType.SCRIPT && scope == ResourceScope.SITE_WIDE) {
            return Access.ALL;
        } else if (method == Method.GET && type == ResourceType.STATUS && scope == ResourceScope.SITE_WIDE) {
            return Access.ALL;
        } else {
            return Access.PROJECT;
        }
    }

    /**
     * Determine the scope of this resource.
     * URI's containing the segment "projects" signify that scope is project-specific,
     * site-wide otherwise.
     * @param request    The request object.
     * @return Indicates the resource scope for the request.
     */
    private ResourceScope determineResourceScope (Request request) {
        if (request.getOriginalRef().getSegments().contains("projects")) {
            return ResourceScope.PROJECT;
        }
        else {
            return ResourceScope.SITE_WIDE;
        }
    }

    /**
     * Parse the URI to determine whether a script or status is being requested.
     * @param request    The request object.
     * @return Checks whether a script or status is being requested.
     */
    private ResourceType determineResourceType (Request request) {
        String resourceType = (String) request.getAttributes().get(DicomEdit.RESOURCE);
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

    /**
     * Build a closure that extracts the script from an uploaded file.
     * @return The script for the indicated file.
     */
    private String getFile() throws Exception {
        Request rq = DicomEdit.this.getRequest();
        Response rp = DicomEdit.this.getResponse();
        Representation entity = rq.getEntity();
        if(entity == null || entity.getSize() == 0) {
            return "";
        }
        List<FileWriterWrapperI> fws = DicomEdit.this.getFileWriters();

        if (fws.isEmpty()) {
            logger.warn("Unable to unpack script from request {}", rq);
            rp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
            return null;
        }

        if (fws.size() > 1) {
            rp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
            return null;
        }

        return DicomEdit.convertStreamToString(fws.get(0).getInputStream());
	}
	
	private static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
        String line;
	    while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
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
         * @return An object of the type for the script operation.
		 * @throws Exception
		 */
		A run() throws Exception {
			if (this.rType != ResourceType.UNKNOWN) {
				if (this.scope == ResourceScope.PROJECT && this.d == null) {
					resp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "This project does not exist.");
					return null;
				}
				else {
					String projectId = this.d == null ? null : this.d.getId();
					if (a == Access.ALL || user.hasAccessTo(projectId)) {
						return c.call();
					}
					else {
                        logger.warn("User {} does not have privileges to access this project", user.getUsername());
						resp.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
						return null;
					}
				}
			}
			else {
                logger.warn("Resource type must be either script or status.");
				resp.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Resource type must be either script or status.");
				return null;
			}
		}
	}

    /**
     * Declare the class logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DicomEdit.class);

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
    private static final String[] scriptColumns = {"project","user","create_date","script", "id"};
    private static final String[] editColumns = {"project","edit","create_date","user", "id"};

    /**
     * Project for this operation.
     */
    private final XnatProjectdata project;

    /**
     * Data types
     */
    private final ResourceScope scope;
    private final ResourceType rType;
    private final Access access;

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
        UNKNOWN
}

    /**
     * ALL - Everyone has access to this resource
     * PROJECT - Only project owners have access to this resource
     * @author aditya
     */
    private enum Access {
        ALL,
        PROJECT
    }
}
