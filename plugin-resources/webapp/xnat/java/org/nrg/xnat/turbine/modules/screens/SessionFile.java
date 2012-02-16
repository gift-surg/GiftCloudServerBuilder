//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Oct 29, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatMrassessordata;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.turbine.modules.screens.FileScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class SessionFile extends FileScreen {

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.FileScreen#getDownloadFile(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public File getDownloadFile(RunData data, Context context) {
        String fileName= (String)TurbineUtils.GetPassedParameter("file_name",data);
        String mrID= (String)TurbineUtils.GetPassedParameter("mr",data);
        
        
        XnatMrsessiondata mr = (XnatMrsessiondata)XnatMrsessiondata.getXnatMrsessiondatasById(mrID, TurbineUtils.getUser(data), false);


        String rootPath;
		try {
			rootPath = mr.getArchiveRootPath();
		} catch (UnknownPrimaryProjectException e) {
			rootPath=null;
		}
		
        ArrayList<File> files = new ArrayList<File>();
        
        if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("scan",data))!=null){
            String scanId = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("scan",data));
            XnatMrscandata scan = (XnatMrscandata)mr.getScanById(scanId);
            List<XnatAbstractresourceI> resources= scan.getFile();
            
            String resourceID= ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resourceID",data));
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(rootPath));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(rootPath);
                    }
                }
            }
        }
        
        if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("assessor",data))!=null){
            String scanId = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("assessor",data));
            XnatMrassessordata scan = (XnatMrassessordata)mr.getAssessorById(scanId);
            List<XnatAbstractresourceI> resources= scan.getOut_file();

            String resourceID= ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resourceID",data));
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(rootPath));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(rootPath);
                    }
                }
            }
        }
        
        if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("reconstruction",data))!=null){
            String scanId = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("reconstruction",data));
            XnatReconstructedimagedata scan = (XnatReconstructedimagedata)mr.getReconstructionByID(scanId);
            List<XnatAbstractresourceI> resources= scan.getOut_file();

            String resourceID= ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resourceID",data));
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(rootPath));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(rootPath);
                    }
                }
            }
        }
        
        if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resource",data))!=null){
            String scanId = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resource",data));
            List<XnatAbstractresourceI> resources= mr.getResources_resource();

            String resourceID= ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("resourceID",data));
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(rootPath));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(rootPath);
                    }
                }
            }
        }
        
        File _return = null;
        
        for (File f: files){
            String localFilename=f.getName();
            int index = localFilename.lastIndexOf("\\");
            if (index< localFilename.lastIndexOf("/"))index = localFilename.lastIndexOf("/");
            if(index>0)localFilename = localFilename.substring(index+1);
            if (localFilename.equals(fileName)){
                _return = f;
            }
        }
        
        return _return;
    }
    
    public void logAccess(RunData data)
    {
    }

}
