// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.collections.CollectionUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.nrg.action.ActionException;
import org.nrg.dcm.Dcm2Jpg;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.helpers.resource.direct.ResourceModifierA.UpdateMeta;
import org.nrg.xnat.restlet.files.utils.RestFileUtils;
import org.nrg.xnat.restlet.representations.CatalogRepresentation;
import org.nrg.xnat.restlet.representations.ZipRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.nrg.xnat.utils.CatalogUtils;
import org.nrg.xnat.utils.CatalogUtils.CatEntryFilterI;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * @author timo
 *
 */
public class FileList extends XNATCatalogTemplate {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FileList.class);

	static String[] zipExtensions={".zip",".jar",".rar",".ear",".gar"};
	
	String filepath = null;
	
	XnatAbstractresource resource =null;
		
	public FileList(Context context, Request request, Response response) {
		super(context, request, response,true);
			try {
				if(catalogs!=null && catalogs.size()>0  && resource_ids!=null){
	
					for(Object[] row: catalogs.rows()){
						Integer id = (Integer)row[0];
						String label = (String)row[1];
						
						for(String resourceID:this.resource_ids){
							if(id.toString().equals(resourceID) || (label!=null && label.equals(resourceID))){
								XnatAbstractresource res=XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(row[0], user, false);
								if(row.length==7)res.setBaseURI((String)row[6]);
								resources.add(res);
							}
						}
						
					}
							
					if(resources.size()>0){
						resource=resources.get(0);
					}
				}
				
				filepath = this.getRequest().getResourceRef().getRemainingPart();
				if(filepath!=null && filepath.contains("?")){
					filepath = filepath.substring(0,filepath.indexOf("?"));
				}
				
				if(filepath!=null && filepath.startsWith("/")){
					filepath=filepath.substring(1);
				}
				
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
				this.getVariants().add(new Variant(MediaType.IMAGE_JPEG));
			} catch (Exception e) {
	            logger.error("",e);
			}
	}


	@Override
	public boolean allowPost() {
		return true;
	}
	
	public boolean allowPut(){
		return true;
	}
	
	public void handlePut(){
		handlePost();
	}

	@Override
	public void handlePost() {
		if(this.parent!=null && this.security!=null){
			try {
				if(user.canEdit(this.security)){
					String securityId=null;
					if(proj==null){
						if(parent.getItem().instanceOf("xnat:experimentData")){
							securityId=((XnatExperimentdata)parent).getId();
							proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
						}else if(security.getItem().instanceOf("xnat:experimentData")){
							securityId=((XnatExperimentdata)security).getId();
							proj = ((XnatExperimentdata)security).getPrimaryProject(false);
						}
					}
					
					final Object resourceIdentifier;
					
					if(resource==null){
						if(catalogs.rows().size()>0){
							resourceIdentifier=catalogs.getFirstObject();
						}else{
							if(resource_ids.size()>0){
								resourceIdentifier=resource_ids.get(0);
							}else{
								resourceIdentifier=null;
							}
						}
					}else{
						resourceIdentifier=resource.getXnatAbstractresourceId();
					}

					final boolean overwrite=this.isQueryVariableTrue("overwrite");


					PersistentWorkflowI wrk=PersistentWorkflowUtils.getWorkflowByEventId(user,getEventId());
					if(wrk==null && resource!=null && "SNAPSHOTS".equals(resource.getLabel())){
						if(getSecurityItem() instanceof XnatExperimentdata){
							Collection<? extends PersistentWorkflowI> wrks=PersistentWorkflowUtils.getOpenWorkflows(user,((ArchivableItem)security).getId());
							if(wrks!=null && wrks.size()==1){
								wrk=(WrkWorkflowdata)CollectionUtils.get(wrks, 0);
								if(!"xnat_tools/AutoRun.xml".equals(wrk.getPipelineName())){
									wrk=null;
								}
							}
						}
					}
					
					boolean skipUpdateStats=isQueryVariableFalse("update-stats");
					
					boolean isNew=false;
					if(wrk==null && !skipUpdateStats){
						isNew=true;
						wrk=PersistentWorkflowUtils.buildOpenWorkflow(user, getSecurityItem().getItem(), newEventInstance(EventUtils.CATEGORY.DATA,(getAction()!=null)?getAction():EventUtils.UPLOAD_FILE));
					}
					
					final EventMetaI i;
					if(wrk==null){
						i=EventUtils.ADMIN_EVENT(user);
					}else{
						i=wrk.buildEvent();
					}
					
					UpdateMeta um= new UpdateMeta(i,!(skipUpdateStats));
					
					try {
						this.buildResourceModifier(overwrite,um).addFile(getFileWriters(),resourceIdentifier,type, filepath, this.buildResourceInfo(um),overwrite);
					} catch (Exception e) {
						logger.error("",e);
						throw e;
					}


					if(isNew){
						WorkflowUtils.complete(wrk, i);
					}
				}
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				logger.error("",e);
				return;
			}
		}
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public void handleDelete(){
		if(resource!=null && this.parent!=null && this.security!=null){
			try {
				if(user.canEdit(this.security)){
					
					if(proj==null){
						if(parent.getItem().instanceOf("xnat:experimentData")){
							proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
						}else if(security.getItem().instanceOf("xnat:experimentData")){
							proj = ((XnatExperimentdata)security).getPrimaryProject(false);
						}
					}
						
					if(resource instanceof XnatResourcecatalog){
						final XnatResourcecatalog catResource=(XnatResourcecatalog)resource;
						
						final File catFile = catResource.getCatalogFile(proj.getRootArchivePath());
						final String parentPath=catFile.getParent();
						final CatCatalogBean cat=(CatCatalogBean)catResource.getCleanCatalog(proj.getRootArchivePath(), false,null,null);

						CatEntryBean entry = (CatEntryBean)CatalogUtils.getEntryByURI(cat, filepath);
						
						if(entry==null){
							entry = (CatEntryBean)CatalogUtils.getEntryById(cat, filepath);
						}
						
						if(entry==null){
							this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify specified file.");
							return;
						}
						
						final File f = new File(parentPath,entry.getUri());
						
						if(f.exists()){
							PersistentWorkflowI work=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, this.security.getItem(), newEventInstance(EventUtils.CATEGORY.DATA,EventUtils.REMOVE_FILE));
							EventMetaI ci=work.buildEvent();
					
							CatalogUtils.removeEntry(cat, entry);
							//update for file deletion
							CatalogUtils.writeCatalogToFile(cat, catFile);
							
							CatalogUtils.moveToHistory(catFile,cat,f,entry,ci);
							
							//if parent folder is empty, then delete folder
							if(FileUtils.CountFiles(f.getParentFile(),true)==0){
								FileUtils.DeleteFile(f.getParentFile());
							}
							
							CatalogUtils.populateStats(catResource,proj.getRootArchivePath());
							SaveItemHelper.authorizedSave(catResource,user, false, false, ci);
							
							WorkflowUtils.complete(work, ci);
						}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"File missing");
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"File missing");
						return;
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to modify this session.");
					return;
				}
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				return;
			}
		}
	}

	/*******************************************
	 * if(filepath>"")then returns File
	 * else returns table of files
	 */
	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
			try {
				if(proj==null){
					//setting project as primary project, or shared project
					//this only works because the absolute paths are stored in the database for each resource, so the actual project path isn't used.
                    if(parent!=null && parent.getItem().instanceOf("xnat:experimentData")){
                    	proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
                            // Per FogBugz 4746, prevent NPE when user doesn't have access to resource (MRH)
                            // Check access through shared project when user doesn't have access to primary project
                            if (proj == null) {
                                    proj = (XnatProjectdata)((XnatExperimentdata)parent).getFirstProject();
                            }
                    }else if(security!=null && security.getItem().instanceOf("xnat:experimentData")){
                            proj = ((XnatExperimentdata)security).getPrimaryProject(false);
                            // Per FogBugz 4746, ....
                            if (proj == null) {
                                    proj = (XnatProjectdata)((XnatExperimentdata)security).getFirstProject();
                            }
                    }else if(security!=null && security.getItem().instanceOf("xnat:subjectData")){
                            proj = ((XnatSubjectdata)security).getPrimaryProject(false);
                            // Per FogBugz 4746, ....
                            if (proj == null) {
                                    proj = (XnatProjectdata)((XnatSubjectdata)security).getFirstProject();
                            }
                    }else if(security!=null && security.getItem().instanceOf("xnat:projectData")){
                            proj = (XnatProjectdata)security;
                    }
				}
				
			if(resources.size()==1 && !(isZIPRequest(mt))){
				//one catalog
				return handleSingleCatalog(mt);
			}else if(resources.size()>0){
				//multiple catalogs
				return handleMultipleCatalogs(mt);
			}else{
				//all catalogs
				catalogs.resetRowCursor();
				for(Hashtable<String,Object> rowHash: catalogs.rowHashs()){
					Object o =rowHash.get("xnat_abstractresource_id");
					XnatAbstractresource res=XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(o, user, false);
					if(rowHash.containsKey("resource_path"))res.setBaseURI((String)rowHash.get("resource_path"));
					resources.add(res);
				}
				
				return handleMultipleCatalogs(mt);				
			}
		} catch (ElementNotFoundException e) {
            logger.error("",e);
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
			return new StringRepresentation("");
		}
	}
	
	private Map<String,String> getReMaps(){
		return RestFileUtils.getReMaps(scans,recons);
	}
	
	public Representation representTable(XFTTable table, MediaType mt,Hashtable<String,Object> params,Map<String,Map<String,String>> cp,Map<String,String> session_mapping){
		if(mt.equals(SecureResource.APPLICATION_XCAT)){
			//"Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID"
			CatCatalogBean cat = new CatCatalogBean();
					
	        String server= TurbineUtils.GetFullServerPath(getHttpServletRequest());
	        if(server.endsWith("/")){
	            server=server.substring(0,server.length()-1);
	        }
			
			final int uriIndex=table.getColumnIndex("URI"); 
			final int sizeIndex=table.getColumnIndex("Size"); 
			
			final int collectionIndex=table.getColumnIndex("collection"); 
			final int cat_IDIndex=table.getColumnIndex("cat_ID"); 
			
			Map<String,String> valuesToReplace=this.getReMaps();
			
			for(Object[] row:table.rows()){
				
				CatEntryBean entry = new CatEntryBean();
                
				String uri=(String)row[uriIndex];
                String relative = RestFileUtils.getRelativePath(uri, session_mapping);
                
                entry.setUri(server+uri);
                
                relative = relative.replace('\\', '/');
                
                relative=RestFileUtils.replaceResourceLabel(relative, row[cat_IDIndex], (String)row[collectionIndex]);
                
                for(Map.Entry<String, String> e:valuesToReplace.entrySet()){
                    relative=RestFileUtils.replaceInPath(relative, e.getKey(), e.getValue());
                }
                
                entry.setCachepath(relative);
                
                CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
                meta.setMetafield(relative);
                meta.setName("RELATIVE_PATH");
                entry.addMetafields_metafield(meta);
                
                meta = new CatEntryMetafieldBean();
                meta.setMetafield(row[sizeIndex].toString());
                meta.setName("SIZE");
                entry.addMetafields_metafield(meta);
                
                cat.addEntries_entry(entry);
			}
			
			this.setContentDisposition("files.xcat", false);
			
			return new CatalogRepresentation(cat, mt,false);
		}else if(isZIPRequest(mt)){
			ZipRepresentation rep;
			try {
				rep = new ZipRepresentation(mt,this.getSessionIds(),identifyCompression(null));
			} catch (ActionException e) {
				logger.error("",e);
				this.setResponseStatus(e);
				return null;
			}
			
			final int uriIndex=table.getColumnIndex("URI"); 
			final int fileIndex=table.getColumnIndex("file"); 
			
			final int collectionIndex=table.getColumnIndex("collection"); 
			final int cat_IDIndex=table.getColumnIndex("cat_ID"); 

			//Refactored on 3/24 to allow the returning of the old file structure.  This was to support Mohana's legacy pipelines.
			String structure=this.getQueryVariable("structure");
			if(StringUtils.isEmpty(structure)){
				structure="default";
			}
			
			final Map<String,String> valuesToReplace;
			if(structure.equalsIgnoreCase("legacy") || structure.equalsIgnoreCase("simplified")){
				valuesToReplace=new Hashtable<String,String>();
			}else{
				valuesToReplace=this.getReMaps();
			}
			
			//TODO: This should all be rewritten.  The implementation of the path relativation should be injectable, particularly to support other possible structures.
			for(final Object[] row:table.rows()){	
				final String uri=(String)row[uriIndex];
				final File child=(File)row[fileIndex];
				if(child!=null && child.exists()){
					final String pathForZip;
					if(structure.equalsIgnoreCase("legacy")){
						pathForZip=child.getAbsolutePath();
					}else{
						pathForZip=uri;
					}
					
	                final String relative;
                    if (structure.equals("simplified")) {
                        relative = RestFileUtils.buildRelativePath(pathForZip, session_mapping, valuesToReplace, row[cat_IDIndex], (String)row[collectionIndex]).replace("/resources", "").replace("/files",  "");
                    } else {
                        relative = RestFileUtils.buildRelativePath(pathForZip, session_mapping, valuesToReplace, row[cat_IDIndex], (String)row[collectionIndex]);
                    }
	                
					rep.addEntry(relative, child);
				}
			}
			
			if(rep.getEntryCount()==0){
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return null;
			}
						
			return rep;
		}else{
			return super.representTable(table, mt, params,cp);
		}
	}
	
	protected Representation handleSingleCatalog(MediaType mt) throws ElementNotFoundException{
		File f=null;
		XFTTable table = new XFTTable();
		
		String[] headers=CatalogUtils.FILE_HEADERS.clone();
		String locator="URI";
		if (this.getQueryVariable("locator")!=null) {
			if (this.getQueryVariable("locator").equalsIgnoreCase("absolutePath")) {
				locator="absolutePath";
				headers[ArrayUtils.indexOf(headers,"URI")]=locator;
			} else if (this.getQueryVariable("locator").equalsIgnoreCase("projectPath")) {
				locator="projectPath";
				headers[ArrayUtils.indexOf(headers,"URI")]=locator;
			} 
		}
		table.initTable(headers);
			    	
    	final CatalogUtils.CatEntryFilterI entryFilter=buildFilter();
    	final Integer index=(containsQueryVariable("index"))?Integer.parseInt(getQueryVariable("index")):null;
    	

		if(resource.getItem().instanceOf("xnat:resourceCatalog")){
			boolean includeRoot=false;
			if(this.getQueryVariable("includeRootPath")!=null){
				includeRoot=true;
			}
			
			XnatResourcecatalog catResource = (XnatResourcecatalog)resource;
			CatCatalogBean cat= catResource.getCleanCatalog(proj.getRootArchivePath(),includeRoot,null,null);
			String parentPath=catResource.getCatalogFile(proj.getRootArchivePath()).getParent();
				    					    
			if(StringUtils.isEmpty(filepath) && index==null){
		    	String baseURI=this.getBaseURI();
		    	
		    	if(cat!=null){
		    		table.rows().addAll(CatalogUtils.getEntryDetails(cat, parentPath,baseURI + "/resources/" + catResource.getXnatAbstractresourceId() + "/files",catResource,catResource.getTagString(),false,entryFilter,proj,locator));
		    	}
			}else{

				String zipEntry=null;
				
				CatEntryI entry;
				if(index!=null){
					entry =CatalogUtils.getEntryByFilter(cat, new CatEntryFilterI(){
						private int count=0;
						private CatEntryFilterI filter = entryFilter;
						
						public boolean accept(CatEntryI entry) {
							if(filter.accept(entry)){
								if(index.equals(count++)){
									return true;
								}
							}
							
							return false;
						}
						
					});
				}else{
					String lowercase=this.filepath.toLowerCase();
				
					for(String s: zipExtensions){
						if(lowercase.contains(s)&&!lowercase.endsWith(s)){
							zipEntry=this.filepath.substring(lowercase.indexOf(s)+s.length());
							this.filepath=this.filepath.substring(0,lowercase.indexOf(s)+s.length());
							if(zipEntry.startsWith("!"))zipEntry=zipEntry.substring(1);
							if(zipEntry.startsWith("/"))zipEntry=zipEntry.substring(1);
							break;
						}
					}
					entry = CatalogUtils.getEntryByURI(cat, this.filepath);
					
					if(entry==null){
						entry = CatalogUtils.getEntryById(cat, this.filepath);
					}
				}
							
							
							
				if(entry==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find catalog entry for given uri.");
								
					return new StringRepresentation("");
				}else{
					if(FileUtils.IsAbsolutePath(entry.getUri())){
						f = new File(entry.getUri());
					}else{
						f = new File(parentPath,entry.getUri());
					}
					
					if(f.exists()){
						String fName;
						if(zipEntry==null){
							fName=f.getName().toLowerCase();
						}else{
							fName=zipEntry.toLowerCase();
						}
							
						if (mt.equals(MediaType.IMAGE_JPEG) && Dcm2Jpg.isDicom(f)) {
							try {
								byte[] jpeg = Dcm2Jpg.convert(f);
								ByteArrayInputStream bais = new ByteArrayInputStream(jpeg);
								return new InputRepresentation(bais, mt);
							}
							catch (IOException e) {
								this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to convert this file to jpeg : " + e.getMessage());
								return new StringRepresentation("");
							}
						}
						
						mt=buildMediaType(mt,fName);
						
						if(zipEntry!=null){
							try {
								ZipFile zF=new ZipFile(f);
								ZipEntry zE=zF.getEntry(zipEntry);
								if(zE==null){
									this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
									return new StringRepresentation("");
								}else{
									return new InputRepresentation(zF.getInputStream(zE),mt);
								}
							} catch (ZipException e) {
								this.getResponse().setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,e.getMessage());
								return new StringRepresentation("");
							} catch (IOException e) {
								this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,e.getMessage());
								return new StringRepresentation("");
							}
						}
						
						else{
							return this.getFileRepresentation(f,mt);
						}
						
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
						return new StringRepresentation("");
					}
				}
			}
		}else{
			if(filepath==null|| filepath.equals("")){
			    String baseURI=this.getBaseURI();
			    if(entryFilter==null){
			    	ArrayList<File> files = this.resource.getCorrespondingFiles(proj.getRootArchivePath());
					for(File subFile:files){
						Object[] row = new Object[13];
			            row[0]=(subFile.getName());
			            row[1]=(subFile.length());
			            if (locator.equalsIgnoreCase("URI")) {
			            	row[2]=baseURI + "/resources/" + resource.getXnatAbstractresourceId() + "/files/" + subFile.getName();
			            } else if (locator.equalsIgnoreCase("absolutePath")) {
			                row[2]=subFile.getAbsolutePath();
			            } else if (locator.equalsIgnoreCase("projectPath")) {
			                row[2]=subFile.getAbsolutePath().substring(proj.getRootArchivePath().substring(0,proj.getRootArchivePath().lastIndexOf(proj.getId())).length());
			            } 
			            row[3]=this.resource.getLabel();
			            row[4]=this.resource.getTagString();
			            row[5]=this.resource.getFormat();
			            row[6]=this.resource.getContent();
			            row[7]=this.resource.getXnatAbstractresourceId();
			            table.rows().add(row);
					}
			    }
			}else{
				ArrayList<File> files = this.resource.getCorrespondingFiles(proj.getRootArchivePath());
				for(File subFile:files){
					if(subFile.getName().equals(filepath)){
						f=subFile;
						break;
					}
				}
				
				if(f!=null && f.exists()){
					return this.getFileRepresentation(f,mt);
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
					return new StringRepresentation("");
				}
			}
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Files");
		
		Map<String,Map<String,String>> cp = new Hashtable<String,Map<String,String>>();
		cp.put("URI", new Hashtable<String,String>());
		cp.get("URI").put("serverRoot", getContextPath());

		return this.representTable(table, mt, params,cp,this.getSessionMaps());
	}
	
	private Map<String,String> getSessionMaps(){
		Map<String,String> session_ids=new Hashtable<String,String>();
		if(assesseds.size()>0){
//IOWA customization: to inlcude project and subject in path
            boolean projectIncludedInPath = this.isQueryVariableTrue("projectIncludedInPath");
            boolean subjectIncludedInPath = this.isQueryVariableTrue("subjectIncludedInPath");
			for(XnatExperimentdata session:assesseds){
                String replacing = session.getArchiveDirectoryName();
                if(subjectIncludedInPath) {
                     if(session instanceof XnatImagesessiondata){
                         XnatSubjectdata subject = XnatSubjectdata.getXnatSubjectdatasById(((XnatImagesessiondata)session).getSubjectId(), user, false);
                         replacing = subject.getLabel() + "/" + replacing;
                     }
                }
                if(projectIncludedInPath) {
                     replacing = session.getProject() + "/" + replacing;
                }
                session_ids.put(session.getId(),replacing);
                //session_ids.put(session.getId(),session.getArchiveDirectoryName());
			}
		}else if(expts.size()>0){
			for(XnatExperimentdata session:expts){
				session_ids.put(session.getId(),session.getArchiveDirectoryName());
			}
		}else if(sub!=null){
			session_ids.put(sub.getId(),sub.getArchiveDirectoryName());
		}else if(proj!=null){
			session_ids.put(proj.getId(),proj.getId());
		}
		
		return session_ids;
	}
	
	private ArrayList<String> getSessionIds(){
		ArrayList<String> session_ids=new ArrayList<String>();
		if(assesseds.size()>0){
			for(XnatExperimentdata session:assesseds){
				session_ids.add(session.getArchiveDirectoryName());
			}
		}else if(expts.size()>0){
			for(XnatExperimentdata session:expts){
				session_ids.add(session.getArchiveDirectoryName());
			}
		}else if(sub!=null){
			session_ids.add(sub.getArchiveDirectoryName());
		}else if(proj!=null){
			session_ids.add(proj.getId());
		}
		
		return session_ids;
	}
	
	public CatEntryFilterI buildFilter(){
		final String[] file_content=getQueryVariables("file_content");
		final String[] file_format=getQueryVariables("file_format");
		if((file_content!=null && file_content.length>0) || (file_format!=null && file_format.length>0)){
			return new CatEntryFilterI(){
				public boolean accept(CatEntryI entry) {
					if(file_format!=null && file_format.length>0){
						if(entry.getFormat()==null){
							if(!ArrayUtils.contains(file_format,"NULL"))return false;
						}else{
							if(!ArrayUtils.contains(file_format,entry.getFormat()))return false;
						}
					}

					if(file_content!=null && file_content.length>0){
						if(entry.getContent()==null){
							if(!ArrayUtils.contains(file_content,"NULL"))return false;
						}else{
							if(!ArrayUtils.contains(file_content,entry.getContent()))return false;
						}
					}
					
					return true;
				}
			};
		}
		
		return null;
	}
	
	protected Representation handleMultipleCatalogs(MediaType mt) throws ElementNotFoundException{
		final boolean isZip=isZIPRequest(mt);
    	
			    	File f = null;
		final XFTTable table = new XFTTable();
		
		String[] headers;
		if(isZip)
			headers=CatalogUtils.FILE_HEADERS_W_FILE.clone();
		else
			headers=CatalogUtils.FILE_HEADERS.clone();
		
		String locator="URI";
		// NOTE:  zip representations must have URI
		if (!isZip && this.getQueryVariable("locator")!=null) {
			if (this.getQueryVariable("locator").equalsIgnoreCase("absolutePath")) {
				locator="absolutePath";
				headers[ArrayUtils.indexOf(headers,"URI")]=locator;
			} else if (this.getQueryVariable("locator").equalsIgnoreCase("projectPath")) {
				locator="projectPath";
				headers[ArrayUtils.indexOf(headers,"URI")]=locator;
			}
		}
		table.initTable(headers);

    	final String baseURI=this.getBaseURI();
			    	
    	final CatEntryFilterI entryFilter=buildFilter();
    	
    	final Integer index=(containsQueryVariable("index"))?Integer.parseInt(getQueryVariable("index")):null;

		for(final XnatAbstractresource temp: resources){			
			if(temp.getItem().instanceOf("xnat:resourceCatalog")){
				final boolean includeRoot=(this.getQueryVariable("includeRootPath")!=null);
							
				final XnatResourcecatalog catResource = (XnatResourcecatalog)temp;
				
				
				final CatCatalogBean cat= catResource.getCleanCatalog(proj.getRootArchivePath(),includeRoot,null,null);
		    	final String parentPath=catResource.getCatalogFile(proj.getRootArchivePath()).getParent();
			    	
		    	if(cat!=null){
			    	if(filepath==null|| filepath.equals("")){
				    	
				    	if(cat!=null){
				    		table.rows().addAll(CatalogUtils.getEntryDetails(cat, parentPath,(catResource.getBaseURI()!=null)?catResource.getBaseURI() + "/files":baseURI + "/resources/" + catResource.getXnatAbstractresourceId() + "/files",catResource,catResource.getTagString(),isZip || (index!=null),entryFilter,proj,locator));
				    	}
					}else{
						CatEntryI entry = CatalogUtils.getEntryByURI(cat, filepath);
						
						if(entry==null){
							entry = CatalogUtils.getEntryById(cat, filepath);
						}
						
						if(entry!=null){
							if(FileUtils.IsAbsolutePath(entry.getUri())){
								f = new File(entry.getUri());
							}else{
								f = new File(parentPath,entry.getUri());
							}
							
							if(f.exists())break;
						}
					}
				}
			}else{
				//not catalog
				if(entryFilter==null){
    				ArrayList<File> files =temp.getCorrespondingFiles(proj.getRootArchivePath());
    				for(File subFile:files){
    					Object[] row = new Object[(isZip)?9:8];
    					row[0]=(subFile.getName());
    		            row[1]=(subFile.length());
			            if (locator.equalsIgnoreCase("URI")) {
			            	row[2]=(temp.getBaseURI()!=null)?temp.getBaseURI() + "/files/" + subFile.getName():baseURI + "/resources/" + temp.getXnatAbstractresourceId() + "/files/" + subFile.getName();
			            } else if (locator.equalsIgnoreCase("absolutePath")) {
			                row[2]=subFile.getAbsolutePath();
			            } else if (locator.equalsIgnoreCase("projectPath")) {
			                row[2]=subFile.getAbsolutePath().substring(proj.getRootArchivePath().substring(0,proj.getRootArchivePath().lastIndexOf(proj.getId())).length());
			            } 
    		            row[3]=temp.getLabel();
    		            row[4]=temp.getTagString();
    		            row[5]=temp.getFormat();
    		            row[6]=temp.getContent();
    		            row[7]=temp.getXnatAbstractresourceId();
    		            if(isZip)row[8]=subFile;
    		            table.rows().add(row);
    				}
				}
			}
		}
		
		String downloadName;
		if(security !=null){
			downloadName=((ArchivableItem)security).getArchiveDirectoryName();
		}else {
			downloadName=this.getSessionMaps().get(0);
		}
		
		if(mt.equals(MediaType.APPLICATION_ZIP)){
			this.setContentDisposition(downloadName + ".zip");
 		}else if(mt.equals(MediaType.APPLICATION_GNU_TAR)){
			this.setContentDisposition(downloadName + ".tar.gz");
 		}else if(mt.equals(MediaType.APPLICATION_TAR)){
			this.setContentDisposition(downloadName + ".tar");
		}
					
		if(StringUtils.isEmpty(filepath) && index==null){
			Hashtable<String,Object> params=new Hashtable<String,Object>();
			params.put("title", "Files");
			
			Map<String,Map<String,String>> cp = new Hashtable<String,Map<String,String>>();
			cp.put("URI", new Hashtable<String,String>());
			String rootPath = this.getRequest().getRootRef().getPath();
			if(rootPath.endsWith("/data")){
				rootPath=rootPath.substring(0,rootPath.indexOf("/data"));
			}
			if(rootPath.endsWith("/REST")){
				rootPath=rootPath.substring(0,rootPath.indexOf("/REST"));
			}
			cp.get("URI").put("serverRoot", rootPath);

			return this.representTable(table, mt, params,cp,this.getSessionMaps());
		}else{
			if(index!=null&& table.rows().size()>index){
				f=(File)table.rows().get(index)[8];
			}
						
			//return file
			if(f!=null){
				if(f!=null && f.exists()){
					if((mt.equals(MediaType.APPLICATION_ZIP) && !f.getName().toLowerCase().endsWith(".zip")) 
							|| (mt.equals(MediaType.APPLICATION_GNU_TAR) && !f.getName().toLowerCase().endsWith(".tar.gz") )
							|| (mt.equals(MediaType.APPLICATION_TAR) && !f.getName().toLowerCase().endsWith(".tar"))){
						final ZipRepresentation rep;
						try{
							rep=new ZipRepresentation(mt,((ArchivableItem)security).getArchiveDirectoryName(),identifyCompression(null));
						} catch (ActionException e) {
							logger.error("",e);
							this.setResponseStatus(e);
							return null;
						}
						rep.addEntry(f.getName(),f);
						return rep;
					}else{
						return this.getFileRepresentation(f,mt);
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
					return null;
				}
			}else{
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find file.");
				return null;
			}
		}
	}
	
	private FileRepresentation getFileRepresentation(File f, MediaType mt) {
		FileRepresentation fr = null;
		try {
			fr = this.setFileRepresentation(f, mt);
		}
		catch (IOException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Unable to return file as " + mt.getName());
		}
		return fr;	
	}
	
	private FileRepresentation setFileRepresentation(File f, MediaType mt) throws IOException {
		this.setResponseHeader("Cache-Control", "must-revalidate");
		return representFile(f,mt);
	}
	
}
