package org.nrg.xnat.restlet.resources;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**********************************************************************************************************************************************************************
 * 
 *  ConfigResource - a restlet for nrg_config to get and put text files 
 * 
 *		GET       /config								format=json/xml/html      Return a list of all tools that have a configuration
 *		GET       /config/{TOOL_NAME} 					format=json/xml/html      Return all configurations for this tool (all paths)
 *		GET/PUT   /config/{TOOL_NAME}/{PATH_TO_FILE}  	GET version= || action=getHistory || format=json/xml/html; || contents=true || meta=true  PUT status=disabled/enabled     
 *		GET       /projects/{PROJECT_ID}/config      	format=json/xml/html      Return a list of all tools that have a configuration associated with this project.
 *		GET       /projects/{PROJECT_ID}/config/{TOOL_NAME}						format=json/xml/html    Return all configurations associated with this project for this tool (all paths)
 *		GET/PUT   /projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}		GET version= || action=getHistory || format=json/xml/html; || contents=true || meta=true  PUT status=disabled/enabled
 *     
 *        
 *	Examples:
 *
 *		/config/{tool-name}/{PATH_TO_FILE}?status=disabled   PUT   disable a configuration
 *		/config/{tool-name}/{PATH_TO_FILE}?status=enabled    PUT   enable a configuration
 *		/config/{tool-name}/{PATH_TO_FILE}?version=1         GET   get a version of a config
 *		/config/{tool-name}/{PATH_TO_FILE}?action=getHistory GET   get a list of all the configurations
 *				
 *		/conf/anon/this/is/one/file                          PUT   put a configuration for the anon tool at that path
 *		/conf/anon/this/is/a/different/file                  PUT   put a configuration for the anon tool at that path
 *
 * (non-Javadoc)
 * @see org.restlet.resource.Resource#getRepresentation(org.restlet.resource.Variant)
 * 
 **********************************************************************************************************************************************************************
 */

public class ConfigResource extends SecureResource {

	private static final String PROJECT_ID = "PROJECT_ID";
	private static final String TOOL_NAME = "TOOL_NAME";
	private static final String PATH_TO_FILE = "PATH_TO_FILE";
	private static final String REASON = "REASON";
	private final String[] configColumns = {"tool","path","project","user","create_date","reason","contents", "version", "status"};
	private final String[] listColumns = {"tool"};
	
	private final String project;
	private final String toolName;
	private final String reason;
	private final String path;

	//TODO: if we start using projectdata_info instead of id in configservice:
	//private final long projectid;
	
	private final ConfigService configService;

	public ConfigResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		
		configService = XDAT.getConfigService();

		//handle url here
		project = (String) getRequest().getAttributes().get(PROJECT_ID);
		toolName = (String) getRequest().getAttributes().get(TOOL_NAME);
		reason = (String) getRequest().getAttributes().get(REASON);
		path = getFullConfigPath();
		//TODO: if we start using projectdata_info instead of id in configservice:
		//XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(this.project, null, false);
        //projectid = Long.valueOf(((Integer)p.getItem().getProps().get("projectdata_info")));
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		
		try {
			final MediaType mt = overrideVariant(variant);
			
			//handle query variables
			final boolean getHistory = "getHistory".equalsIgnoreCase(this.getQueryVariable("action")) ? true : false;
			final String version = this.getQueryVariable("version");
			final boolean meta =  "true".equalsIgnoreCase(this.getQueryVariable("meta")) ? true : false;
			final boolean contents =  "true".equalsIgnoreCase(this.getQueryVariable("contents")) ? true : false;
			
			XFTTable table = new XFTTable();

			//check access
			if(project != null && !hasAccessTo(project)){
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
				return null;
			}
			
			List<Configuration> confs  = new ArrayList<Configuration>();
			List<String> list = new ArrayList<String>();
			
			if(toolName == null && path == null && project == null) {
				//  /REST/config
				list.addAll(configService.getTools());
			} else if(toolName == null && path == null) {
				//  /REST/projects/{PROJECT_ID}/config
				list.addAll(configService.getTools(project));
			} else if (path == null) {
				//  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}  or    /REST/config/{TOOL_NAME} 
				confs.addAll(configService.getConfigsByTool(toolName, project));
			} else {
				if(getHistory){ 
					//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&action=getHistory  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&action=getHistory
					confs.addAll(configService.getHistory(toolName, path, project));
				} else {
					if(version == null) {
						//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}
						confs.add(configService.getConfig(toolName, path, project));						
					} else {
						//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&version={version}  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&version={version}
						confs.add(configService.getConfigById(toolName, path, version, project));
					}
					
					// we now react to the meta and contents parameters. if we're here, there is only 1 configuration added to the array.
					// if contents=true, just send the contents as a string.
					// if meta=true, zero out contents and just send the configuration meta data.
					// if meta=true && contents==true, send teh configuration as-is.
					// if meta=false && contents==false, this is the same as not specifying either in the querystring. So, just act as if they didn't.
					if(contents && !meta){
						Configuration c = confs.get(0);
						if(c == null){
							return new StringRepresentation(null);
						} else {
							return new StringRepresentation(c.getContents());
						}
					} else if (meta && !contents){
						Configuration c = confs.get(0);
						if(c != null){
							c.setConfigData(null);
						}
					}
				}
			}
			
			
			if(list.size()>0){ //"tool"
				//if we generated a listing of tools, represent those.
				table.initTable(listColumns);
				for(String s : list){
					if(s != null){
						String[] scriptArray = { s };
						table.insertRow(scriptArray);
					}
				}
				return this.representTable(table, mt, new Hashtable<String,Object>());
				
			} else if (confs.size() > 0) {
			    //we generated a list of configurations, so represent those.
				table.initTable(configColumns);  //"tool","path","project","user","create_date","reason","contents", "version", "status"};
				for(Configuration c : confs){
					if(c != null){
						
						//TODO: if you are really passing the projectdata_info instead of the string for id to ConfigService, then you have to convert back to a project name string, here.
						// something like this:
//						String projectName;
//						List<XnatProjectdata> projects = XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/projectdata_info", new Long(c.getProject()), this.user,false);
//						if(projects.size() < 1){
//							projectName = "DELETED";
//						} else {
//							XnatProjectdata match = projects.get(0);
//							projectName = match.getId();
//						}

						String[] scriptArray = {
								c.getTool(),
								c.getPath(),
								c.getProject(), 
								c.getXnatUser(), 
								c.getCreated().toString(),
								c.getReason(),
								c.getContents(), 
								((Long)c.getId()).toString(),
								c.getStatus()
						};
						table.insertRow(scriptArray);
					}
				}
				return this.representTable(table, mt, new Hashtable<String,Object>());
			} else {
				return new StringRepresentation("we fell through the if... what should we do in this case?");
			}		
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			//TODO: is return null the right thing to do? probably not...
			return null;
		}
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}
	
	@Override
	public void handlePut() {
		/*
		 * PUT is idempotent: if the network is botched and the client is not sure whether his request made it through, 
		 * it can just send it a second (or 100th) time, and it is guaranteed by the HTTP spec that this has exactly the 
		 * same effect as sending once.
		 */
		try{
			//check access
			if(project != null && !hasAccessTo(project)) {
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
				return;
			}

			//if this is a status update, do it and return
			if(this.getQueryVariable("status") != null ) {
				//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&status={enabled, disabled}    or      /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&status={enabled, disabled} 
				final boolean enable = "enabled".equals(this.getQueryVariable("status")) ? true : false;
				if(enable){
					configService.enable(user.getUsername(), reason, toolName, path, project);
				} else {
					configService.disable(user.getUsername(), reason, toolName, path, project);
				}
				getResponse().setStatus(Status.SUCCESS_OK);
				return;
			}

			//if we got to here, we're adding a new configuration. do it:
			Representation entity = null;
			FileWriterWrapperI fw = null;
			
			entity = this.getRequest().getEntity();
			List<FileWriterWrapperI> fws = this.getFileWritersAndLoadParams(entity);
			if (fws.size() == 0) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
				return;
			}

			if(fws.size()>1){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
				return;
			}
			fw = fws.get(0);

			//read the input stream into a string buffer.
			final InputStream is = fw.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();
		    String line = null;
		    while ((line = reader.readLine()) != null) {
		      sb.append(line + "\n");
		    }
		    is.close();
		    String contents = sb.toString();
			
		    //if there is a previous configuration check to see if its contents equals the new contents, if so, just return success.
			//do not update the configuration for puts are idempotent
		    Configuration prevConfig = configService.getConfig(toolName, path, project);
			if(prevConfig != null && contents.equals(prevConfig.getContents())) {	
				getResponse().setStatus(Status.SUCCESS_OK);
			} else {
				//save/update the configuration
				configService.replaceConfig(user.getUsername(), reason, toolName, path, contents, project);
				getResponse().setStatus(Status.SUCCESS_CREATED);
			}
		}
		catch (ConfigServiceException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
		catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
		return;
	}
	
	private String getFullConfigPath() {

		String path = (String) getRequest().getAttributes().get(PATH_TO_FILE);

		//restlet matches the first part of the path and ignores the rest.
		//if path is not null, we need to see if there's anything at the end of the URL to add.
		if(path != null){
			path = path + getRequest().getResourceRef().getRemainingPart();
			
			//lop off any query string parameters.
			int index = path.indexOf('?');
			if(index > 0){
				path = StringUtils.left(path, index);
			}
		}
		return path;
	}
	
	//this is DEECH's from his DicomEdit code, I would use it directly, but I don't want to be dependent on anonymize.
	boolean hasAccessTo(String project) throws Exception {
		ArrayList<String> projects = PrearcUtils.getProjects(this.user, project);
		return projects.contains(project);
	}
}
