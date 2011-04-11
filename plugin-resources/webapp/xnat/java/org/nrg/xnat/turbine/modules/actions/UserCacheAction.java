/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class UserCacheAction extends SecureAction {

	static Logger logger = Logger.getLogger(UserCacheAction.class);

    /* (non-Javadoc)

     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)

     */

    public void doPerform(RunData data, Context context) throws Exception {
    	String folder = data.getParameters().get("folder");
    	Boolean delete = new Boolean(data.getParameters().get("delete"));
    	if (folder != null) {
	    	if (delete.booleanValue()) {
	    		//doDelete(data,context);
	    	}else {
	    		doDownload(data,context);
	    	}
    	}
    }
    
    public void doDownload(RunData data, Context context) throws Exception {
    	String folder = data.getParameters().get("folder");
    	if (folder != null) {
    		folder = folder.trim();
    		XDATUser user = TurbineUtils.getUser(data);
    		String globalCachePath = ArcSpecManager.GetInstance().getGlobalCachePath() + user.getLogin();
    		String folderPath = globalCachePath + File.separator + folder;
    		File folderFile = new File(folderPath); 
    		if (folderFile.exists()) {
    			
    			String contentType = "application/zip";
	        	String fileName= folderFile.getName() + ".zip";
	        	
	        	Object  xarDownload = TurbineUtils.GetPassedParameter("download_type",data); 
	        	
    			if (xarDownload != null && xarDownload.equals("xar"))  {
                    contentType = "application/xar";
                    fileName= folderFile.getName() + ".xar";
    			}   
    			    boolean isZip = true;
		    	        try {
		    	            HttpServletResponse response= data.getResponse();
		    	    		response.setContentType(contentType);
		    				response.setHeader("Content-Disposition","inline;filename=" + fileName);
		                    OutputStream outStream = response.getOutputStream();
		                    
		    				if ( isZip) {
	    						ZipI zip = null;
		    					try {
		    						 zip = new ZipUtils();
		    						zip.setOutputStream(outStream,ZipUtils.DEFAULT_COMPRESSION);
				                	if (!folderFile.isDirectory())
				                		zip.write(folderFile.getName(),folderFile);
				                	else 
				                		zip.writeDirectory(folderFile);
		    					}finally{
		    						if (zip != null) zip.close();
		    					}
		    				}/*else {
		    					URL fileDir = null;
		    				    URLConnection urlConn = null;
		    				    BufferedInputStream buf = null;
		    				    try {
		    				    fileDir = folderFile.toURL();
	    				        urlConn = fileDir.openConnection();
	    				        response.setContentLength((int) urlConn.getContentLength());
	    				        buf = new BufferedInputStream(urlConn.getInputStream());
	    				        int readBytes = 0;
	    				        //read from the file; write to the ServletOutputStream
	    				        while ((readBytes = buf.read()) != -1)
	    				        	outStream.write(readBytes);
		    				    }finally{
		    				    	if (outStream != null) outStream.close();
		    				    	if (buf != null) buf.close();
		    				    }
		    				}*/
		    	        }catch (Exception e) {
		    	        	logger.error("",e);
		    	            data.setMessage(e.getMessage());
		    	        } 
    		}
    	}
    }
    
    public void doDelete(RunData data, Context context) throws Exception {
    	String folder = data.getParameters().get("folder");
    	Boolean delete = new Boolean(data.getParameters().get("delete"));
    	if (folder != null && delete != null) {
        	folder = folder.trim();
    		XDATUser user = TurbineUtils.getUser(data);
    		String globalCachePath = ArcSpecManager.GetInstance().getGlobalCachePath() + user.getLogin();
    		String folderPath = globalCachePath + File.separator + folder;
    		File folderFile = new File(folderPath); 
    		if (folderFile.exists() && delete.booleanValue()) {
    			boolean success = deleteDir(folderFile);
    			 String msg = "Folder " + folder  ;
    			if (!success) {
    				msg += " couldnt be deleted";
    			}else msg+= " deleted succesfully";
    			data.setMessage(msg);
    			data.setScreenTemplate("ClosePage.vm");
    		}
    	}
   }
    
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

   
    
      
}
