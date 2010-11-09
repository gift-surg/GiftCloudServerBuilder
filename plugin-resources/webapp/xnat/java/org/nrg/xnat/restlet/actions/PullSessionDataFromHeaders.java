/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.nrg.session.SessionBuilder.NoUniqueSessionException;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.archive.XNATSessionBuilder;
import org.nrg.xnat.exceptions.ValidationException;
import org.nrg.xnat.restlet.util.SimpleDateFormatUtil;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.xml.sax.SAXException;


/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */
public class PullSessionDataFromHeaders implements Callable<Boolean> {
	static Logger logger = Logger.getLogger(PullSessionDataFromHeaders.class);
	
	private final XnatImagesessiondata tempMR;
	private final XDATUser user;
	private boolean allowDataDeletion;
	private final boolean overwrite;
	
	public PullSessionDataFromHeaders(final XnatImagesessiondata mr, final XDATUser user, boolean allowDataDeletion, final boolean overwrite){
		this.tempMR=mr;
		this.user=user;
		this.allowDataDeletion=allowDataDeletion;
		this.overwrite=overwrite;
	}


	/**
	 * This method will pull header values from DICOM (or ECAT) and update the session xml accordingly.  It assumes the files are already in the archive and properly referenced from the session xml.  This would usually be run after you've added the files via the REST API.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws MultipleSessionException
     * @throws ValidationException: Scan invalid according to schema requirements (including xdat tags)
     * @throws Exception
	 */
	public Boolean call()
	throws IOException,SAXException,ValidationException,NoUniqueSessionException,Exception {
		//identify session directory location based on existing scans, the deriveSessionDir() method will return null if it can't load it from the scans.
		final String derivedSessionDir=tempMR.deriveSessionDir();
		
		if (derivedSessionDir==null){
			throw new Exception("Unable to derive session directory");
		}
		
		final File sessionDir=new File(derivedSessionDir);
		
		//build session xml document for data in the session directory
		final String timestamp=SimpleDateFormatUtil.format(XNATRestConstants.PREARCHIVE_TIMESTAMP, Calendar.getInstance().getTime());
		final File xml = new File(sessionDir,tempMR.getLabel()+ "_"+ timestamp+".xml");
		
		final XNATSessionBuilder builder= new XNATSessionBuilder(sessionDir,xml,tempMR.getProject());
		builder.call();
	      
	    //this should really throw a specific execution object
	    if (!xml.exists() || xml.length()==0) {
	    	new Exception("Unable to locate DICOM or ECAT files");
	    }
	    
	    //build image session object from generated xml
		final SAXReader reader = new SAXReader(user);
		final XFTItem temp2 = reader.parse(xml.getAbsolutePath());
		final XnatImagesessiondata newmr = (XnatImagesessiondata)BaseElement.GetGeneratedItem(temp2);
        
        if(overwrite)
        {
        	//this will ignore the pre-existing session and store the newly generated xml in place of the old one.
        	//this will delete references added resources (snapshots, reconstructions, assessments, etc)
        	allowDataDeletion=false;//why is this set to false when you want to override?
        	newmr.setId(tempMR.getId());
        }else{
        	//copy values from old session, to new session
            newmr.copyValuesFrom(tempMR);

            for (final XnatImagescandata newscan : newmr.getSortedScans()){
            	final XnatImagescandata oldScan = tempMR.getScanById(newscan.getId());
            	//copy values from old session, to new session
            	//if oldScan is null, then a new scan has been discovered and old values are not present to maintain.
            	if(oldScan!=null){
                	newscan.setXnatImagescandataId(oldScan.getXnatImagescandataId());
                	
                	//if allowDataDeletion=true, then new file tags will replace old ones (modifications to content, format, etc will not be preserved).
        		    if(!allowDataDeletion){
                		//in the current code, the new file entries should not be maintained. The old ones are assumed to be correct and not needing updates.
                    	//the content, format, and description of the new file entries will be preserved if the old ones were null.
        		    	if(newscan.getFile().size()>0){
            		    	final XnatResource newcat=(XnatResource)newscan.getFile().get(0);
        		    		
        		    		final XnatAbstractresource oldCat=oldScan.getFile().get(0);
        		    		if(oldCat instanceof XnatResource){
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getContent()) && !StringUtils.IsEmpty(newcat.getContent()))
        		    				((XnatResource)oldCat).setContent(newcat.getContent());
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getFormat()) && !StringUtils.IsEmpty(newcat.getFormat()))
        		    				((XnatResource)oldCat).setFormat(newcat.getFormat());
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getDescription()) && !StringUtils.IsEmpty(newcat.getDescription()))
        		    				((XnatResource)oldCat).setDescription(newcat.getDescription());
        		    		}
        		    		
        		    		while(newscan.getFile().size()>0)newscan.removeFile(0);
                    		
        		    		//replace new files (catalogs) with old ones.
                    		newscan.setFile(oldCat);
        		    	}else{
        		    		while(newscan.getFile().size()>0)newscan.removeFile(0);
                    		
        		    	}
                	}
            	}
    		}
            
        	newmr.setId(tempMR.getId());
        }
        
        //if any scan types are null, they will be filled according to the standard logic.
        newmr.fixScanTypes();        
        
        //xml validation
        final ValidationResults vr = newmr.validate();        
        
        if (vr != null && !vr.isValid())
        {
            throw new ValidationException(vr.toString());
        }else{
        	final XnatProjectdata proj = newmr.getProjectData();
        	if(newmr.save(user,false,allowDataDeletion)){
				MaterializedView.DeleteByUser(user);

				if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode().equals(1)){
					newmr.quarantine(user);
				}
			}
            
            try {
            	final WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
  				workflow.setDataType(newmr.getXSIType());
  				workflow.setExternalid(proj.getId());
  				workflow.setId(newmr.getId());
  				workflow.setPipelineName("Header Mapping");
  				workflow.setStatus("Complete");
  				workflow.setLaunchTime(Calendar.getInstance().getTime());
  				workflow.save(user, false, false);
  			} catch (Throwable e) {
  				logger.error("",e);
  			}
        }
        
        return Boolean.TRUE;
	}
}
