/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.archive.XNATSessionBuilder;
import org.nrg.xnat.exceptions.MultipleScanException;
import org.nrg.xnat.exceptions.ValidationException;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.xml.sax.SAXException;


/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */
public class PullScanDataFromHeaders implements Callable<Boolean> {
	static Logger logger = Logger.getLogger(PullScanDataFromHeaders.class);
	
	private final XnatImagescandata tempMR;
	private final XDATUser user;
	private final boolean allowDataDeletion,isInPrearchive;
	
	public PullScanDataFromHeaders(final XnatImagescandata scan, final XDATUser user, boolean allowDataDeletion,boolean isInPrearchive){
		this.tempMR=scan;
		this.user=user;
		this.allowDataDeletion=allowDataDeletion;
		this.isInPrearchive=isInPrearchive;
	}

	/**
	 * This method will pull header values from DICOM (or ECAT) and update the scan xml accordingly.  It assumes the files are already in the archive and properly referenced from the session xml.  This would usually be run after you've added the files via the REST API.
	 * WARNINGS: 
	 *    This method will not update session level parameters
	 *    This method will fail if the scan directory contains more then one scan or session.
	 * 
	 * @throws IOException: Error accessing files
	 * @throws SAXException: Error parsing generated xml
	 * @throws MultipleSessionException
	 * @throws MultipleScanException
     * @throws ValidationException: Scan invalid according to schema requirements (including xdat tags)
	 * @throws Exception
	 */
	public Boolean call() throws IOException,SAXException,MultipleScanException,ValidationException,Exception{
		final File scanDir=new File(tempMR.deriveScanDir());
        
		//build timestamped file for SessionBuilder output.
		final String timestamp=(new java.text.SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP)).format(Calendar.getInstance().getTime());
		final File xml= new File(scanDir,tempMR.getId()+ "_"+ timestamp+".xml");
		
		//run DICOM builder
		final XNATSessionBuilder builder= new XNATSessionBuilder(scanDir,xml,tempMR.getImageSessionData().getProject(),isInPrearchive);
		builder.call();
		
	    if(!xml.exists() || xml.length()==0){
	    	new Exception("Unable to locate DICOM or ECAT files");
	    }
	    
		final SAXReader reader = new SAXReader(user);
		final XFTItem temp2 = reader.parse(xml.getAbsolutePath());
		final XnatImagesessiondata newmr = (XnatImagesessiondata)BaseElement.GetGeneratedItem(temp2);
        XnatImagescandata newscan=null;
        
		
    	if(newmr.getScans_scan().size()>1){
    		throw new MultipleScanException();
    	}else{
    		newscan=(XnatImagescandata)newmr.getScans_scan().get(0);
    	}
             
        newscan.copyValuesFrom(tempMR);
        newscan.setImageSessionId(tempMR.getImageSessionId());
        newscan.setId(tempMR.getId());
        newscan.setXnatImagescandataId(tempMR.getXnatImagescandataId());
    	
	    if(!allowDataDeletion){
    		while(newscan.getFile().size()>0)newscan.removeFile(0);
		}

        final ValidationResults vr = newmr.validate();        
        
        if (vr != null && !vr.isValid())
        {
            throw new ValidationException(vr.toString());
        }else{
        	final XnatImagesessiondata mr=tempMR.getImageSessionData();
        	final XnatProjectdata proj = mr.getProjectData();
        	if(SaveItemHelper.authorizedSave(newscan,user,false,allowDataDeletion)){
				try {
				MaterializedView.DeleteByUser(user);

				if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode().equals(1)){
					mr.quarantine(user);
				}
					} catch (Exception e) {
						logger.error("",e);
					}
			}
            
            try {
            	final WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
  				workflow.setDataType(mr.getXSIType());
  				workflow.setExternalid(proj.getId());
  				workflow.setId(mr.getId());
  				workflow.setPipelineName("Header Mapping: Scan "+newscan.getId());
  				workflow.setStatus("Complete");
  				workflow.setLaunchTime(Calendar.getInstance().getTime());
  				SaveItemHelper.authorizedSave(workflow,user, false, false);
  			} catch (Throwable e) {
  				e.printStackTrace();
  			}
        }

        return Boolean.TRUE;
	}
}
