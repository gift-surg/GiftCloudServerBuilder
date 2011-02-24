/**
 * Copyright 2010,2011 Washington University
 */
package org.nrg.xnat.restlet.resources.prearchive;

import java.io.File;
import java.io.SyncFailedException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.TurbineException;
import org.nrg.action.ClientException;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.archive.FinishImageUpload;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.nrg.xnat.helpers.prearchive.SessionDataTriple;
import org.nrg.xnat.helpers.prearchive.SessionException;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.restlet.representations.StandardTurbineScreen;
import org.nrg.xnat.restlet.representations.ZipRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class PrearcSessionResource extends SecureResource {
    private static final String PROJECT_ATTR = "PROJECT_ID";
    private static final String SESSION_TIMESTAMP = "SESSION_TIMESTAMP";
    private static final String SESSION_LABEL = "SESSION_LABEL";

    public static final String POST_ACTION_RESET = "reset-status";
    public static final String POST_ACTION_BUILD = "build";
    public static final String POST_ACTION_MOVE = "move";
    public static final String POST_ACTION_COMMIT = "commit";

    private final Logger logger = LoggerFactory.getLogger(PrearcSessionResource.class);

    private String project;
    private final String timestamp, session;

    /**
     * @param context
     * @param request
     * @param response
     */
    public PrearcSessionResource(Context context, Request request,
            Response response) {
        super(context, request, response);

        final Map<String,Object> attrs = getRequest().getAttributes();

        // Project, timestamp, session are explicit in the request
        project = (String)attrs.get(PROJECT_ATTR);
        timestamp = (String)attrs.get(SESSION_TIMESTAMP);
        session = (String)attrs.get(SESSION_LABEL);

        getVariants().add(new Variant(MediaType.TEXT_XML));
        getVariants().add(new Variant(MediaType.APPLICATION_ZIP));
        getVariants().add(new Variant(MediaType.APPLICATION_GNU_ZIP));
        getVariants().add(new Variant(MediaType.TEXT_HTML));
    }

    @Override
    public final boolean allowPost() { return true; }

    @Override
    public final boolean allowDelete() { return true; }

	final Map<String,Object> params=new Hashtable<String,Object>();

	String action=null;
	
	@Override
	public void handleParam(String key, Object value) throws ClientException {
		if(key.equals("action")){
			action=(String)value;
		}else{
			params.put(key,value);
		}
	}
	
    @Override
    public void handlePost(){
        if(project.equalsIgnoreCase("Unassigned"))project=null;
        
        try {
			loadParams(this.getBodyAsForm());
			
			loadParams(this.getQueryVariableForm());
		} catch (ClientException e1) {
			logger.error("",e1);
			this.getResponse().setStatus(e1.getStatus(), e1);
		}
        
        final File sessionDir;
        try {
            sessionDir = PrearcUtils.getPrearcSessionDir(user, project, timestamp, session,false);
        } catch (InvalidPermissionException e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            return;
        }

        if (POST_ACTION_BUILD.equals(action)) {
            try {
                if (PrearcDatabase.setStatus(session, timestamp, project, PrearcUtils.PrearcStatus.BUILDING)) {
                    PrearcDatabase.buildSession(sessionDir, session, timestamp, project);
                    PrearcUtils.resetStatus(user, project, timestamp, session,true);
                	returnString(wrapPartialDataURI(PrearcUtils.buildURI(project,timestamp,session)), MediaType.TEXT_URI_LIST,Status.SUCCESS_OK);
                } else {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "session document locked");
                }
            } catch (InvalidPermissionException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            } catch (Exception e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
            }
        } else if (POST_ACTION_RESET.equals(action)) {
            try {
                final String tag= getQueryVariable("tag");
                PrearcUtils.resetStatus(user, project, timestamp, session,tag,true);
            	returnString(wrapPartialDataURI(PrearcUtils.buildURI(project,timestamp,session)), MediaType.TEXT_URI_LIST,Status.SUCCESS_OK);
            } catch (InvalidPermissionException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            } catch (Exception e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
            }
        } else if (POST_ACTION_MOVE.equals(action)) {
            String newProj=this.getQueryVariable("newProject");

            if(StringUtils.isNotEmpty(newProj)){
                //TODO: convert ALIAS to project ID (if necessary)
            }

            try {
                if(!PrearcUtils.canModify(user, newProj)){
                    this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Unable to modify session data for destination project.");
                    return;
                }
                if(PrearcDatabase.setStatus(session, timestamp, project, PrearcStatus.MOVING)){
                    PrearcDatabase.moveToProject(session, timestamp, project, newProj);
                    PrearcUtils.buildURI(project,timestamp,session);
                	returnString(wrapPartialDataURI(PrearcUtils.buildURI(newProj,timestamp,session)), MediaType.TEXT_URI_LIST,Status.REDIRECTION_PERMANENT);
                }				
            } catch (SyncFailedException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
            } catch (SQLException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
            } catch (SessionException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e);
            } catch (Exception e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
            }			
        }else if (POST_ACTION_COMMIT.equals(action)) {
            try {
                if (PrearcDatabase.setStatus(session, timestamp, project, PrearcUtils.PrearcStatus.BUILDING)) {
                    PrearcDatabase.buildSession(sessionDir, session, timestamp, project);
                    PrearcUtils.resetStatus(user, project, timestamp, session,true);
        			
                    final FinishImageUpload uploader=new FinishImageUpload(null, user, new PrearcSession(project,timestamp,session,params,user), null, false, true, false);
                    
                    if(uploader.isAutoArchive()){
                        returnString(wrapPartialDataURI(uploader.call()),Status.REDIRECTION_PERMANENT);
                    }else{
                    	returnString(wrapPartialDataURI(uploader.call()), MediaType.TEXT_URI_LIST,Status.SUCCESS_OK);
                    }
                } else {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "session document locked");
                }
            } catch (InvalidPermissionException e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            } catch (Exception e) {
                logger.error("",e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
            }
        } else {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "unsupported action on prearchive session: " + action);
        }
    }

    @Override
    public void handleDelete() {

        if(project.equalsIgnoreCase("Unassigned"))project=null;

        try {
            //checks if the user can access this session
            PrearcUtils.getPrearcSessionDir(user, project, timestamp, session,false);
        } catch (InvalidPermissionException e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            return;
        }

        try {
            if(!PrearcUtils.canModify(user, project)){
                this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Unable to modify session data for destination project.");
                return;
            }

            PrearcDatabase.deleteSession(session, timestamp, project);
        } catch (SessionException e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("",e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            return;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
     */
    @SuppressWarnings("serial")
    @Override
    public Representation getRepresentation(final Variant variant){
        final File sessionDir;
        try {
            sessionDir = PrearcUtils.getPrearcSessionDir(user, project, timestamp, session,false);
        } catch (InvalidPermissionException e) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage());
            return null;
        } catch (Exception e) {
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            return null;
        }

        final MediaType mt = overrideVariant(variant);
        if (MediaType.TEXT_XML.equals(mt)) {
            // Return the session XML, if it exists
            final File sessionXML = new File(sessionDir.getPath() + ".xml");
            if (!sessionXML.isFile()) {
                this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,	"The named session exists, but its XNAT session document is not available. The session is likely invalid or incomplete.");
                return null;
            }
            return new FileRepresentation(sessionXML, variant.getMediaType(), 0);
        } else if (MediaType.APPLICATION_JSON.equals(mt)) {
            List<SessionDataTriple> l = new ArrayList<SessionDataTriple>();
            l.add(new SessionDataTriple().setFolderName(sessionDir.getName()).setProject(project).setTimestamp(timestamp));
            XFTTable table = null;
            try {
                table = PrearcUtils.convertArrayLtoTable(PrearcDatabase.buildRows(l));
            } catch (Exception e) {
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            } 
            return this.representTable(table, MediaType.APPLICATION_JSON, new Hashtable<String,Object>());
        } 
        else if (MediaType.TEXT_HTML.equals(mt)) {
            // Return the session XML, if it exists

            String screen=this.getQueryVariable("screen");
            if(StringUtils.isEmpty(screen)){
                screen="XDATScreen_brief_xnat_imageSessionData.vm";
            }else if (screen.equals("XDATScreen_uploaded_xnat_imageSessionData.vm")){
                if(project==null){
                    this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Projects in the unassigned folder cannot be archived.");
                    return null;
                }
                getResponse().redirectSeeOther(getContextPath()+String.format("/app/action/LoadImageData/project/%s/timestamp/%s/folder/%s/popup/false",project,timestamp,session));
                return null;
            }

            try {
                return new StandardTurbineScreen(mt, getRequest(), user, screen, new HashMap<String,String>(){{
                    put("project",project);
                    put("timestamp",timestamp);
                    put("folder",session);
                }});
            } catch (TurbineException e) {
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                return null;
            }

        } else if (MediaType.APPLICATION_GNU_ZIP.equals(mt) || MediaType.APPLICATION_ZIP.equals(mt)) {
            ZipRepresentation zip = new ZipRepresentation(mt, sessionDir.getName());
            zip.addFolder(sessionDir.getName(), sessionDir);
            return zip;
        } else {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"Requested type " + mt + " is not supported");
            return null;
        }
    }
}
