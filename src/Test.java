import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.util.Calendar;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xft.XFT;
import org.nrg.xft.utils.zip.ZipUtils;


/*
 * Test
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */




public class Test {

    public static void main(String[] args) {

        String appDir = "C:\\xdat\\deployments\\cnda_xnat";

        Service service = new Service();
        String siteurl ="http://cninds05l.neuroimage.wustl.edu:8080/cnda_xnat/"; 
        //String siteurl ="http://localhost:7080/cnda_xnat/";
        try {
        //REQUEST SESSION ID
        Call call = (Call)service.createCall();
        call.setUsername("tolsen");
        call.setPassword("mysql");
        URL requestSessionURL = new URL(siteurl + "axis/CreateServiceSession.jws");
        call.setTargetEndpointAddress(requestSessionURL);
        call.setOperationName("execute");
        
        
        String adjustPath = "fullpath";
        Object[] params = {};  
        String service_session = (String)call.invoke(params);

        File workDir = new File(appDir,"work/cnda");
        if (!adjustPath.equals(""))
            workDir = new File(workDir,"_" + adjustPath);
        workDir.mkdirs();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -100);
        try {
            call = (Call)service.createCall();
            call.setTargetEndpointAddress(siteurl + "axis/Browse.jws");

            call.setOperationName("search");
            params = new Object[]{service_session,"xnat:mrSessionData",cal.getTime()};
            //SEND SEARCH

            if (XFT.VERBOSE)System.out.println("Sending Request...");
            long startTime = Calendar.getInstance().getTimeInMillis();
            Object[] o = (Object[])call.invoke(params);
            long duration = Calendar.getInstance().getTimeInMillis() - startTime;
            if (XFT.VERBOSE)System.out.println("Response Received (" + duration + " ms)");

            //CLOSE SESSION
            call = (Call) service.createCall();
            requestSessionURL = new URL(siteurl
                + "axis/CloseServiceSession.jws");
            call.setTargetEndpointAddress(requestSessionURL);
            call.setOperationName("execute");
            params = new Object[] { service_session };
            call.invoke(params);
            
            Object[] ids = (Object[])o[1];
            
            for (int i =0;i<ids.length;i++)
            {
                String id = (String)ids[i];

                //id ="030421_vc11784";
                
                call = (Call)service.createCall();
                call.setUsername("tolsen");
                call.setPassword("mysql");
                requestSessionURL = new URL(siteurl + "axis/CreateServiceSession.jws");
                call.setTargetEndpointAddress(requestSessionURL);
                call.setOperationName("execute");
                
                params = new Object[]{};  
                service_session = (String)call.invoke(params);
                
                
                if (XFT.VERBOSE)System.out.println("Requesting xml for " + id + ": " + i + " of " + ids.length);
                startTime = Calendar.getInstance().getTimeInMillis();
                URL url = new URL(siteurl + "app/template/MRXMLSearch.vm/session/" + service_session + "/id/" + id + "/data_type/xnat:mrSessionData/adjustPath/" + adjustPath);
//           Use Buffered Stream for reading/writing.
                BufferedInputStream  bis = null; 
                BufferedOutputStream bos = null;
                
                
                File outFile = new File(workDir,id +"_" + adjustPath + ".xml");
                
                FileOutputStream out = new FileOutputStream(outFile);

                bis = new BufferedInputStream(url.openStream());
                bos = new BufferedOutputStream(out);

                byte[] buff = new byte[2048];
                int bytesRead;
                
                while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                    bos.write(buff, 0, bytesRead);

                }
                
                bos.flush();
                bos.close();
                
                if (XFT.VERBOSE)System.out.println("Response Received (" + (Calendar.getInstance().getTimeInMillis() - startTime) + " ms)");

                call = (Call) service.createCall();
                requestSessionURL = new URL(siteurl
                    + "axis/CloseServiceSession.jws");
                call.setTargetEndpointAddress(requestSessionURL);
                call.setOperationName("execute");
                params = new Object[] { service_session };
                call.invoke(params);
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
        }finally{
            call = (Call) service.createCall();
            requestSessionURL = new URL(siteurl
                + "axis/CloseServiceSession.jws");
            call.setTargetEndpointAddress(requestSessionURL);
            call.setOperationName("execute");
            params = new Object[] { service_session };
            call.invoke(params);
        }
        
        
    }catch(Exception e){
        e.printStackTrace();
    }
    
    
        System.out.print("");
    }

}
