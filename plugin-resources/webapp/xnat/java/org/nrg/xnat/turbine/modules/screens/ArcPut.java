//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 25, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.modules.screens.RawScreen;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatCatalogTagI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.FailedLoginException;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
public class ArcPut extends RawScreen {
    static org.apache.log4j.Logger logger = Logger.getLogger(ArcPut.class);
    @Override
    protected void doOutput(RunData data) throws Exception {
        String session = data.getParameters().getString("session");
        String mr_session_id = data.getParameters().getString("mr_session_id");
        HttpServletResponse response = data.getResponse();
        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        PrintWriter pw = response.getWriter();
        try {
            if (session==null){
                pw.println("<UploadResponse status=\"ERROR\" CODE=\"100\">");
                pw.println("<message>Unspecified User Session</message>");
                pw.println("</UploadResponse>");
                return;
            }
            XDATUser user = TurbineUtils.getUser(data);
            if (user==null){
                pw.println("<UploadResponse status=\"ERROR\" CODE=\"103\">");
                pw.println("<message>User Session Missing</message>");
                pw.println("</UploadResponse>");
                return;
            }
            XnatMrsessiondata tempMR =XnatMrsessiondata.getXnatMrsessiondatasById(mr_session_id, user, false);
            if(tempMR==null){
            	ArrayList<XnatMrsessiondata> al=XnatMrsessiondata.getXnatMrsessiondatasByField("xnat:mrSessionData/label",mr_session_id, user, false);
            	if(al.size()>0){
            		tempMR=al.get(0);
            	}
            }
            
            if (tempMR==null){
                pw.println("<UploadResponse status=\"ERROR\" CODE=\"101\">");
                pw.println("<message>Unknown MRSession ID: " + mr_session_id + "</message>");
                pw.println("</UploadResponse>");
                return;
            }
            ParameterParser params = data.getParameters();
            FileItem fi = params.getFileItem("archive");
            if (fi != null )
            {
                String filename = fi.getName();
                int index = filename.lastIndexOf('\\');
                if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
                if(index>0)filename = filename.substring(index+1);
                String cachepath= ArcSpecManager.GetInstance().getGlobalCachePath();
                cachepath+="user_uploads/"+user.getXdatUserId() + "/" + session + "/";
                File destination = new File(cachepath);
                if(!destination.exists()){
                    destination.mkdirs();
                }
                String compression_method = ".zip";
                if (filename.indexOf(".")!=-1){
                    compression_method = filename.substring(filename.lastIndexOf("."));
                }                   
                if (compression_method.equalsIgnoreCase(".tar") || 
                        compression_method.equalsIgnoreCase(".gz") || 
                        compression_method.equalsIgnoreCase(".zip") || 
                        compression_method.equalsIgnoreCase(".zar"))
                {
                    InputStream is = fi.getInputStream();
                    System.out.println("Extracting file.");
                    ZipI zipper = null;
                    if (compression_method.equalsIgnoreCase(".tar")){
                        zipper = new TarUtils();
                    }else if (compression_method.equalsIgnoreCase(".gz")){
                        zipper = new TarUtils();
                        zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
                    }else{
                        zipper = new ZipUtils();
                    }
                    try {
                        zipper.extract(is,cachepath);
                    } catch (Throwable e1) {
                        pw.println("<UploadResponse status=\"ERROR\" CODE=\"102\">");
                        pw.println("<message>" + e1.getMessage() + "</message>");
                        pw.println("</UploadResponse>");
                        return;
                    }
                }else{
                    //PLACE UPLOADED IMAGE INTO FOLDER
                    File uploaded = new File(cachepath + filename) ;
                    fi.write(uploaded);
                }
                fi.delete();
                int counter=0; 
                Date d = Calendar.getInstance().getTime();
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_hhmmss");
                String uploadID = formatter.format(d);
                String arcPath= tempMR.getRelativeArchivePath();
                arcPath= FileUtils.AppendSlash(arcPath);
                String destinationPath=arcPath + "/UPLOADS/" + uploadID + "/" ;
                File[] listFiles = destination.listFiles();
                for (int i=0;i<listFiles.length;i++)
                {
                    if(!listFiles[i].isDirectory())
                    {
                        if (listFiles[i].getName().endsWith(".xml") || listFiles[i].getName().endsWith(".xcat"))
                        {
                            File xml = listFiles[i];
                            if (xml.exists())
                            {
                                FileInputStream fis = new FileInputStream(xml);
                                XDATXMLReader reader = new XDATXMLReader();
                                try {
                                    BaseElement base = reader.parse(fis);
                                    if (base instanceof CatCatalogBean){
                                        CatCatalogI cBean=(CatCatalogBean)base;
                                        XnatResourcecatalog cat = new XnatResourcecatalog((UserI)TurbineUtils.getUser(data));
                                        if (cBean.getId()!=null){
                                            cat.setLabel(cBean.getId());
                                        }else{
                                            cat.setLabel(Calendar.getInstance().getTime().toString());
                                        }
                                        for(CatCatalogTagI tag: cBean.getTags_tag()){
                                        	XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)TurbineUtils.getUser(data));
                                            t.setTag(tag.getTag());
                                            cat.setTags_tag(t);
                                        }
                                        cat.setUri(destinationPath + xml.getName());
                                        tempMR.setResources_resource(cat);
                                        counter++;
                                    }
                                } catch (Throwable e) {
                                    logger.error("",e);
                                }
                            }
                        }
                    }else{
                        for (int j=0;j<listFiles[i].listFiles().length;j++)
                        {
                            if(!listFiles[i].listFiles()[j].isDirectory())
                            {
                                if (listFiles[i].listFiles()[j].getName().endsWith(".xml") || listFiles[i].getName().endsWith(".xcat"))
                                {
                                    File xml = listFiles[i].listFiles()[j];
                                    if (xml.exists())
                                    {
                                        FileInputStream fis = new FileInputStream(xml);
                                        XDATXMLReader reader = new XDATXMLReader();
                                        try {
                                            BaseElement base = reader.parse(fis);
                                            if (base instanceof CatCatalogBean){
                                                CatCatalogI cBean=(CatCatalogBean)base;
                                                XnatResourcecatalog cat = new XnatResourcecatalog((UserI)TurbineUtils.getUser(data));
                                                if (cBean.getId()!=null){
                                                    cat.setLabel(cBean.getId());
                                                }else{
                                                    cat.setLabel(Calendar.getInstance().getTime().toString());
                                                }
                                                for(CatCatalogTagI tag: cBean.getTags_tag()){
                                                	XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)TurbineUtils.getUser(data));
                                                    t.setTag(tag.getTag());
                                                    cat.setTags_tag(t);
                                                }
                                                cat.setUri(destinationPath + listFiles[i].getName() + "/" + xml.getName());
                                                tempMR.setResources_resource(cat);
                                                counter++;
                                            }
                                        } catch (Throwable e) {
                                            logger.error("",e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (counter>0){
                	PersistentWorkflowI workflow =WorkflowUtils.getOrCreateWorkflowData(null, user, tempMR.getXSIType(), tempMR.getId(), tempMR.getProject(),SecureAction.newEventInstance(data, EventUtils.CATEGORY.DATA, EventUtils.ARCPUT));
                	EventMetaI ci=workflow.buildEvent();
                	
                    File dest = new File(FileUtils.AppendRootPath(tempMR.getArchiveRootPath(),destinationPath));
                    FileUtils.MoveDir(destination, dest, true);
                    FileUtils.DeleteFile(destination);
                    try {
                    	PersistentWorkflowUtils.save(workflow,ci);
                    	
                        tempMR.save(user,false,false,ci);
                        data.setMessage("Files successfully uploaded.");
                        PersistentWorkflowUtils.complete(workflow,ci);
                    } catch (Exception e) {
                        PersistentWorkflowUtils.fail(workflow,ci);
                    	logger.error("",e);
                        pw.println("<UploadResponse status=\"ERROR\" CODE=\"104\">");
                        pw.println("<message>Error updating MR Database Entries.</message>");
                        pw.println("</UploadResponse>");
                        return;
                    }
                }else{
                    pw.println("<UploadResponse status=\"ERROR\" CODE=\"104\">");
                    pw.println("<message>Missing catalog xml.</message>");
                    pw.println("</UploadResponse>");
                    return;
                }
                pw.println("<UploadResponse status=\"COMPLETE\" CODE=\"0\">");
                pw.println("<message>Upload Complete</message>");
                pw.println("</UploadResponse>");
                return;
            }
        } catch (FileNotFoundException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"105\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (IOException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"106\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (XFTInitException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"107\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (ElementNotFoundException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"108\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (DBPoolException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"109\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (SQLException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"110\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (FieldNotFoundException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"111\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (FailedLoginException e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"112\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        } catch (Exception e) {
            logger.error(e);
            pw.println("<UploadResponse status=\"ERROR\" CODE=\"113\">");
            pw.println("<message>" + e.getMessage() + "</message>");
            pw.println("</UploadResponse>");
            return;
        }
    }
    @Override
    protected String getContentType(RunData data) {
        return "text/xml";
    }
}
