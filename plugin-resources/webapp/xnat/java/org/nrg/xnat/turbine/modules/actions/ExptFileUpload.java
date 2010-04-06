//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 26, 2007 
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatCatalogTagBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.xml.sax.SAXException;

public class ExptFileUpload extends SecureAction {
    static org.apache.log4j.Logger logger = Logger.getLogger(ExptFileUpload.class);

    @Override
    public void doPerform(RunData data, Context context) throws Exception{
        ItemI temp = TurbineUtils.GetItemBySearch(data,false);
        XnatImagesessiondata pet = (XnatImagesessiondata) org.nrg.xdat.base.BaseElement.GetGeneratedItem(temp);
        
        ParameterParser params = data.getParameters();
        HttpSession session = data.getSession();
        String uploadID= null;
        if (params.get("ID")!=null && !params.get("ID").equals("")){
            uploadID=params.get("ID");
            session.setAttribute(uploadID + "Upload", new Integer(0));
            session.setAttribute(uploadID + "Extract", new Integer(0));
            session.setAttribute(uploadID + "Analyze", new Integer(0));
        }
        if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Integer(0));
            try {
                //byte[] bytes = params.getUploadData();
                //grab the FileItems available in ParameterParser
                FileItem fi = params.getFileItem("image_archive");
                if (fi != null)
                { 
                    
                    String cache_path = ArcSpecManager.GetInstance().getGlobalCachePath();
                    if (!cache_path.endsWith(File.separator)){
                        cache_path += File.separator;
                    }                    
                    
                    cache_path +="user_uploads" + File.separator + uploadID + File.separator;
                    File dir = new File(cache_path);
                    
                    if (!dir.exists()){
                        dir.mkdirs();
                    }
                    System.out.println("Uploading file.");
                    //upload file

                    String filename = fi.getName();

                    int index = filename.lastIndexOf('\\');
                    if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
                    if(index>0)filename = filename.substring(index+1);
                    
                    String compression_method = ".zip";
                    if (filename.indexOf(".")!=-1){
                        compression_method = filename.substring(filename.lastIndexOf("."));
                    }                   
                    
                    if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Integer(100));
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
                            zipper.extract(is,cache_path);
                        } catch (Throwable e1) {
                            error(e1,data);
                            session.setAttribute(uploadID + "Extract", new Integer(-1));
                            session.setAttribute(uploadID + "Analyze", new Integer(-1));
                            return;
                        }
                    }else{
                        //PLACE UPLOADED IMAGE INTO FOLDER
                        File uploaded = new File(cache_path + filename) ;
                        fi.write(uploaded);
                    }
                    if (uploadID!=null)session.setAttribute(uploadID + "Extract", new Integer(100));
                    
                    
                    fi.delete();
                    

                    String tag = (String)TurbineUtils.GetPassedParameter("tags", data);
                    this.addTag(dir, tag);
                    
                    System.out.println("File Upload Complete.");
                    data.setMessage("File Uploaded.");
                    context.put("search_element",data.getParameters().getString("search_element"));
                    context.put("search_field",data.getParameters().getString("search_field"));
                    context.put("search_value",data.getParameters().getString("search_value"));
                    context.put("uploadID",uploadID);
                    context.put("destination","ExptUploadConfirm.vm");
                    data.setScreenTemplate("FileUploadSummary.vm");
                }
            } catch (FileNotFoundException e) {
                error(e,data);
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                session.setAttribute(uploadID + "Extract", new Integer(-1));
                session.setAttribute(uploadID + "Analyze", new Integer(-1));
            } catch (IOException e) {
                error(e,data);
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                session.setAttribute(uploadID + "Extract", new Integer(-1));
                session.setAttribute(uploadID + "Analyze", new Integer(-1));
            } catch (RuntimeException e) {
                error(e,data);
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                session.setAttribute(uploadID + "Extract", new Integer(-1));
                session.setAttribute(uploadID + "Analyze", new Integer(-1));
            }
    }
    
    public void addTag(File dir,String tag){
        
        CatCatalogBean cat = null;
        
        if (dir.exists())
        {
            int counter=0;
            
            File[] listFiles = dir.listFiles();
            for (int i=0;i<listFiles.length;i++)
            {
                if(!listFiles[i].isDirectory())
                {
                    if (listFiles[i].getName().endsWith(".xml"))
                    {
                        File xml = listFiles[i];
                        if (xml.exists())
                        {
                            try {
                                FileInputStream fis = new FileInputStream(xml);
                                XDATXMLReader reader = new XDATXMLReader();
                                BaseElement base = reader.parse(fis);

                                
                                if (base instanceof CatCatalogBean){
                                	cat = (CatCatalogBean)base;

                                    CatCatalogTagBean tagBean = new CatCatalogTagBean();
                                    tagBean.setTag(tag);
                                    cat.addTags_tag(tagBean);

            						FileWriter fw = new FileWriter(xml);
            						cat.toXML(fw, true);
            						fw.close();
            						
                                    counter++;
                                }
                            } catch (FileNotFoundException e) {
                                logger.error("",e);
                            } catch (IOException e) {
                                logger.error("",e);
                            } catch (SAXException e) {
                                logger.error("",e);
                            }
                        }
                    }
                }else{
                    for (int j=0;j<listFiles[i].listFiles().length;j++)
                    {
                        if(!listFiles[i].listFiles()[j].isDirectory())
                        {
                            if (listFiles[i].listFiles()[j].getName().endsWith(".xml"))
                            {
                                File xml = listFiles[i].listFiles()[j];
                                if (xml.exists())
                                {
                                    try {
                                        FileInputStream fis = new FileInputStream(xml);
                                        XDATXMLReader reader = new XDATXMLReader();
                                        BaseElement base = reader.parse(fis);

                                        
                                        if (base instanceof CatCatalogBean){
                                        	cat = (CatCatalogBean)base;
                                            counter++;
                                        }
                                    } catch (FileNotFoundException e) {
                                        logger.error("",e);
                                    } catch (IOException e) {
                                        logger.error("",e);
                                    } catch (SAXException e) {
                                        logger.error("",e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            
            if (counter==0){
            	cat = new CatCatalogBean();
            	if (dir.exists())
                {
                    for (File f: dir.listFiles())
                    {
                    	XNATUtils.populateCatalogBean(cat, "", f);
                    }
                    
                    if (tag != null){
                        CatCatalogTagBean tagBean = new CatCatalogTagBean();
                        tagBean.setTag(tag);
                        cat.addTags_tag(tagBean);
                    }
                    
                    try {
						File catF = new File(dir,"generated_catalog.xml");
						FileWriter fw = new FileWriter(catF);
						cat.toXML(fw, true);
						fw.close();
					} catch (IOException e) {
						logger.error("",e);
					}
                }
            }
        }
    }

    public void doFinalize(RunData data, Context context) throws Exception {
        ItemI temp = TurbineUtils.GetItemBySearch(data,false);
        XnatImagesessiondata tempMR = (XnatImagesessiondata) org.nrg.xdat.base.BaseElement.GetGeneratedItem(temp);
        
        String uploadID= data.getParameters().getString("uploadID");

        
        String cache_path = ArcSpecManager.GetInstance().getGlobalCachePath();
 
        
        cache_path +="user_uploads" + File.separator + uploadID + File.separator;
        File dir = new File(cache_path);
        if (dir.exists())
        {
            int counter=0; 

            String arcPath= tempMR.getRelativeArchivePath();
            arcPath= FileUtils.AppendSlash(arcPath);
            String destinationPath=arcPath + "/UPLOADS/" + uploadID + "/" ;
            
            File[] listFiles = dir.listFiles();
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
                                    CatCatalogBean cBean=(CatCatalogBean)base;
                                    XnatResourcecatalog cat = new XnatResourcecatalog((UserI)TurbineUtils.getUser(data));
                                    
                                    if (cBean.getId()!=null){
                                        cat.setLabel(cBean.getId());
                                    }else{
                                        cat.setLabel(Calendar.getInstance().getTime().toString());
                                    }
                                    
                                    for(CatCatalogTagBean tag: cBean.getTags_tag()){
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
                                            CatCatalogBean cBean=(CatCatalogBean)base;
                                            XnatResourcecatalog cat = new XnatResourcecatalog((UserI)TurbineUtils.getUser(data));
                                            
                                            if (cBean.getId()!=null){
                                                cat.setLabel(cBean.getId());
                                            }else{
                                                cat.setLabel(Calendar.getInstance().getTime().toString());
                                            }
                                            
                                            for(CatCatalogTagBean tag: cBean.getTags_tag()){
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
                File dest = new File(FileUtils.AppendRootPath(tempMR.getArchiveRootPath(),destinationPath));
                FileUtils.MoveDir(dir, dest, true);
                FileUtils.DeleteFile(dir);
                
                try {
                    tempMR.save(TurbineUtils.getUser(data),false,false);
                    data.setMessage("Files successfully uploaded.");
                } catch (Exception e) {
                    error(e,data);
                }
                
                if (tempMR.getProject()!=null){
                    data.getParameters().setString("project", tempMR.getProject());
                }
                
                if (TurbineUtils.HasPassedParameter("destination", data)){
                    this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data), tempMR.getItem(), data);
                }else{
                    this.redirectToReportScreen(tempMR.getItem(), data);
                }
            }
        }
    }
}
