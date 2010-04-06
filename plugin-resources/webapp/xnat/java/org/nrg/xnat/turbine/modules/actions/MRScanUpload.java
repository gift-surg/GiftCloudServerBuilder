//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Mar 6, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.PrearcImporter;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.modules.actions.ModifyItem.CriticalException;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ImageUploadHelper;
import org.nrg.xnat.turbine.utils.XNATSessionPopulater;

public class MRScanUpload extends StoreImageSession {

    static org.apache.log4j.Logger logger = Logger.getLogger(MRScanUpload.class);
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception{
        ItemI temp = TurbineUtils.GetItemBySearch(data,false);
        XnatMrsessiondata pet = new XnatMrsessiondata(temp);
        
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
                    String filename = fi.getName();
                    
                    String cache_path = ArcSpecManager.GetInstance().getGlobalCachePath();
                    if (!cache_path.endsWith(File.separator)){
                        cache_path += File.separator;
                    }                    
                    
                    cache_path +="uploads" + File.separator + uploadID + File.separator;
                    File cacheDir = new File(cache_path);
                    
                    if (!cacheDir.exists()){
                        cacheDir.mkdirs();
                    }
                    
                    UserI user = TurbineUtils.getUser(data);

                    String temppath= ArcSpecManager.GetInstance().getGlobalCachePath();
                    
                    temppath += "uploads" + File.separator + "temp" + File.separator + user.getUsername() + File.separator;
                    File tempDIR = new File(temppath);
                    if (!tempDIR.exists())tempDIR.mkdirs();
                    
                    System.out.println("Uploading file.");
                    //upload file
                    
                    if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Integer(100));

                    int index = filename.lastIndexOf('\\');
                    if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
                    if(index>0)filename = filename.substring(index+1);
                    File uploaded = new File(temppath + filename) ;
                    fi.write(uploaded);

                    
                    System.out.println("File Upload Complete.");
                    
                    try {
                	final String project = params.get("project");
                        final ImageUploadHelper helper = new ImageUploadHelper(uploadID,session, project);
                        helper.run(tempDIR, cacheDir);

                        if (uploadID!=null) {
                            session.setAttribute(uploadID + "Extract", new Integer(100));
                            session.setAttribute(uploadID + "Analyze", new Integer(100));
                        }
                        
                        data.setMessage("File Uploaded.");
                        data.setScreenTemplate("PETUploadSummary.vm");
                    } catch (Throwable e) {
                        error(e,data);
                        session.setAttribute(uploadID + "Upload", new Integer(-1));
                        session.setAttribute(uploadID + "Extract", new Integer(-1));
                        session.setAttribute(uploadID + "Analyze", new Integer(-1));
                    }finally{
                        fi.delete();
                        
                        if (tempDIR.exists())
                            FileUtils.DeleteFile(tempDIR);
                        
                        if (uploaded.exists())FileUtils.DeleteFile(uploaded);
                    }
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

    
    public void doFinalize(RunData data, Context context) throws Exception {
    	final ItemI temp = TurbineUtils.GetItemBySearch(data,false);
    	final XnatMrsessiondata tempMR = new XnatMrsessiondata(temp);
        
    	final String uploadID= data.getParameters().getString("uploadID");

    	XnatMrsessiondata mr=null;
        
        String cache_path = ArcSpecManager.GetInstance().getGlobalCachePath();
 
        
        cache_path +="uploads" + File.separator + uploadID + File.separator;
        File dir = new File(cache_path);
        if (dir.exists())
        {
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
                            SAXReader reader = new SAXReader(TurbineUtils.getUser(data));
                            XFTItem temp2 = reader.parse(xml.getAbsolutePath());
                            mr = new XnatMrsessiondata(temp2);
                        }
                    }
                }
            }
        }
        
        if (mr.getUser()==null)
        {
            mr.getItem().setUser(TurbineUtils.getUser(data));
        }
        
        

        if (tempMR.getId()!=null){
            mr.setId(tempMR.getId());
        }
        
        if (tempMR.getSessionType()!=null){
            mr.setSessionType(tempMR.getSessionType());
        }
        
        if (tempMR.getScanner()!=null){
            mr.setScanner(tempMR.getScanner());
        }
        
        if (tempMR.getStabilization()!=null){
            mr.setStabilization(tempMR.getStabilization());
        }
        
        if (tempMR.getMarker()!=null){
            mr.setMarker(tempMR.getMarker());
        }
        
        if (tempMR.getOperator()!=null){
            mr.setOperator(tempMR.getOperator());
        }
        
        if (tempMR.getNote()!=null){
            mr.setNote(tempMR.getNote());
        }
                
        if (tempMR.getSubjectId()!=null){
            mr.setSubjectId(tempMR.getSubjectId());
        }
        
        if (!tempMR.getSharing_share().isEmpty()){
            for (int i=0;i<tempMR.getSharing_share().size();i++){
                mr.setSharing_share((ItemI)tempMR.getSharing_share().get(i));
            }
        }
        
        if (tempMR.getProject()!=null){
            mr.setProject(tempMR.getProject());
        }
        
        //ITERATE THROUGH SCANS
        int scancounter =0;
        Iterator scansOLD = mr.getScans_scan().iterator();
        while(scansOLD.hasNext())
        {
            XnatMrscandata scan = (XnatMrscandata)scansOLD.next();
            XnatMrscandata scanTEMP= (XnatMrscandata)tempMR.getScanById(scan.getId());
            if (scanTEMP==null){
            }else{
                if (scanTEMP.getType()!=null)
                    scan.setType(scanTEMP.getType());
                if (scanTEMP.getQuality()!=null)
                    scan.setQuality(scanTEMP.getQuality());
                if (scanTEMP.getNote()!=null)
                    scan.setNote(scanTEMP.getNote());
                
                if (scan.getQuality()==null)
                {
                    scan.setQuality("usable");
                }
            }
            scancounter++;
        }
        
        mr.fixScanTypes();
        
        try {
            preProcess(mr.getItem(),data,context);
        } catch (RuntimeException e1) {
            logger.error("",e1);
        }
        
        
        final ValidationResults vr = mr.validate();        
        
        if (vr != null && !vr.isValid())
        {
            data.getSession().setAttribute(this.getReturnEditItemIdentifier(),mr);
            context.put("vr",vr);
            if (data.getParameters().getString("edit_screen") !=null)
            {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            }
        }else{
            try {
                try { 
                    preSave(mr.getItem(),data,context);
                } catch (CriticalException e) {
                    throw e;
                } catch (RuntimeException e) {
                    logger.error("",e);
                }
                
                mr.save(TurbineUtils.getUser(data),false,allowDataDeletion());
            } catch (Exception e) {
                logger.error("Error Storing " + mr.getXSIType(),e);
                data.setMessage(e.getMessage());

                data.getSession().setAttribute(this.getReturnEditItemIdentifier(),mr);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return;
            }
            
            try {
                postProcessing(mr.getItem(),data,context);
            } catch (Exception e) {
                logger.error("",e);
                data.setMessage(e.getMessage());
            }
        }
    }

}
