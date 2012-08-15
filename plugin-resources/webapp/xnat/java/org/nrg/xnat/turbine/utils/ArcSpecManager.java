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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.prearchive.PrearcConfig;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
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
        if(site_id==null || site_id.equals("")) {
            site_id="XNAT";
        }
        return site_id;
    }

	public synchronized static ArcArchivespecification GetFreshInstance() {
		ArcArchivespecification arcSpec = null;
        logger.warn("Getting Fresh ArcSpec...");
		ArrayList<ArcArchivespecification> allSpecs = ArcArchivespecification.getAllArcArchivespecifications(null,false);
	    if (allSpecs.size()>0) {
	        arcSpec = allSpecs.get(0);
	    }
	    return arcSpec;
	}
    
    public synchronized static  ArcArchivespecification GetInstance(){
    	return GetInstance(true);
    }
    
    public synchronized static  ArcArchivespecification GetInstance(boolean dbInit){
        if (arcSpec==null){
            logger.info("Initializing ArcSpec...");
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

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getFtppath()!=null){
                    XFT.setFtpPath(arcSpec.getGlobalpaths().getFtppath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null && XFT.getFtpPath()!=null){
                        arcSpec.getGlobalpaths().setFtppath(XFT.getFtpPath());
                    }
                }

                if (arcSpec.getGlobalpaths()!=null && arcSpec.getGlobalpaths().getBuildpath()!=null){
                    XFT.setFtpPath(arcSpec.getGlobalpaths().getBuildpath());
                }else{
                    if (arcSpec.getGlobalpaths()!=null && XFT.getBuildPath()!=null){
                        arcSpec.getGlobalpaths().setBuildpath(XFT.getBuildPath());
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


                if (StringUtils.isEmpty(arcSpec.getDcm_dcmAe())){
                    arcSpec.setDcm_dcmAe("XNAT");
                }

                if (StringUtils.isEmpty(arcSpec.getDcm_dcmHost())){
                    arcSpec.setDcm_dcmHost("localhost");
                }

                if (StringUtils.isEmpty(arcSpec.getDcm_dcmPort())){
                    arcSpec.setDcm_dcmPort("8104");
                }

                if (arcSpec.getDcm_appletLink()==null){
                    arcSpec.setDcm_appletLink(Boolean.TRUE);
                }
                
                if (arcSpec.getEnableCsrfToken()!=null){
                    XFT.SetEnableCsrfToken(arcSpec.getEnableCsrfToken().toString());
                }else{
                    arcSpec.setEnableCsrfToken(XFT.GetEnableCsrfToken());
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
   
            if(dbInit){
                PrearcConfig prearcConfig = XDAT.getContextService().getBean(PrearcConfig.class);
	            try {
	    			PrearcDatabase.initDatabase(prearcConfig.isReloadPrearcDatabaseOnApplicationStartup());
	    		} catch (Exception e) {
	    			logger.error("",e);
	    		}
            }
        }
        
        return arcSpec;
    }

    public synchronized static  void Reset(){
        arcSpec=null;
    }

    public synchronized static ArcArchivespecification initialize(UserI user) throws XFTInitException, ElementNotFoundException, FieldNotFoundException, InvalidValueException {
        arcSpec = new ArcArchivespecification(user);
        if (XFT.GetAdminEmail()!=null && !XFT.GetAdminEmail().equals("")) {
            arcSpec.setSiteAdminEmail(XFT.GetAdminEmail());
        }

        if (XFT.GetSiteURL()!=null && !XFT.GetSiteURL().equals("")) {
            arcSpec.setSiteUrl(XFT.GetSiteURL());
        }

        if (XFT.GetAdminEmailHost()!=null && !XFT.GetAdminEmailHost().equals("")) {
            arcSpec.setSmtpHost(XFT.GetAdminEmailHost());
        }

        arcSpec.setEnableNewRegistrations(XFT.GetUserRegistration());

        arcSpec.setRequireLogin(XFT.GetRequireLogin());
        if (XFT.GetPipelinePath()!=null && !XFT.GetPipelinePath().equals("")) {
            arcSpec.setProperty("globalPaths/pipelinePath", XFT.GetPipelinePath());
        }

        if (XFT.GetArchiveRootPath()!=null && !XFT.GetArchiveRootPath().equals("")) {
            arcSpec.setProperty("globalPaths/archivePath", XFT.GetArchiveRootPath());
        }

        if (XFT.GetPrearchivePath()!=null && !XFT.GetPrearchivePath().equals("")) {
            arcSpec.setProperty("globalPaths/prearchivePath", XFT.GetPrearchivePath());
        }

        if (XFT.GetCachePath()!=null && !XFT.GetCachePath().equals("")) {
            arcSpec.setProperty("globalPaths/cachePath", XFT.GetCachePath());
        }

        if (XFT.getFtpPath()!=null && !XFT.getFtpPath().equals("")) {
            arcSpec.setProperty("globalPaths/ftpPath", XFT.getFtpPath());
        }

        if (XFT.getBuildPath()!=null && !XFT.getBuildPath().equals("")) {
            arcSpec.setProperty("globalPaths/buildPath", XFT.getBuildPath());
        }
        arcSpec.setEnableCsrfToken(XFT.GetEnableCsrfToken());
        return arcSpec;
    }

    public static boolean allowTransferEmail(){
        return GetInstance().getEmailspecifications_transfer();
    }

}
