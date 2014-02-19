/*
 * org.nrg.xnat.turbine.modules.actions.UploadBatch
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class UploadBatch extends SecureAction {
    static org.apache.log4j.Logger logger = Logger.getLogger(UploadBatch.class);

    @Override
    public void doPerform(RunData data,Context context) throws Exception {
        System.out.println("Starting Upload");
        long startTime = Calendar.getInstance().getTimeInMillis();
        ParameterParser params = data.getParameters();
        HttpSession session = data.getSession();
        String uploadID= null;
        if (params.get("ID")!=null && !params.get("ID").equals("")){
            uploadID=params.get("ID");
            session.setAttribute(uploadID + "Upload", new Integer(0));
        }
        XDATUser user = TurbineUtils.getUser(data);
        String cachepath= ArcSpecManager.GetInstance().getGlobalCachePath();
        Date d = Calendar.getInstance().getTime();
        StringBuffer sb = new StringBuffer();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
        sb.append(formatter.format(d));
        
        cachepath += "uploads" + File.separator + user.getLogin() + File.separator + sb.toString() + File.separator;
        File dir = new File(cachepath);

        if (!dir.exists()){
            dir.mkdirs();
        }
//        
//        if (TurbineUpload.getAutomatic())
//        {
            Float file_size = params.getFloat("file_size");
            try {
                //byte[] bytes = params.getUploadData();
                //grab the FileItems available in ParameterParser
                FileItem fi = params.getFileItem("image_archive");
                if (fi != null)
                {                    
                    String filename = fi.getName();
                    File f = new File(filename);
                    f = new File(dir.getAbsolutePath() + File.separator + f.getName());
                    System.out.println("Pre-write: " + ((Calendar.getInstance().getTimeInMillis()-startTime)) + " ms");
                    startTime = Calendar.getInstance().getTimeInMillis();
                    fi.write(f);
                    System.out.println("Write: " + ((Calendar.getInstance().getTimeInMillis()-startTime)) + " ms");
                    
                    if (uploadID!=null)session.setAttribute(uploadID + "Upload", new Long(100));

                    data.setMessage("File Uploaded.");
                    data.setScreenTemplate("BatchUploadSummary.vm");
                    return;
                }
            } catch (FileNotFoundException e) {
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                logger.error("",e);
                data.setMessage("Error. Upload Failed.");
                data.setScreenTemplate("BatchUploadSummary.vm");
                return;
            } catch (IOException e) {
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                logger.error("",e);
                data.setMessage("Error. Upload Failed.");
                data.setScreenTemplate("BatchUploadSummary.vm");
                return;
            } catch (Throwable e) {
                session.setAttribute(uploadID + "Upload", new Integer(-1));
                logger.error("",e);
                data.setMessage("Error. Upload Failed.");
                data.setScreenTemplate("BatchUploadSummary.vm");
                return;
            }
        
    }

}
