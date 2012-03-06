// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.turbine.modules.PageLoader;
import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.services.rundata.TurbineRunDataFacade;
import org.apache.turbine.services.template.TemplateService;
import org.apache.turbine.services.template.TurbineTemplate;
import org.apache.turbine.services.velocity.TurbineVelocity;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XdatUsergroup;
import org.nrg.xdat.om.XnatInvestigatordata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

public class RESTServlet extends HttpServlet {
	static org.apache.log4j.Logger logger = Logger.getLogger(RESTServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = XDAT.getUserDetails();
		if (user==null){
			response.sendError(401);
			return;
		}
		String url = parseContext(request);
		
		String format=request.getParameter("format");
		
		if (url.startsWith("users")){
			try {
		        response.setHeader("Cache-Control", "no-cache");
				String query = "SELECT xdat_user_id,login,firstname,lastname,email FROM xdat_user WHERE enabled=1 ORDER BY lastname;";
				XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());

	            String title = "ALL USERS";
	            this.returnTable(title, table, request, response);
			} catch (SQLException e) {
				logger.error(e);
			} catch (DBPoolException e) {
				logger.error(e);
			}
		}else if (url.startsWith("projects")){
			url = removeChunck(url);
			if (url==null){
				if (user.getGroup("ALL_DATA_ADMIN")==null){
					response.sendError(401);
					return;
				}
				
			    //return all projects
			    try {
					response.setHeader("Cache-Control", "no-cache");
					String query = "SELECT p.id,p.secondary_id,p.name FROM xnat_projectData p";
					XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
					
					String title = "ALL PROJECTS";
					this.returnTable(title, table, request, response);
				} catch (SQLException e) {
					logger.error(e);
				} catch (DBPoolException e) {
					logger.error(e);
				}
			}else{
				String pID=getChunck(url);
				url = removeChunck(url);
				XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
				if (proj==null){
					response.sendError(404);
					return;
				}else{
					if (url ==null){
						response.setHeader("Cache-Control", "no-cache");
			        	returnItem(proj,request,response);
					}else{
						if (url.startsWith("users")){
							url = removeChunck(url);
							if (url==null){
								try {
									returnProjectUsers(request,response,user,pID);
								} catch (SQLException e) {
									logger.error(e);
								} catch (DBPoolException e) {
									logger.error(e);
								}
							}else{
								
							}
						}else{
							response.sendError(400);
							return;
						}
					}
				}
			}
		}else if (url.startsWith("investigators")){
			url = removeChunck(url);
			if (url==null){				
			    //return all projects
			    try {
					response.setHeader("Cache-Control", "no-cache");
					String query = "SELECT DISTINCT ON ( inv.lastname,inv.firstname) inv.firstname,inv.lastname,inv.institution,inv.department,inv.email,inv.xnat_investigatorData_id,login FROM xnat_investigatorData inv LEFT JOIN xdat_user u ON ((lower(inv.firstname)=lower(u.firstname) AND lower(inv.lastname)=lower(u.lastname)) OR inv.email=u.email) ORDER BY inv.lastname,inv.firstname";
					XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
					
					String title = "ALL Investigators";
					this.returnTable(title, table, request, response);
				} catch (SQLException e) {
					logger.error(e);
				} catch (DBPoolException e) {
					logger.error(e);
				}
			}else{
				
			}
		}
	}
	

	
	public List<String> retrieveAllTags(final XDATUser user){
		try {
			return (List<String>)(XFTTable.Execute("SELECT DISTINCT tag from xdat_userGroup", user.getDBName(), user.getLogin()).convertColumnToArrayList("tag"));
		} catch (SQLException e) {
			logger.error("",e);
		} catch (DBPoolException e) {
			logger.error("",e);
		}
		
		return Lists.newArrayList();
	}
	
	private void returnProjectUsers(HttpServletRequest request, HttpServletResponse response,XDATUser user,String pID) throws IOException,SQLException,DBPoolException{
        response.setHeader("Cache-Control", "no-cache");
        
        if(!retrieveAllTags(user).contains(pID)){
        	logger.error("Unknown Project ID: " + pID,new Exception());
        	return;
        }
        
		String query = "SELECT g.id AS \"GROUP_ID\", displayname,login,firstname,lastname,email FROM xdat_userGroup g RIGHT JOIN xdat_user_Groupid map ON g.id=map.groupid RIGHT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE tag='" + pID + "' ORDER BY g.id DESC;";
		XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());

        String title = pID+ " USERS";
        this.returnTable(title, table, request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = XDAT.getUserDetails();
		if (user==null){
			response.sendError(401);
			return;
		}
		String url = parseContext(request);
		
		String format=request.getParameter("format");
		
		if (url.startsWith("projects")){
			url = removeChunck(url);
			if (url==null){
				//return all projects
			}else{
				String pID=getChunck(url);
				url = removeChunck(url);
				XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
				if (project==null){
					response.sendError(404);
					return;
				}else{
					if (url ==null){
						response.sendError(400);
					}else{
						if (url.startsWith("users")){
							url = removeChunck(url);
							if (url==null){
								response.sendError(400);
								return;
							}else{
								String gID=getChunck(url);
								url = removeChunck(url);

								CriteriaCollection cc = new CriteriaCollection("OR");
								cc.addClause("xdat:userGroup/ID", gID);
								cc.addClause("xdat:userGroup/ID", pID + "_" +gID);
								CriteriaCollection subCC = new CriteriaCollection("AND");
								subCC.addClause("xdat:userGroup/tag", pID);
								subCC.addClause("xdat:userGroup/displayName", gID);
								cc.addClause(subCC);
								
								try {
									ItemI gItem = ItemSearch.GetItems(cc, user, false).getFirst();
									if (gItem==null){
										response.sendError(404);
									}else{
										XdatUsergroup group = new XdatUsergroup(gItem);
										if(url==null){
											response.sendError(400);
										}else{
											try {
												if (!user.canDelete(project)){
													response.sendError(401);
													return;
												}
											} catch (InvalidItemException e1) {	
												logger.error(e1);
											} catch (Exception e1) {	
												logger.error(e1);
											}
											ArrayList<XDATUser> newUsers= new ArrayList<XDATUser>();
																						
											String tempValue=getChunck(url);
											String[] ids=null;
											if(tempValue.indexOf(",")>-1){
												ids=tempValue.split(",");
											}else{
												ids=new String[]{tempValue};
											}
											
											ArrayList<String> unknown= new ArrayList<String>();
											
											for(int i=0;i<ids.length;i++){
												String uID=ids[i].trim();
												Integer xdat_user_id= null;
												try {
													xdat_user_id=Integer.parseInt(uID);
												} catch (NumberFormatException e) {
													
												}
												
												
												if (xdat_user_id==null){
													//login or email
													XDATUser newUser=null;
													try {
														newUser = new XDATUser(uID);
													} catch (XDATUser.UserNotFoundException e) {
													}
													if (newUser==null){
														//by email
														ArrayList<ItemI> items =ItemSearch.GetItems("xdat:user/email", uID, user, false).items();
														if(items.size()>0){
															for(ItemI temp: items){
																newUsers.add(new XDATUser(temp));
															}
														}else{
															unknown.add(uID);
														}
													}else{
														newUsers.add(newUser);
													}
												}else{
													XdatUser tempUser = XdatUser.getXdatUsersByXdatUserId(xdat_user_id, user, false);
													if (tempUser!=null){
														newUsers.add(new XDATUser(tempUser.getItem()));
													}
												}
											}
											
											if (unknown.size()>0){
												//NEW USER
					            				RunData data = populateRunData(request,response);
					                            
					                            for(String uID : unknown){
						                            Context context = TurbineVelocity.getContext(data);
													context.put("user",user);
										            context.put("server",TurbineUtils.GetFullServerPath(request));
										            context.put("process","Transfer to the archive.");
										            context.put("system",TurbineUtils.GetSystemName());
										            context.put("access_level",gID);
										            context.put("admin_email",AdminUtils.getAdminEmailId());
										            context.put("projectOM",project);
										            //SEND email to user
										            ProjectAccessRequest.InviteUser(context, uID, user, user.getFirstname() + " " + user.getLastname() + " has invited you to join the " + project.getName() + " project.");
									            }
											}
											
											if (newUsers.size()>0){
												//CURRENT USER
												String email=request.getParameter("sendmail");
												boolean sendmail=Boolean.parseBoolean(email);
												
												for(XDATUser newUser: newUsers){
													project.addGroupMember(group.getId(), newUser, user);
							                        try {
							            				WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
							            				workflow.setDataType("xnat:projectData");
							            				workflow.setExternalid(project.getId());
							            				workflow.setId(project.getId());
							            				workflow.setPipelineName("New Member: " + newUser.getFirstname() + " " + newUser.getLastname());
							            				workflow.setStatus("Complete");
							            				workflow.setLaunchTime(Calendar.getInstance().getTime());
							            				workflow.save(user, false, false);
							            			} catch (Throwable e) {
							            				logger.error(e);
							            			}
							                        if (sendmail){
							            				RunData data = populateRunData(request,response);
							                            Context context = TurbineVelocity.getContext(data);
							            				
							                        	context.put("user",TurbineUtils.getUser(data));
											            context.put("server",TurbineUtils.GetFullServerPath(request));
							                            context.put("process","Transfer to the archive.");
							                            context.put("system",TurbineUtils.GetSystemName());
							                            context.put("access_level","member");
							                            context.put("admin_email",AdminUtils.getAdminEmailId());
							                            context.put("projectOM",project);
							                        	org.nrg.xnat.turbine.modules.actions.ProcessAccessRequest.SendAccessApprovalEmail(context, newUser.getEmail(), TurbineUtils.getUser(data), TurbineUtils.GetSystemName() + " Access Granted for " + project.getName());
							                        }
												}
											}
											try {
												returnProjectUsers(request,response,user,pID);
											} catch (SQLException e) {
												logger.error(e);
											} catch (DBPoolException e) {
												logger.error(e);
											}
										}
									}
								} catch (Exception e) {
									logger.error(e);
									response.sendError(500);
								}
								
							}
						}else{
							response.sendError(400);
							return;
						}
					}
				}
			}
		}	else if (url.startsWith("investigators")){
			url = removeChunck(url);
			if (url==null){
				//return all investigators
			}else{
				String lastname=getChunck(url);
				String xmlString = request.getParameter("investigator_xml");
	            StringReader sr = new StringReader(xmlString);
	            InputSource is = new InputSource(sr);
	            SAXReader reader = new SAXReader(user);
                try {
					XFTItem item = reader.parse(is);
					XnatInvestigatordata investigator=new XnatInvestigatordata(item);
					if(investigator.getItem().getCurrentDBVersion()==null){
						investigator.save(user, false, false);
					}
					response.setHeader("Cache-Control", "no-cache");
					String query = "SELECT DISTINCT ON ( inv.lastname,inv.firstname) inv.firstname,inv.lastname,inv.institution,inv.department,inv.email,inv.xnat_investigatorData_id,login FROM xnat_investigatorData inv LEFT JOIN xdat_user u ON ((lower(inv.firstname)=lower(u.firstname) AND lower(inv.lastname)=lower(u.lastname)) OR inv.email=u.email) ORDER BY inv.lastname,inv.firstname";
					XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
					
					String title = "ALL Investigators";
					this.returnTable(title, table, request, response);
				} catch (SAXException e) {
					logger.error(e);
					response.sendError(500);
				} catch (Exception e) {
					logger.error(e);
					response.sendError(500);
				}
			}
		}
	}



	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = XDAT.getUserDetails();
		if (user==null){
			response.sendError(401);
			return;
		}
		String url = parseContext(request);
		
		String format=request.getParameter("format");
		
		if (url.startsWith("projects")){
			url = removeChunck(url);
			if (url==null){
				//return all projects
			}else{
				String pID=getChunck(url);
				url = removeChunck(url);
				XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
				if (project==null){
					response.sendError(404);
					return;
				}else{
					if (url ==null){
						response.sendError(400);
					}else{
						if (url.startsWith("accessibility")){
							url = removeChunck(url);
							if (url==null){
								response.sendError(400);
								return;
							}else{
								String access=getChunck(url);
								url = removeChunck(url);

								try {
									if (!user.canDelete(project)){
										response.sendError(401);
										return;
									}
								} catch (InvalidItemException e1) {	
									logger.error(e1);
								} catch (Exception e1) {	
									logger.error(e1);
								}
								
				                try {
									String currentAccess = project.getPublicAccessibility();

									if (!currentAccess.equals(access)){

										project.initAccessibility(access, true);

									}
								} catch (Exception e) {
									logger.error(e);
									response.sendError(500);
								}
							}
						}else{
							response.sendError(400);
							return;
						}
					}
				}
			}
		}	
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = XDAT.getUserDetails();
		if (user==null){
			response.sendError(401);
			return;
		}
		String url = parseContext(request);
		
		String format=request.getParameter("format");
		
		if (url.startsWith("projects")){
			url = removeChunck(url);
			if (url==null){
				//return all projects
			}else{
				String pID=getChunck(url);
				url = removeChunck(url);
				XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
				
				try {
					if (!user.canDelete(project)){
						response.sendError(401);
						return;
					}
				} catch (InvalidItemException e1) {	
					logger.error(e1);
				} catch (Exception e1) {	
					logger.error(e1);
				}
				
				if (project==null){
					response.sendError(404);
					return;
				}else{
					if (url ==null){
						response.sendError(400);
					}else{
						if (url.startsWith("users")){
							url = removeChunck(url);
							if (url==null){
								response.sendError(400);
								return;
							}else{
								String gID=getChunck(url);
								url = removeChunck(url);

								CriteriaCollection cc = new CriteriaCollection("OR");
								cc.addClause("xdat:userGroup/ID", gID);
								cc.addClause("xdat:userGroup/ID", pID + "_" +gID);
								CriteriaCollection subCC = new CriteriaCollection("AND");
								subCC.addClause("xdat:userGroup/tag", pID);
								subCC.addClause("xdat:userGroup/displayName", gID);
								cc.addClause(subCC);
								
								try {
									ItemI gItem = ItemSearch.GetItems(cc, user, false).getFirst();
									if (gItem==null){
										response.sendError(404);
									}else{
										XdatUsergroup group = new XdatUsergroup(gItem);
										if(url==null){
											response.sendError(400);
										}else{
											String uID=getChunck(url);
											Integer xdat_user_id= null;
											try {
												xdat_user_id=Integer.parseInt(uID);
											} catch (NumberFormatException e) {
												
											}
											
											ArrayList<XDATUser> newUsers= new ArrayList<XDATUser>();
											
											if (xdat_user_id==null){
												//login or email
												XDATUser newUser=null;
												try {
													newUser = new XDATUser(uID);
												} catch (XDATUser.UserNotFoundException e) {
												}
												if (newUser==null){
													//by email
													ArrayList<ItemI> items =ItemSearch.GetItems("xdat:user/email", uID, user, false).items();
													for(ItemI temp: items){
														newUsers.add(new XDATUser(temp));
													}
												}else{
													newUsers.add(newUser);
												}
											}else{
												XdatUser tempUser = XdatUser.getXdatUsersByXdatUserId(xdat_user_id, user, false);
												if (tempUser!=null){
													newUsers.add(new XDATUser(tempUser.getItem()));
												}
											}
											
											if (newUsers.size()==0){
												response.sendError(404);
											}else{
												//CURRENT USER												
												for(XDATUser newUser: newUsers){
							                        project.removeGroupMember(group.getId(), newUser, user);													
												}
											}
											try {
												returnProjectUsers(request,response,user,pID);
											} catch (SQLException e) {
												logger.error(e);
											} catch (DBPoolException e) {
												logger.error(e);
											}
										}
									}
								} catch (Exception e) {
									logger.error(e);
									response.sendError(500);
								}
								
							}
						}else{
							response.sendError(400);
							return;
						}
					}
				}
			}
		}	
	}

	private String parseContext(HttpServletRequest request){
		System.out.println("PathInfo: "+ request.getPathInfo());
        String path = request.getPathInfo();
        if (path.startsWith("/")){
            path = path.substring(1);
        }
        return path;
	}
	
	private String getChunck(String url){
		int i = url.indexOf("/");
		if (i==-1){
			return url;
		}else{
			return url.substring(0,i);
		}
	}
	
	private String removeChunck(String url){
		int i = url.indexOf("/");
		if (i==-1){
			return null;
		}else{
			return url.substring(i +1);
		}
	}
	
	private void returnTable(String title, XFTTable table,HttpServletRequest request, HttpServletResponse response) throws IOException{
		String format = request.getParameter("format");
		ServletOutputStream out = response.getOutputStream();
		OutputStreamWriter sw = new OutputStreamWriter(out);
		BufferedWriter writer = new BufferedWriter(sw);
		if(format!=null && format.equals("xml_results")){
		    response.setContentType("text/xml");
			table.toXMLList(writer, title);
		}else if(format!=null && format.equals("json")){
		    response.setContentType("application/json");
		    writer.write("({\"ResultSet\":{\"Result\":");
			table.toJSON(writer);
			writer.write("}})");
		}else{
		    response.setContentType("application/xhtml+xml");
		    writer.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		    writer.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
		    writer.write("<head><title>" + title + "</title></head><body>");
		    table.toHTML(true,writer);
		    writer.write("</body></html>");
		}
		writer.flush();
		writer.close();
	}
	
	private RunData populateRunData(HttpServletRequest request, HttpServletResponse response) throws TurbineException{
		RunDataService rundataService = null;
		rundataService = TurbineRunDataFacade.getService();
		if (rundataService == null)
		{
		    throw new TurbineException(
		            "No RunData Service configured!");
		}
		RunData data = rundataService.getRunData(request, response, this.getServletConfig());
		//RENAME script name /REST to /app
		data.getServerData().setScriptName("/app");
		
		return data;
	}
	
	private void returnItem(ItemI item, HttpServletRequest request, HttpServletResponse response) throws IOException{
		String format = request.getParameter("format");
		if(format!=null && format.equals("xnat")){
			try {
				response.setContentType("text/xml");
				item.toXML(response.getWriter(), true);
			} catch (IllegalArgumentException e) {
				logger.error(e);
			} catch (SAXException e) {
				logger.error(e);
			}
		}else{
			try {
				RunData data = populateRunData(request,response);
				TurbineUtils.setDataItem(data, item);
				
				try {
					String screen =DisplayItemAction.GetReportScreen(item.getItem().getGenericSchemaElement());
	            	data.setScreenTemplate(screen);
	            	turbineScreen(data);
				} catch (ElementNotFoundException e) {
					logger.error(e);
				}
			} catch (TurbineException e) {
				logger.error(e);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}
	
	protected void turbineScreen(RunData data)throws IOException,Exception{
		TemplateService templateService = null;
        templateService = TurbineTemplate.getService();
        String defaultPage = (templateService == null)
                ? null :templateService.getDefaultPageName(data);

        PageLoader.getInstance().exec(data, defaultPage);
		
		//COPIED FROM org.apache.turbine.Turbine.doGet
        if (data.isPageSet() && data.isOutSet() == false)
        {
            // Modules can override these.
            data.getResponse().setLocale(data.getLocale());
            data.getResponse().setContentType(
                    data.getContentType());

            // Set the status code.
            data.getResponse().setStatus(data.getStatusCode());
            // Output the Page.
            data.getPage().output(data.getOut());
        }
	}
}
