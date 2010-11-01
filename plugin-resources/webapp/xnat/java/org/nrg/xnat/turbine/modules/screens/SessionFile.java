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
        
        ArrayList<File> files = new ArrayList<File>();
        
        if (data.getParameters().get("scan")!=null){
            String scanId = data.getParameters().get("scan");
            XnatMrscandata scan = (XnatMrscandata)mr.getScanById(scanId);
            List<XnatAbstractresourceI> resources= scan.getFile();

            String resourceID= data.getParameters().get("resourceID");
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath()));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath());
                    }
                }
            }
        }
        
        if (data.getParameters().get("assessor")!=null){
            String scanId = data.getParameters().get("assessor");
            XnatMrassessordata scan = (XnatMrassessordata)mr.getAssessorById(scanId);
            List<XnatAbstractresourceI> resources= scan.getOut_file();

            String resourceID= data.getParameters().get("resourceID");
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath()));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath());
                    }
                }
            }
        }
        
        if (data.getParameters().get("reconstruction")!=null){
            String scanId = data.getParameters().get("reconstruction");
            XnatReconstructedimagedata scan = (XnatReconstructedimagedata)mr.getReconstructionByID(scanId);
            List<XnatAbstractresourceI> resources= scan.getOut_file();

            String resourceID= data.getParameters().get("resourceID");
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath()));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath());
                    }
                }
            }
        }
        
        if (data.getParameters().get("resource")!=null){
            String scanId = data.getParameters().get("resource");
            List<XnatAbstractresourceI> resources= mr.getResources_resource();

            String resourceID= data.getParameters().get("resourceID");
            if (resourceID==null){
                for(XnatAbstractresourceI resource : resources){
                    files.addAll(((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath()));
                }
            }else{
                for (XnatAbstractresourceI resource : resources){
                    if (resource.getXnatAbstractresourceId().toString().equals(resourceID)){
                        files= ((XnatAbstractresource)resource).getCorrespondingFiles(mr.getArchivePath());
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
