//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 12, 2006
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ImageUploadHelper;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author timo
 *
 */
public class ImageUpload extends SecureAction {
    static org.apache.log4j.Logger logger = Logger.getLogger(ImageUpload.class);
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */    
    public void doPerform(RunData data, Context context) throws Exception {
//    	ParameterParser params = data.getParameters();
//        HttpSession session = data.getSession();
//        String uploadID= null;
//        if (params.get("ID")!=null && !params.get("ID").equals("")){
//            uploadID=params.get("ID");
//            session.setAttribute(uploadID + "Upload", new Integer(0));
//            session.setAttribute(uploadID + "Extract", new Integer(0));
//            session.setAttribute(uploadID + "Analyze", new Integer(0));
//        }
//
//        Date d = Calendar.getInstance().getTime();
//        
//        StringBuffer sb = new StringBuffer();
//        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
//        String timestamp=formatter.format(d);
//        File log = new File(org.nrg.xdat.turbine.utils.AccessLogger.getAccessLogDirectory() + "zip_uploads" + File.separator + TurbineUtils.getUser(data).getUsername() + "_" +timestamp + ".log");
//    	log.getParentFile().mkdirs();
//
//    	PrintStream upload_log=new PrintStream(new FileOutputStream(log));
//    	upload_log.println("Starting file upload.");
//    	
//        if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Integer(0));
//        try {
//        	
//            //byte[] bytes = params.getUploadData();
//            //grab the FileItems available in ParameterParser
//            FileItem fi = params.getFileItem("image_archive");
//            String project = params.getString("project");
//            if (fi != null)
//            {
//                String filename = fi.getName();
//                upload_log.println("fileName:" + filename);
//                upload_log.println("project:" + project);
//                
//                ArcArchivespecification spec=ArcSpecManager.GetInstance();
//                
//                String prearchive_path = spec.getPrearchivePathForProject(project);
//                String cachepath= spec.getCachePathForProject(project);
//                                
//                //BUILD PREARCHIVE PATH
//                sb.append(timestamp);
//                
//                prearchive_path +=sb.toString() + File.separator;
//                File prearcDIR = new File(prearchive_path);
//                
//
//                upload_log.println("mkdir " + prearchive_path);
//                if (!prearcDIR.exists()){
//                    prearcDIR.mkdirs();
//                }
//                
//                //BUILD TEMPORARY PATH
//                XDATUser user = TurbineUtils.getUser(data);
//                
//                cachepath += "uploads" + File.separator + user.getXdatUserId() + File.separator + sb.toString() + File.separator;
//                File cacheDIR = new File(cachepath);
//                if (!cacheDIR.exists()){
//                	cacheDIR.mkdirs();
//                	upload_log.println("mkdir " + cachepath);
//                }
//                
//                
//                //upload file
//                if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Integer(100));
//                
//                int index = filename.lastIndexOf('\\');
//                if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
//                if(index>0)filename = filename.substring(index+1);
//                File uploaded = new File(cachepath + filename) ;
//                upload_log.print("Uploading to "+uploaded.getAbsolutePath() + " ... ");
//                fi.write(uploaded);
//                
//                
//                upload_log.println("done.");
//
//                try {
//                    final ImageUploadHelper helper = new ImageUploadHelper(uploadID,session,project);
//                                       
//                    upload_log.println("Prearchive:" + prearcDIR.getAbsolutePath());
//                    HelperResults results=helper.run(cacheDIR, prearcDIR);
//                    
//                    for(String[] s: results.listener.getMessages()){
//                        upload_log.println(s[0]+ ":" + s[1]);
//                    }
//                    
//                    boolean autoArchive=false;
//                    boolean quarantine=false;
//                    
//                    String prearchive_code = params.getString("prearchive_code");
//                    if(prearchive_code!=null && prearchive_code.equals("1")){
//                    	autoArchive=true;
//                        upload_log.println("Option:AUTO ARCHIVE w NO Quarantine");
//                    }else if(prearchive_code!=null && prearchive_code.equals("2")){
//                    	autoArchive=true;
//                    	quarantine=true;
//                        upload_log.println("Option:AUTO ARCHIVE w Quarantine");
//                    }else{
//                        upload_log.println("Option:PRE-ARCHIVE");
//                    }
//                    
//                    if(autoArchive){
//                    	//auto-archive
//                    	String html="The following sessions were uploaded:<br><br>";
//                    	for(File folder: results.sessions){
//                        	LoadImageData loader = new LoadImageData();
//                        	
//                    		File xml=new File(folder.getAbsolutePath() + ".xml");
//                    		
//                    		if(xml.exists()){
//                            	try {
//									XnatImagesessiondata mr=loader.getSession(TurbineUtils.getUser(data), xml, project, false);
//									
//									XnatSubjectdata subj=mr.getSubjectData();
//									
//									if(subj==null  && XNATUtils.hasValue(mr.getSubjectId())){
//										String cleaned=XnatSubjectdata.cleanValue(mr.getSubjectId());
//										if(!cleaned.equals(mr.getSubjectId())){
//											mr.setSubjectId(cleaned);
//											subj=mr.getSubjectData();
//										}
//									}
//									
//									if(subj==null){
//				                        upload_log.println("PROCESSING:Creating unmatched subject '" + mr.getSubjectId() +"'");
//										results.listener.addMessage("PROCESSING", "Creating unmatched subject '" + mr.getSubjectId() +"'");
//										XnatSubjectdata sub=new XnatSubjectdata((UserI)user);
//										sub.setProject(project);
//										if(XNATUtils.hasValue(mr.getSubjectId())){
//											sub.setLabel(XnatSubjectdata.cleanValue(mr.getSubjectId()));
//										}
//										sub.setId(XnatSubjectdata.CreateNewID());
//										sub.save(user, false, false);
//										
//										mr.setSubjectId(sub.getId());
//									}else{
//										upload_log.println("PROCESSING:Matched existing subject '" + mr.getSubjectId() +"'");
//									}
//									
//									if(!XNATUtils.hasValue(mr.getLabel())){
//										if(mr.getDcmpatientid()!=null && !mr.getDcmpatientid().equals("") && !mr.getDcmpatientid().equals("NULL")){
//											mr.setLabel(mr.getDcmpatientid());
//										}
//									}
//									
//									if(!XNATUtils.hasValue(mr.getLabel())){
//										results.listener.addMessage("ERROR", "Unable to identify appropriate session ID.");
//										upload_log.println("ERROR:Unable to identify appropriate session ID, left in prearchive");
//										html +="<a style='font-color:red' style='font-color:green' target='_parent' href='" + TurbineUtils.GetRelativeServerPath(data) + "/app/template/XDATScreen_prearchives.vm/project/" + mr.getProject() + "'>" + mr.getLabel() + "</a> Placed in prearchive, due to inability to identify an appropriate session ID.<br>";
//										continue;
//									}else{
//										mr.setLabel(XnatMrsessiondata.cleanValue(mr.getLabel()));
//									}
//									
//									if(quarantine){
//										data.getParameters().setString("quarantine", "true");
//									}
//									final StoreImageSession store=new StoreImageSession();
//									
//									final String arcSessionPath=store.getArcSessionPath(mr);
//									
//									
//									boolean hasFiles=false;
//									final File session_dir=new File(arcSessionPath);
//									if(session_dir.exists()){
//										File scans=new File(session_dir,"RAW");
//										if(!scans.exists()){
//											scans=new File(session_dir,"SCANS");
//										}
//										if(scans.exists()){
//											if(FileUtils.HasFiles(scans)){
//												hasFiles=true;
//											}
//										}
//									}
//									
//									if(hasFiles){
//										results.listener.addMessage("ERROR", "Archive folder already exists '" + mr.getLabel() +"'");
//										upload_log.println("ERROR:Archive folder already exists '" + mr.getLabel() +"', left in prearchive");
//										html +="<a style='font-color:red' style='font-color:green' target='_parent' href='" + TurbineUtils.GetRelativeServerPath(data) + "/app/template/XDATScreen_prearchives.vm/project/" + mr.getProject() + "'>" + mr.getLabel() + "</a> Placed in prearchive, due to pre-existing archive folder.<br>";
//									}else{
//										store.template=mr;
//										results.listener.addMessage("PROCESSING", "Auto-archiving '" + mr.getLabel() +"'");
//										upload_log.println("PROCESSING:Auto-archiving '" + mr.getLabel() +"'");
//										if(store.process(data, context)!=null){
//											html +="<a style='font-color:green' target='_parent' href='" + TurbineUtils.GetRelativeServerPath(data) + "/app/action/DisplayItemAction/search_element/" + mr.getXSIType() + "/search_field/" + mr.getXSIType() + ".ID/search_value/" + mr.getId() + "/project/" + mr.getProject() + "'>" + mr.getLabel() + "</a> successfully archived.<br>";
//										}
//									}
//									
//								} catch (Throwable e) {
//									html +="Error storing " + folder.getName() + "<br>";
//									upload_log.println("ERROR:Error storing '" + folder.getName() +"'");
//									e.printStackTrace(upload_log);
//									upload_log.println("");
//									
//									logger.error("Error storing " + folder.getName(), e);
//								}
//                    		}
//                    	}
//
//                        if (uploadID!=null)session.setAttribute(uploadID + "Extract", new Integer(100));
//                        if (uploadID!=null)session.setAttribute(uploadID + "Analyze", new Integer(100));
//
//                        results.listener.addMessage("COMPLETED", "Process complete.");
//                        upload_log.println("COMPLETED:Process complete.");
//						
//                        data.getParameters().add("project",project);
//                        context.put("project", project);
//                        data.setMessage(html);
//                        data.setScreenTemplate("JS_Return_Message.vm");
//                    }else{
//                        if (uploadID!=null)session.setAttribute(uploadID + "Extract", new Integer(100));
//                        results.listener.addMessage("COMPLETED", "Extraction and Review complete.");
//                        upload_log.println("COMPLETED:Extraction and Review complete.");
//						//pre-archive
//                        if (uploadID!=null)session.setAttribute(uploadID + "Analyze", new Integer(100));
//                        
//                        data.getParameters().add("project",project);
//                        context.put("project", project);
//                        data.setMessage("File Uploaded.");
//                        data.setScreenTemplate("UploadSummary.vm");
//                    }
//                } catch (Throwable e) {
//                	upload_log.println("ERROR occured");
//					e.printStackTrace(upload_log);
//					upload_log.println("");
//					
//					
//                    error(e,data);
//                    session.setAttribute(uploadID + "Upload", new Integer(-1));
//                    session.setAttribute(uploadID + "Extract", new Integer(-1));
//                    session.setAttribute(uploadID + "Analyze", new Integer(-1));
//                }finally{
//
//                	upload_log.println("Deleting " + fi.getName());
//                    fi.delete();
//                    
//                    if (cacheDIR.exists()){
//                    	upload_log.println("Deleting " + cacheDIR.getAbsolutePath());
//                        FileUtils.DeleteFile(cacheDIR);
//                    }
//                    
//                    if (uploaded.exists()){
//                    	upload_log.println("Deleting " + uploaded.getAbsolutePath());
//                    	FileUtils.DeleteFile(uploaded);
//                    }
//                }
//            }
//        } catch (FileNotFoundException e) {
//        	upload_log.println("ERROR occured");
//			e.printStackTrace(upload_log);
//			upload_log.println("");
//            error(e,data);
//            session.setAttribute(uploadID + "Upload", new Integer(-1));
//            session.setAttribute(uploadID + "Extract", new Integer(-1));
//            session.setAttribute(uploadID + "Analyze", new Integer(-1));
//        } catch (IOException e) {
//        	upload_log.println("ERROR occured");
//			e.printStackTrace(upload_log);
//			upload_log.println("");
//            error(e,data);
//            session.setAttribute(uploadID + "Upload", new Integer(-1));
//            session.setAttribute(uploadID + "Extract", new Integer(-1));
//            session.setAttribute(uploadID + "Analyze", new Integer(-1));
//        } catch (RuntimeException e) {
//        	upload_log.println("ERROR occured");
//			e.printStackTrace(upload_log);
//			upload_log.println("");
//            error(e,data);
//            session.setAttribute(uploadID + "Upload", new Integer(-1));
//            session.setAttribute(uploadID + "Extract", new Integer(-1));
//            session.setAttribute(uploadID + "Analyze", new Integer(-1));
//        }finally{
//        	upload_log.close();
//        }
        }

    
}
