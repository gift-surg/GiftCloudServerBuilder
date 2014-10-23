/*
 * org.nrg.xnat.restlet.resources.ProjectResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 2/18/14 1:32 PM
 */
package org.nrg.xnat.restlet.resources;

import com.google.common.collect.Lists;
import org.nrg.action.ActionException;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class ProjectResource extends ItemResource {
    private static final Logger logger = LoggerFactory.getLogger(ProjectResource.class);

    public XnatProjectdata proj = null;
    String pID = null;

    public ProjectResource(Context context, Request request, Response response) throws ResourceException {
        super(context, request, response);

        // This was part of a fix for XNAT-3453, but it breaks other non-standard REST ways of setting project properties.
        // if (!validateCleanUrl(request, response)) {
        //     throw new ResourceException(response.getStatus());
        // }

        pID = (String) getParameter(request, "PROJECT_ID");
        if (pID != null) {
            proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
        }

        if (proj != null) {
            getVariants().add(new Variant(MediaType.TEXT_HTML));
            getVariants().add(new Variant(MediaType.TEXT_XML));
        }

        fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.PROJECT_DATA, false));
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handleDelete() {
        if (user == null || user.isGuest()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
        } else {
            if (filepath != null && !filepath.equals("")) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                return;
            }

            if (proj != null) {
                try {
                    final boolean removeFiles = isQueryVariableTrue("removeFiles");
                    if (user.canDelete(proj)) {
                        final PersistentWorkflowI workflow = WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, XnatProjectdata.SCHEMA_ELEMENT_NAME, proj.getId(), proj.getId(), newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, EventUtils.getDeleteAction(proj.getXSIType())));
                        final EventMetaI ci = workflow.buildEvent();

                        try {
                            proj.delete(removeFiles, user, ci);
                            PersistentWorkflowUtils.complete(workflow, ci);

                            Date curr = new Date();
                            long timestamp = curr.getTime();
                            String query = "UPDATE xnat_projectdata_history SET id = '"+ proj.getId() + timestamp +"' where id = '"+ proj.getId() +"';";
                            PoolDBUtils.ExecuteNonSelectQuery(query, proj.getItem().getDBName(), user.getLogin());
                        } catch (Exception e) {
                            logger.error("", e);
                            PersistentWorkflowUtils.fail(workflow, ci);
                            throw e;
                        }
                    } else {
                        getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User account doesn't have permission to delete this project.");
                    }
                } catch (InvalidItemException e) {
                    logger.error("", e);
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                } catch (Exception e) {
                    logger.error("", e);
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                }
            }
        }
    }

    @Override
    public void handlePut() {
        if (user == null || user.isGuest()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
        } else {
            try {
                if (proj == null || user.canEdit(proj)) {
                    XFTItem item = loadItem("xnat:projectData", true);

                    if (item == null) {
                        String xsiType = getQueryVariable("xsiType");
                        if (xsiType != null) {
                            item = XFTItem.NewItem(xsiType, user);
                        }
                    }

                    if (item == null) {
                        if (proj != null) {
                            item = proj.getItem();
                        }
                    }

                    if (item == null) {
                        getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need PUT Contents");
                        return;
                    }

                    boolean allowDataDeletion = false;
                    if (getQueryVariable("allowDataDeletion") != null && getQueryVariable("allowDataDeletion").equalsIgnoreCase("true")) {
                        allowDataDeletion = true;
                    }

                    if (item.instanceOf("xnat:projectData")) {
                        XnatProjectdata project = new XnatProjectdata(item);

                        if (filepath != null && !filepath.equals("")) {
                            if (project.getId() == null) {
                                item = proj.getItem();
                                project = proj;
                            }

                            if (!user.canEdit(item)) {
                                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User account doesn't have permission to edit this project.");
                                return;
                            }
                            if (filepath.startsWith("quarantine_code/")) {
                                String qc = filepath.substring(16);
                                if (!qc.equals("")) {
                                    ArcProject ap = project.getArcSpecification();
                                    try {
                                        Integer qcI = Integer.valueOf(qc);
                                        ap.setQuarantineCode(qcI);
                                    } catch (NumberFormatException e) {
                                        if (qc.equals("true")) {
                                            ap.setQuarantineCode(new Integer(1));
                                        } else if (qc.equals("false")) {
                                            ap.setQuarantineCode(new Integer(0));
                                        } else {
                                            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Prearchive code must be an integer.");
                                            return;
                                        }
                                    }

                                    create(project, ap, false, false, newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, "Configured quarantine code"));
                                    ArcSpecManager.Reset();
                                }
                            } else if (filepath.startsWith("prearchive_code/")) {
                                String qc = filepath.substring(16);
                                if (!qc.equals("")) {
                                    ArcProject ap = project.getArcSpecification();
                                    try {
                                        Integer qcI = Integer.valueOf(qc);
                                        ap.setPrearchiveCode(qcI);
                                    } catch (NumberFormatException e) {
                                        if (qc.equals("true")) {
                                            ap.setPrearchiveCode(new Integer(1));
                                        } else if (qc.equals("false")) {
                                            ap.setPrearchiveCode(new Integer(0));
                                        } else {
                                            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Prearchive code must be an integer.");
                                            return;
                                        }
                                    }
                                    create(project, ap, false, false, newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, "Configured prearchive code"));
                                    ArcSpecManager.Reset();
                                }
                            } else if (filepath.startsWith("current_arc/")) {
                                String qc = filepath.substring(12);
                                if (!qc.equals("")) {
                                    ArcProject ap = project.getArcSpecification();
                                    ap.setCurrentArc(qc);

                                    create(project, ap, false, false, newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, "Configured current arc"));
                                    ArcSpecManager.Reset();
                                }
                            } else if (filepath.startsWith("scan_type_mapping/")) {
                                String stm = filepath.substring(18);
                                if (stm.equals("false")) {
                                    project.setUseScanTypeMapping(false);
                                }
                                else if (stm.equals("true")) {
                                    project.setUseScanTypeMapping(true);
                                }
                            } else {
                                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                            }
                        } else {
                            if (StringUtils.IsEmpty(project.getId())) {
                                project.setId(pID);
                            }

                            if (StringUtils.IsEmpty(project.getId())) {
                                getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Requires XNAT ProjectData ID");
                                return;
                            }

                            if (!StringUtils.IsAlphaNumericUnderscore(project.getId()) && !isQueryVariableTrue("testHyphen")) {
                                getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Invalid character in project ID.");
                                return;
                            }

                            if (item.getCurrentDBVersion() != null) {
                                if (!user.canEdit(item)) {
                                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User account doesn't have permission to edit this project.");
                                    return;
                                }
                            } else {
                                Long count = (Long) PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(ID) FROM xnat_projectdata_history WHERE ID='" + project.getId() + "';", "COUNT", null, null);
                                if (count > 0) {
                                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Project '" + project.getId() + "' was used in a previously deleted project and cannot be reused.");
                                    return;
                                }
                            }

                            if (XFT.getBooleanProperty("UI.allow-non-admin-project-creation", true) || user.isSiteAdmin()) {
                                project.preSave();
                                BaseXnatProjectdata.createProject(project, user, allowDataDeletion, true, newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN), getQueryVariable("accessibility"));
                            } else {
                                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User account doesn't have permission to edit this project.");
                            }
                        }
                    }
                } else {
                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User account doesn't have permission to edit this project.");
                }
            } catch (ActionException e) {
    			getResponse().setStatus(e.getStatus(),e.getMessage());
    		} catch (InvalidPermissionException e) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            } catch (IllegalArgumentException e) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            } catch (Exception e) {
                logger.error("Unknown exception type", e);
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }
        }
    }

    @Override
    public Representation represent(Variant variant) {

        if (proj != null) {
        	FilteredResourceHandlerI handler=null;
            try {
    			for(FilteredResourceHandlerI filter:getHandlers("org.nrg.xnat.restlet.projectResource.extensions",_defaultHandlers)){
    				if(filter.canHandle(this)){
    					handler=filter;
    				}
    			}
    		} catch (InstantiationException e1) {
    			logger.error("",e1);
    		} catch (IllegalAccessException e1) {
    			logger.error("",e1);
    		}
    		
    		try {
    			if(handler!=null){
    				return handler.handle(this,variant);
    			}else{
    				return null;
    			}
    		} catch (Exception e) {
    			logger.error("",e);
    			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    			return null;
    		}
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified experiment.");
            return null;
        }
    }
    

	public final static List<FilteredResourceHandlerI> _defaultHandlers=Lists.newArrayList();
	static{
		_defaultHandlers.add(new DefaultProjectHandler());
	}
    
    public static class DefaultProjectHandler implements FilteredResourceHandlerI{

		@Override
		public boolean canHandle(SecureResource resource) {
			return true;
		}

		@Override
		public Representation handle(SecureResource resource, Variant variant) throws Exception {
	        MediaType mt = resource.overrideVariant(variant);
	        ProjectResource projResource=(ProjectResource)resource;
            if (resource.filepath != null && !resource.filepath.equals("")) {
                if (resource.filepath.equals("quarantine_code")) {
                    try {
                        return new StringRepresentation(projResource.proj.getArcSpecification().getQuarantineCode().toString(), mt);
                    } catch (Throwable e) {
                        logger.error("", e);
                        projResource.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                        return null;
                    }
                } else if (resource.filepath.startsWith("prearchive_code")) {
                    try {
                        return new StringRepresentation(projResource.proj.getArcSpecification().getPrearchiveCode().toString(), mt);
                    } catch (Throwable e) {
                        logger.error("", e);
                        projResource.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                        return null;
                    }
                } else if (resource.filepath.startsWith("current_arc")) {
                    try {
                        return new StringRepresentation(projResource.proj.getArcSpecification().getCurrentArc(), mt);
                    } catch (Throwable e) {
                        logger.error("", e);
                        resource.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                        return null;
                    }
                } else {
                	resource.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                    return null;
                }
            } else {
                return projResource.representItem(projResource.proj.getItem(), mt);
            }
		}
    	
    }
}
