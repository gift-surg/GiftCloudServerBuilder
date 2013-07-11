/*
 * org.nrg.xnat.restlet.resources.search.SearchResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.resources.search;

import com.noelios.restlet.ext.servlet.ServletCall;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.nrg.xdat.collections.DisplayFieldCollection.DisplayFieldNotFoundException;
import org.nrg.xdat.display.DisplayFieldReferenceI;
import org.nrg.xdat.display.HTMLLink;
import org.nrg.xdat.display.HTMLLinkProperty;
import org.nrg.xdat.display.SQLQueryField;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.restlet.presentation.RESTHTMLPresenter;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.*;

public class SearchResource extends SecureResource {
	static org.apache.log4j.Logger logger = Logger.getLogger(SearchResource.class);
	XFTTable table= null;
	Long rows=null;
	String tableName=null;

	String rootElementName=null;
	
	Hashtable<String,Object> tableParams=new Hashtable<String,Object>();
	Map<String,Map<String,String>> cp=new LinkedHashMap<String,Map<String,String>>();
	
	public SearchResource(Context context, Request request, Response response) {
		super(context, request, response);
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	@Override
	public boolean allowGet() {
		return false;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {
            try {
				String cacheRequest = this.getQueryVariable("cache");
				boolean cache = false;
				if (cacheRequest!=null && cacheRequest.equalsIgnoreCase("true")){
					cache =true;
				}
				
			XFTItem item=null;
			Representation entity = this.getRequest().getEntity();
			if(entity!=null && entity.getMediaType()!=null && entity.getMediaType().getName().equals(MediaType.MULTIPART_FORM_DATA.getName())){
				try {
					org.apache.commons.fileupload.DefaultFileItemFactory factory = new org.apache.commons.fileupload.DefaultFileItemFactory();
					org.restlet.ext.fileupload.RestletFileUpload upload = new  org.restlet.ext.fileupload.RestletFileUpload(factory);

					List items = upload.parseRequest(this.getRequest());
					String xml_text="";

					int i = 0;
					for (final Iterator it = items.iterator(); it.hasNext(); ) {    
					    FileItem fi = (FileItem)it.next();
					     
					    String fileName=fi.getName();
					    if(fileName.indexOf("\\")>-1){
					    	fileName.substring(fileName.indexOf("\\")+1);
					    }
					    
					    if(fi.getName().endsWith(".xml")){
							SAXReader reader = new SAXReader(user);
							if(item!=null)
							{
								reader.setTemplate(item);
							}
							try {
								item = reader.parse(fi.getInputStream());

								if(!reader.assertValid()){
									throw reader.getErrors().get(0);
								}
								if (XFT.VERBOSE)
								    System.out.println("Loaded XML Item:" + item.getProperName());
								
								if(item!=null){
									completeDocument=true;
								}
							} catch (SAXParseException e) {
								e.printStackTrace();
								this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
								throw e;
							} catch (IOException e) {
								e.printStackTrace();
								this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
							} catch (Exception e) {
								e.printStackTrace();
								this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
							}
					    }
					}
				} catch (org.apache.commons.fileupload.FileUploadException e) {
					e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			}else{
				if(entity!=null){
					Reader sax=entity.getReader();
			        try {
								
				SAXReader reader = new SAXReader(user);
						if(item!=null)
						{
							reader.setTemplate(item);
						}
						
						item = reader.parse(sax);
	
						if(!reader.assertValid()){
							throw reader.getErrors().get(0);
						}
			            if (XFT.VERBOSE)
			                System.out.println("Loaded XML Item:" + item.getProperName());
			            
			            if(item!=null){
							completeDocument=true;
			            }
			            
					} catch (SAXParseException e) {
						e.printStackTrace();
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
						throw e;
					} catch (IOException e) {
						e.printStackTrace();
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					} catch (Exception e) {
						e.printStackTrace();
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					}
				}
			}
				
				if(!item.instanceOf("xdat:stored_search")){
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
					return;
				}
				XdatStoredSearch search = new XdatStoredSearch(item);
					
			rootElementName=search.getRootElementName();
			
				DisplaySearch ds=search.getDisplaySearch(user);
				
				String sortBy = this.getQueryVariable("sortBy");
				String sortOrder = this.getQueryVariable("sortOrder");
				if (sortBy != null){
				    ds.setSortBy(sortBy);
				    if(sortOrder != null)
				    {
				        ds.setSortOrder(sortOrder);
				    }
				}
				
				MaterializedView mv=null;
					
				if(search.getId()!=null && !search.getId().equals("")){
					mv = MaterializedView.GetMaterializedViewBySearchID(search.getId(), user);
				}
				
			if(mv!=null && (search.getId().startsWith("@") || this.isQueryVariableTrue("refresh"))){
					mv.delete();
					mv=null;
				}

			cp=setColumnProperties(ds,user,this);
				
				if (!cache){
					if(mv!=null){
						table=mv.getData(null, null, null);
					}else{
					    ds.setPagingOn(false);
					MediaType mt = this.getRequestedMediaType();
						if (mt!=null && mt.equals(SecureResource.APPLICATION_XLIST)){
							table=(XFTTable)ds.execute(new RESTHTMLPresenter(TurbineUtils.GetRelativePath(ServletCall.getRequest(this.getRequest())),null,user,sortBy),user.getLogin());
						}else{
					    table=(XFTTable)ds.execute(null,user.getLogin());
					}
					    //table=(XFTTable)ds.execute(null,user.getLogin());

				}
				}else{
					if(mv!=null){
						if(search.getId()!=null && !search.getId().equals("") && mv.getLast_access()!=null)
							tableParams.put("last_access", mv.getLast_access());
						table=mv.getData(null, null, 0);
						tableName=mv.getTable_name();
						rows=mv.getSize();
					}else{
						ds.setPagingOn(false);
						
						String query = ds.getSQLQuery(null);
						query = StringUtils.ReplaceStr(query,"'","*'*");
						query = StringUtils.ReplaceStr(query,"*'*","''");
						
						mv = new MaterializedView(user);
						if(search.getId()!=null && !search.getId().equals(""))
							mv.setSearch_id(search.getId());
						mv.setSearch_sql(query);
						mv.setSearch_xml(item.writeToFlatString(0));
						mv.save();

						if(search.getId()!=null && !search.getId().equals("") && mv.getLast_access()!=null)
							tableParams.put("last_access", mv.getLast_access());
						
						tableName=mv.getTable_name();
					
						int limit=0;
						if(this.getQueryVariable("limit")!=null)
							limit=Integer.valueOf(this.getQueryVariable("limit"));
						table=mv.getData(null, null, limit);
						rows=mv.getSize();
					}
				}
				
				this.returnDefaultRepresentation();
			} catch (IOException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SAXException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
			} catch (ElementNotFoundException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (XFTInitException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (FieldNotFoundException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (DBPoolException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SQLException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (IllegalAccessException e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
			logger.error("Failed POST",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}



	@Override
	public Representation getRepresentation(Variant variant) {	
		if(tableName!=null){
			tableParams.put("ID", tableName);
		}
		
		if(rows!=null){
			tableParams.put("totalRecords", rows);
		}else{
			tableParams.put("totalRecords", table.getNumRows());
		}

		if(rootElementName!=null){
			tableParams.put("rootElementName", rootElementName);
		}

		MediaType mt = overrideVariant(variant);
		
		return this.representTable(table, mt, tableParams,cp);
	}
	
	public static LinkedHashMap<String,Map<String,String>> setColumnProperties(DisplaySearch search,XDATUser user, SecureResource sr){
		LinkedHashMap<String,Map<String,String>> cp=new LinkedHashMap<String,Map<String,String>>();
		try {
			List<DisplayFieldReferenceI> fields = search.getAllFields("");

			//int fieldCount = visibleFields.size() + search.getInClauses().size();

			if (search.getInClauses().size()>0)
			{
			    for(int i=0;i<search.getInClauses().size();i++)
			    {
			        cp.put("search_field"+i,new Hashtable<String,String>());
			        cp.get("search_field"+i).put("header", "");
			    }
			}

			//POPULATE HEADERS

			for (DisplayFieldReferenceI dfr:fields)
			{
				try {
					String id=null;
					if(dfr.getValue()!=null && !dfr.getValue().equals("")){
						if(dfr.getValue().equals("{XDAT_USER_ID}")){
							dfr.setValue(user.getXdatUserId());
						}
					}
					if (dfr.getElementName().equalsIgnoreCase(search.getRootElement().getFullXMLName()))
					{
						id = dfr.getRowID().toLowerCase();
					}else{
						id = dfr.getElementSQLName().toLowerCase() + "_" + dfr.getRowID().toLowerCase();
					}
					cp.put(id,new Hashtable<String,String>());
					cp.get(id).put("element_name", dfr.getElementName());
					try {
						String temp_id=dfr.getDisplayField().getId();
						if(dfr.getValue()!=null)
							temp_id+="="+dfr.getValue();
						cp.get(id).put("id", temp_id);
					} catch (DisplayFieldNotFoundException e2) {
						e2.printStackTrace();
					}
					cp.get(id).put("xPATH", dfr.getElementName() + "." + dfr.getSortBy());
					
					if (dfr.getHeader().equalsIgnoreCase(""))
					{
						cp.get(id).put("header", " ");
					}else{
						cp.get(id).put("header", dfr.getHeader());
					}

					String t=dfr.getType();
					if(t==null){
						try {
							if(dfr.getDisplayField()!=null){
								t=dfr.getDisplayField().getDataType();
							}
						} catch (DisplayFieldNotFoundException e) {
							e.printStackTrace();
						}
					}
					if(t!=null){
						cp.get(id).put("type", t);
					}

					try {
						if(!dfr.isVisible()){
							cp.get(id).put("visible","false");
						}
					} catch (DisplayFieldNotFoundException e1) {
						e1.printStackTrace();
					}

					boolean hasLink = false;

					if (dfr.getHTMLLink() != null && sr.getQueryVariable("format")!=null && sr.getQueryVariable("format").equalsIgnoreCase("json"))
					{
						cp.get(id).put("clickable", "true");
						HTMLLink link = dfr.getHTMLLink();
						
						StringBuffer linkProps=new StringBuffer("[");
						int propCounter=0;
						for(HTMLLinkProperty prop: link.getProperties()){
							if(propCounter++>0)linkProps.append(",");
							linkProps.append("{");
							linkProps.append("\"name\":\"");
							linkProps.append(prop.getName()).append("\"");
							linkProps.append(",\"value\":\"");
							String v =prop.getValue();
							v = StringUtils.ReplaceStr(v,"@WEBAPP",TurbineUtils.GetRelativePath(ServletCall.getRequest(sr.getRequest())) + "/");
							
							linkProps.append(v).append("\"");

							if(prop.getInsertedValues().size()>0)
							{
								linkProps.append(",\"inserts\":[");
								int valueCounter=0;
								for(Map.Entry<String, String> entry :prop.getInsertedValues().entrySet())
								{
									if(valueCounter++>0)linkProps.append(",");
									linkProps.append("{\"name\":\"");
									linkProps.append(entry.getKey()).append("\"");
									linkProps.append(",\"value\":\"");
									
									String insert_value = entry.getValue();
                                    if (insert_value.startsWith("@WHERE")){
                                        try {
											if (dfr.getDisplayField() instanceof SQLQueryField){
											    Object insertValue = dfr.getValue();

											    if (insertValue == null)
											    {
											        insertValue = "NULL";
											    }else{
											        if (insertValue.toString().indexOf(",")!=-1){
											        	insert_value = insert_value.substring(6);
											            try {
											                Integer i = Integer.parseInt(insert_value);
											                ArrayList<String> al = StringUtils.CommaDelimitedStringToArrayList(insertValue.toString());
											                insertValue =al.get(i);
											            } catch (Throwable e) {
											                logger.error("",e);
											            }
											        }
											    }

		    									linkProps.append("@"+insertValue);
											}
										} catch (DisplayFieldNotFoundException e) {
											e.printStackTrace();
										}
                                    }else{
                                         if (! dfr.getElementName().equalsIgnoreCase(search.getRootElement().getFullXMLName()))
                                         {
                                        	 insert_value = dfr.getElementSQLName().toLowerCase() + "_" + insert_value.toLowerCase();
                                         }else{
                                        	 insert_value=insert_value.toLowerCase();
                                         }
                                         if(cp.get(insert_value)==null){
                          					cp.put(insert_value,new Hashtable<String,String>());
                         					
                        					if (! dfr.getElementName().equalsIgnoreCase(search.getRootElement().getFullXMLName()))
                                            {
                            					cp.get(insert_value).put("xPATH", dfr.getElementName() + "." + insert_value);
                                            }else{
                            					cp.get(insert_value).put("xPATH", insert_value);
                                            }
                                         }
                    					
    									linkProps.append(insert_value);
                                    }
									linkProps.append("\"}");
								}
								linkProps.append("]");
							}
							linkProps.append("}");
						}
						linkProps.append("]");
						
						cp.get(id).put("linkProps", linkProps.toString());
					}
					
					if (dfr.isImage())
					{
						cp.get(id).put("imgRoot", TurbineUtils.GetRelativePath(ServletCall.getRequest(sr.getRequest())) + "/");
					}
				} catch (XFTInitException e) {
					logger.error("",e);
				} catch (ElementNotFoundException e) {
					logger.error("",e);
				}

				
			}
			
			cp.put("quarantine_status",new Hashtable<String,String>());
		} catch (ElementNotFoundException e) {
			logger.error("",e);
		} catch (XFTInitException e) {
			logger.error("",e);
		}
		
		return cp;
	}
}
