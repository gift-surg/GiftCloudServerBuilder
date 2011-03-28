/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ProjectPipelineListResource extends SecureResource  {
	XnatProjectdata proj = null;
	String pID = null;
	
	
	
	public ProjectPipelineListResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		this.getVariants().add(new Variant(MediaType.TEXT_XML));
		
		pID= (String)request.getAttributes().get("PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}
	}
	
	
	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public boolean allowDelete() {
		return true;
	}
	
	public void handleDelete() {
		//Remove the Pipeline identified by the path for the project and the datatype
		if (proj != null) {
			Form f = getRequest().getResourceRef().getQueryAsForm();
			String pathToPipeline = null;
			String datatype = null;
			if(f!=null) {
				pathToPipeline = f.getFirstValue("path");
				datatype = f.getFirstValue("datatype");
				if (pathToPipeline != null && datatype != null) {
					pathToPipeline = pathToPipeline.trim();
					datatype=datatype.trim();
					boolean isUserAuthorized = isUserAuthorized();
					if (isUserAuthorized) {
						try {
						ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(proj.getId());
						boolean success = PipelineRepositoryManager.GetInstance().delete(arcProject, pathToPipeline, datatype, user);
						if (!success) {
							getLogger().log(getLogger().getLevel(), "Couldnt delete the pipeline " + pathToPipeline + " for the project " + proj.getId());
							getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, " Couldnt succesfully save Project Specification" );
							return;
						}else {
							ArcSpecManager.Reset();
							getResponse().setEntity(getRepresentation(getVariants().get(0)));
					        Representation selectedRepresentation = getResponse().getEntity();
					        if (getRequest().getConditions().hasSome()) {
					            final Status status = getRequest().getConditions()
					                    .getStatus(getRequest().getMethod(),
					                            selectedRepresentation);

					            if (status != null) {
					                getResponse().setStatus(status);
					                getResponse().setEntity(null);
					            }
					        }
							//Send a 200 OK message back
							//getResponse().setStatus(Status.SUCCESS_OK,"Pipeline has been removed from project " + proj.getId());
						}
						}catch(Exception e) {
							e.printStackTrace();
							getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Encountered exception " + e.getMessage());
						}
					}else {
						getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User unauthroized to remove pipeline from project");
					}
				}
			}else {
				getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Expecting path and datatype as query parameters");
			}
		}else {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Project Resource identified by " + pID  + " not found");
		}
	}

	
	

	private boolean isUserAuthorized() {
		boolean isUserAuthorized = false;
		try {
			isUserAuthorized = user.canDelete(proj);
		}catch(Exception e) {
			e.printStackTrace();
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		return isUserAuthorized;
	}
	
	@Override
	public Representation getRepresentation(Variant variant) {
		//Document xmldoc = null;
		boolean isUserAuthorized = isUserAuthorized();
		ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(proj.getId());
		String comment = "existing";
		boolean additional = false;
		if (isUserAuthorized) {
			Form f = getRequest().getResourceRef().getQueryAsForm();
			if(f!=null) {
				String additionalStr=f.getFirstValue("additional");
				if (additionalStr != null)
					additional = Boolean.parseBoolean(additionalStr);
			}
				//Check to see if the Project already has an entry in the ArcSpec.
				//If yes, then return that entry. If not then construct a new ArcProject element and insert an attribute to say that its an already existing
				//entry or not
				try {
					if (arcProject == null) { // No Project pipelines set in the archive specification
						   if (additional) {
							arcProject = PipelineRepositoryManager.GetInstance().createNewArcProject(proj);
							comment = "new";
						   }else {
							   getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "No archive spec entry for project " + proj.getId());
						   }
						}else {
							if (additional) { //Return all the pipelines that are applicable to the project but not selected
								arcProject = PipelineRepositoryManager.GetInstance().getAdditionalPipelines(proj);
								comment = "additional";
							}else {
								//XFTItem hack = arcProject.getCurrentDBVersion(true);
								//arcProject.setItem(hack);
							}
						}
						//xmldoc = arcProject.toXML();
						//Comment commentNode = xmldoc.createComment(comment);
						//xmldoc.appendChild(commentNode);
					}catch(Exception e) {
						e.printStackTrace();
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
				MediaType mt = overrideVariant(variant);
				if (mt.equals(MediaType.TEXT_XML)) {
					return representItem(arcProject.getItem(), mt, null,false, true);
				}else if (mt.equals(MediaType.APPLICATION_JSON)) {
					XFTTable table = PipelineRepositoryManager.GetInstance().toTable(arcProject);
					
					return representTable(table, mt,null);
				}else {
					return null;
				}
		}else {
			getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
		}
		return null;
	}
}
