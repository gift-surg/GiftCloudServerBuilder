package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**********************************************************************************************************************************************************************
 *  
 *  
 *  ConfigResource - a restlet for nrg_config to get and put text files 
 * 
 *  Full Specs:  https://xnatdev.wikispaces.com/nrg_config
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
 *		/config/{tool-name}/{PATH_TO_FILE}?status=enabled || status=true    PUT   enable a configuration
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

    private static final Logger _log = LoggerFactory.getLogger(ConfigResource.class);

	private final String[] configColumns = {"tool","path","project","user","create_date","reason","contents", "version", "status"};
	private final String[] listColumns = {"tool"};
	
	private final String projectName;
	private final String toolName;
	private final String reason;
	private  String path;

	//TODO: if we start using projectdata_info instead of id in config service:
	//private final long projectid;
	
	private final ConfigService configService;

	public ConfigResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		
		configService = XDAT.getConfigService();

		//handle url here
		projectName = (String) getRequest().getAttributes().get(PROJECT_ID);
		toolName = (String) getRequest().getAttributes().get(TOOL_NAME);
		reason = (String) getRequest().getAttributes().get(REASON);
		path = getFullConfigPath();
	}

	@Override
	public Representation represent(Variant variant) {
		
		try {
			final MediaType mt = overrideVariant(variant);
			
			//handle query variables
			final boolean getHistory = "getHistory".equalsIgnoreCase(this.getQueryVariable("action"));
			Integer version = null;
			final boolean meta = "true".equalsIgnoreCase(this.getQueryVariable("meta"));
			final boolean contents = "true".equalsIgnoreCase(this.getQueryVariable("contents"));
			
			try{
				version = Integer.parseInt(this.getQueryVariable("version"));
			} catch (Exception ignored){}
			
			XFTTable table = new XFTTable();
			
			Long projectId = null;
			//check access, almost copy-paste code in the PUT method.
			if(projectName != null){
				final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectName, user, false);
				if(!user.canRead(("xnat:subjectData/project").intern(), p.getId())){
                    _log.warn("User {} can not access project {}", new Object[] {user.getUsername(), projectName});
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
					return null;
				}
				projectId = (long) (Integer) p.getItem().getProps().get("projectdata_info");
			}
			
			List<Configuration> configurations  = new ArrayList<Configuration>();
			List<String> list = new ArrayList<String>();
			
			if(toolName == null && path == null && projectName == null) {
				//  /REST/config
				List<String> tools = configService.getTools();
				if(tools != null){
					list.addAll(tools);  //addAll is not null safe!
				}
			} else if(toolName == null && path == null) {
				//  /REST/projects/{PROJECT_ID}/config
				List<String> tools = configService.getTools(projectId);
	                
				if(tools != null){
					list.addAll(tools);  //addAll is not null safe!
				}
			} else if (path == null) {
				//  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}  or    /REST/config/{TOOL_NAME} 
				List<Configuration> l = configService.getConfigsByTool(toolName, projectId);
				if(l != null){
					configurations.addAll(l);  //addAll is not null safe.
				}
			} else {
				fixAnonPath();
				
				if(getHistory){ 
					//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&action=getHistory  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&action=getHistory
					List<Configuration> l = configService.getHistory(toolName, path, projectId);
					if(l != null){
						configurations.addAll(l);  //addAll is not null safe.
					}
				} else {
					if(version == null) {
						//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}
						configurations.add(configService.getConfig(toolName, path, projectId));
					} else {
						//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&version={version}  or  /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&version={version}
						configurations.add(configService.getConfigByVersion(toolName, path, version, projectId));
					}
					// we now react to the meta and contents parameters. if we're here, there is zero or 1 configuration added to the array.
					// if contents=true, just send the contents as a string.
					// if meta=true, zero out contents and just send the configuration meta data.
					// if meta=true && contents==true, send teh configuration as-is.
					// if meta=false && contents==false, this is the same as not specifying either in the querystring. So, just act as if they didn't.
					if(contents && !meta){
						Configuration c = configurations.get(0);
						if(c == null){
                            _log.warn("Config not found for user {} and project {} on tool [{}] path [{}]", new Object[]{user.getUsername(), projectName, toolName, path});
							this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
							return null;
						} else {
							return new StringRepresentation(c.getContents());
						}
					} else if (meta && !contents){
						Configuration c = configurations.get(0);
						if(c != null){
							c.setConfigData(null);
						}
					}
				}
			}
			
			//This is a little weird. Above this line, we populate one of 2 lists (either tools (strings) or configurations)
			//Below this line we render the one list that got created. if no list got created, we render a 404 
			
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
				
			} else if (configurations.size() > 0 && configurations.get(0) != null) {
			    //we generated a list of configurations, so represent those.
				table.initTable(configColumns);  //"tool","path","project","user","create_date","reason","contents", "version", "status"};
				for(Configuration c : configurations){
					if(c != null){
						
						//TODO: Since ConfigService is using projectdata_info Long instead of the Project Name String, then we may have to convert 
						//the long id back to a project name string. Luckily, here we already have the project name (passed in)
						//If you ever have to do that, it would look something like this:
						//	String projectName;
						//	List<XnatProjectdata> projects = XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/projectdata_info", new Long(c.getProject()), this.user,false);
						//	if(projects.size() < 1){
						//		projectName = "DELETED";
						//	} else {
						//		XnatProjectdata match = projects.get(0);
						//		projectName = match.getId();
						//	}

						String[] scriptArray = {
								c.getTool(),
								c.getPath(),
								projectName, 
								c.getXnatUser(), 
								c.getCreated().toString(),
								c.getReason(),
								c.getContents(), 
								Integer.toString(c.getVersion()),
								c.getStatus()
						};
						table.insertRow(scriptArray);
					}
				}
				return this.representTable(table, mt, new Hashtable<String,Object>());
			} else {
				//if we fell through to here, nothing existed at the supplied URI
                _log.warn("Couldn't find config for user {} and project {} on tool [{}] path [{}]", new Object[] {user.getUsername(), projectName, toolName, path});
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return null;
			}		
		} catch (Exception e) {
            _log.error("Couldn't find config for user {} and project {} on tool [{}] path [{}]", new Object[] {user.getUsername(), projectName, toolName, path});
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
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
			Long projectId = null;
			//check access, almost copy-paste code in the GET method.
			if(projectName != null){
				final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectName, user, false);
				if(!user.canRead(("xnat:subjectData/project").intern(), p.getId())){
                    _log.warn("User {} can not access project {}", new Object[] {user.getUsername(), projectName});
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project");
					return;
				}
				projectId = (long) (Integer) p.getItem().getProps().get("projectdata_info");
			}
			
			fixAnonPath();
			
			//if this is a status update, do it and return
			if(this.getQueryVariable("status") != null ) {
				//   /REST/config/{TOOL_NAME}/{PATH_TO_FILE}&status={enabled, disabled}    or      /REST/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}&status={enabled, disabled} 
				final boolean enable = "enabled".equals(this.getQueryVariable("status")) || "true".equals(this.getQueryVariable("status")); 
				if(enable){
					configService.enable(user.getUsername(), reason, toolName, path, projectId);
				} else {
					configService.disable(user.getUsername(), reason, toolName, path, projectId);
				}
				getResponse().setStatus(Status.SUCCESS_OK);
				return;
			}

			//if we got to here, we're adding a new configuration. do it:
			Representation entity;
			FileWriterWrapperI fw;
			
			entity = this.getRequest().getEntity();
			List<FileWriterWrapperI> fws = this.getFileWritersAndLoadParams(entity);
			if (fws.size() == 0) {
                _log.error("Unknown upload format", new Object[] {user.getUsername(), projectName});
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
				return;
			}

			if(fws.size()>1){
                _log.error("Importer is limited to one uploaded resource at a time.", new Object[] {user.getUsername(), projectName});
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
				return;
			}
			fw = fws.get(0);

			//read the input stream into a string buffer.
			final InputStream is = fw.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();
		    String line;
		    while ((line = reader.readLine()) != null) {
		      sb.append(line).append("\n");
		    }
		    is.close();
		    String contents = sb.toString();
			
		    //if there is a previous configuration check to see if its contents equals the new contents, if so, just return success.
			//do not update the configuration for puts are idempotent
		    Configuration prevConfig = configService.getConfig(toolName, path, projectId);
			if(prevConfig != null && contents.equals(prevConfig.getContents())) {	
				getResponse().setStatus(Status.SUCCESS_OK);
			} else {
				//save/update the configuration
				configService.replaceConfig(user.getUsername(), reason, toolName, path, contents, projectId);
				getResponse().setStatus(Status.SUCCESS_CREATED);
			}
		} catch (ConfigServiceException e) {
            _log.error("Error replacing config for user {} and project {} on tool [{}] path [{}]", new Object[] {user.getUsername(), projectName, toolName, path});
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		} catch (Exception e) {
            _log.error("Error replacing config for user {} and project {} on tool [{}] path [{}]", new Object[] {user.getUsername(), projectName, toolName, path});
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
	
	private void fixAnonPath(){
		//This is a bit of a hack, but doing the proper fix would introduce risk in the anonymization feature.  Which would be better done in a feature release, then a bug fix release.
		//The anon feature pre-dated the config service, but was migrated to use the config service for storage of the anonymization script.
		//However, it *appears* that the 'path' being set when the anonymization file is added (DicomEdit.buildScriptPath) is incorrect.  It is has a / at the beginning of the path, whereas other scripts in the config service don't.
		//So the ConfigResource correctly creates the path without the / at the beginning, but that fails to match the entry stored in the service by DicomEdit.  DicomEdit should be fixed, but that would introduce alot of headaches. 
		//So, for now, we'll just hack ConfigResource to support the erronous path in this one use case.
		if(toolName!=null && StringUtils.equals("anon", toolName) && projectName!=null && StringUtils.equals("projects/"+projectName, path)){
			path="/projects/"+projectName;
		}
    }
	
	//This method parses the URI and returns the "path" used for Configurations.
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
}