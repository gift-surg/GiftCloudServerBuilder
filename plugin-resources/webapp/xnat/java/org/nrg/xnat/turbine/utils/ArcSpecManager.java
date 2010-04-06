//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 7, 2007
 *
 */
package org.nrg.xnat.turbine.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.actions.DisplaySearchAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFT;
import org.xml.sax.SAXException;

/**
 * @author timo
 *
 */
public class ArcSpecManager {
    static org.apache.log4j.Logger logger = Logger.getLogger(ArcSpecManager.class);
    private static ArcArchivespecification arcSpec = null;
    
    public static String GetSiteID(){
    	String site_id=GetInstance().getSiteId();
    	if(site_id==null || site_id.equals(""))
    		site_id="XNAT";
    	return site_id;
    }
    
    public synchronized static  ArcArchivespecification GetInstance(){
        if (arcSpec==null){
            System.out.print("Initializing ArcSpec...");
            ArrayList<ArcArchivespecification> allSpecs = ArcArchivespecification.getAllArcArchivespecifications(null,false);
            if (allSpecs.size()>0) {
                arcSpec = allSpecs.get(0);
            }
            
            if (arcSpec!=null){
     
                if (arcSpec.getSiteAdminEmail()!=null && !arcSpec.getSiteAdminEmail().equals("")){
                    XFT.SetAdminEmail(arcSpec.getSiteAdminEmail());
                }else{
                    arcSpec.setSiteAdminEmail(XFT.GetAdminEmail());
                }
                
                if (arcSpec.getSiteUrl()!=null && !arcSpec.getSiteUrl().equals("")){
                    XFT.SetSiteURL(arcSpec.getSiteUrl());
                }else{
                    arcSpec.setSiteUrl(XFT.GetSiteURL());
                }
                
                if (arcSpec.getSiteId()!=null && !arcSpec.getSiteId().equals("")){
                    XFT.SetSiteID(arcSpec.getSiteId());
                }else{
                    arcSpec.setSiteId("");
                }
                
                if (arcSpec.getSmtpHost()!=null && !arcSpec.getSmtpHost().equals("")){
                    XFT.SetAdminEmailHost(arcSpec.getSmtpHost());
                }else{
                    arcSpec.setSmtpHost(XFT.GetAdminEmailHost());
                }
                
                if (arcSpec.getEnableNewRegistrations()!=null){
                    XFT.SetUserRegistration(arcSpec.getEnableNewRegistrations().toString());
                }else{
                    arcSpec.setEnableNewRegistrations(XFT.GetUserRegistration());
                }
                
                if (arcSpec.getRequireLogin()!=null){
                    XFT.SetRequireLogin(arcSpec.getRequireLogin().toString());
                }else{
                    arcSpec.setRequireLogin(XFT.GetRequireLogin());
                }

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getPipelinepath()!=null){
                    XFT.SetPipelinePath(arcSpec.getGlobalpaths().getPipelinepath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null){
                        arcSpec.getGlobalpaths().setPipelinepath(XFT.GetPipelinePath());
                    }
                }

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getArchivepath()!=null){
                    XFT.SetArchiveRootPath(arcSpec.getGlobalpaths().getArchivepath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null && XFT.GetArchiveRootPath()!=null){
                        arcSpec.getGlobalpaths().setArchivepath(XFT.GetArchiveRootPath());
                    }
                }

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getCachepath()!=null){
                    XFT.SetCachePath(arcSpec.getGlobalpaths().getCachepath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null && XFT.GetCachePath()!=null){
                        arcSpec.getGlobalpaths().setCachepath(XFT.GetCachePath());
                    }
                }

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getPrearchivepath()!=null){
                    XFT.SetPrearchivePath(arcSpec.getGlobalpaths().getPrearchivepath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null && XFT.GetPrearchivePath()!=null){
                        arcSpec.getGlobalpaths().setPrearchivepath(XFT.GetPrearchivePath());
                    }
                }

                
                //set email defaults
                if (arcSpec.getEmailspecifications_newUserRegistration()==null){
                    arcSpec.setEmailspecifications_newUserRegistration(true);
                }
                if (arcSpec.getEmailspecifications_pageEmail()==null){
                    arcSpec.setEmailspecifications_pageEmail(true);
                }
                if (arcSpec.getEmailspecifications_pipeline()==null){
                    arcSpec.setEmailspecifications_pipeline(true);
                }
                if (arcSpec.getEmailspecifications_projectAccess()==null){
                    arcSpec.setEmailspecifications_projectAccess(true);
                }
                if (arcSpec.getEmailspecifications_transfer()==null){
                    arcSpec.setEmailspecifications_transfer(true);
                }
                //end email defaults

                if (arcSpec.getEmailspecifications_newUserRegistration()!=null){
                    AdminUtils.SetNewUserRegistrationsEmail(arcSpec.getEmailspecifications_newUserRegistration());
                }

                if (arcSpec.getEmailspecifications_pageEmail()!=null){
                    AdminUtils.SetPageEmail(arcSpec.getEmailspecifications_pageEmail());
                }
            }
            
            try {
                if (arcSpec!=null){
                    String cachePath = arcSpec.getGlobalCachePath();
                    if (cachePath!=null){
                        File f = new File(cachePath,"archive_specification.xml");
                        f.getParentFile().mkdirs();
                        FileWriter fw = new FileWriter(f);
                        
                        arcSpec.toXML(fw, true);
                        fw.flush();
                        fw.close();
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.error("",e);
            } catch (IOException e) {
                logger.error("",e);
            } catch (SAXException e) {
                logger.error("",e);
            }
            System.out.println("done");
        }
        return arcSpec;
    }

    public synchronized static  void Reset(){
        arcSpec=null;
    }
    
    public static boolean allowTransferEmail(){
    	return GetInstance().getEmailspecifications_transfer();
    }
    
    /*
    public  String getGlobalArchivePath(){
        ArcArchivespecification spec = GetInstance();
        String path = null;
        if (spec!=null){
            ArcPathinfoI pathInfo= spec.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getArchivepath();
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getGlobalPrearchivePath(){
        ArcArchivespecification spec = GetInstance();
        String path = null;
        if (spec!=null){
            ArcPathinfoI pathInfo= spec.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getPrearchivepath();
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getGlobalCachePath(){
        ArcArchivespecification spec = GetInstance();
        String path = null;
        if (spec!=null){
            ArcPathinfoI pathInfo= spec.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getCachepath();
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  String getGlobalBuildPath(){
        ArcArchivespecification spec = GetInstance();
        String path = null;
        if (spec!=null){
            ArcPathinfoI pathInfo= spec.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getBuildpath();
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String GetArchivePathForProject(String id){
        String path = null;
        ArcArchivespecification spec = GetInstance();
        if (spec !=null){
            ArrayList<ArcProject> projects=spec.getProjects_project();
            for (ArcProject p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getArchivepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= spec.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getArchivepath();
                }
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String GetCachePathForProject(String id){
        String path = null;
        ArcArchivespecification spec = GetInstance();
        if (spec !=null){
            ArrayList<ArcProject> projects=spec.getProjects_project();
            for (ArcProject p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getCachepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= spec.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getCachepath();
                }
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  String GetPrearchivePathForProject(String id){
        String path = null;
        ArcArchivespecification spec = GetInstance();
        if (spec !=null){
            ArrayList<ArcProject> projects=spec.getProjects_project();
            for (ArcProject p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getPrearchivepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= spec.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getPrearchivepath();
                }
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String GetBuildPathForProject(String id){
        String path = null;
        ArcArchivespecification spec = GetInstance();
        if (spec !=null){
            ArrayList<ArcProject> projects=spec.getProjects_project();
            for (ArcProject p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getBuildpath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= spec.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getBuildpath();
                }
            }
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  ArcProject GetProjectArc(String id){
        ArcArchivespecification spec = GetInstance();
        if (spec !=null){
            ArrayList<ArcProject> projects=spec.getProjects_project();
            for (ArcProject p : projects){
                if (p.getId().equals(id)){
                    return p;
                }
            }
        }
        return null;
    }
    */
}
