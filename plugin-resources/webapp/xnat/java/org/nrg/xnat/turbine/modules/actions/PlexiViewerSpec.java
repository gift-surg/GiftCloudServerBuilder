/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.plexiViewer.manager.PlexiSpecDocReader;
import org.nrg.xdat.turbine.modules.actions.SecureAction;

public class PlexiViewerSpec extends SecureAction {

    static Logger logger = Logger.getLogger(PlexiViewerSpec.class);
    
    public void doPerform(RunData data, Context context){
        boolean rtn = PlexiSpecDocReader.GetInstance().refresh();
        String msg = "PlexiViewerSpec file was refreshed with " + (rtn?"success":"failure");
        logger.info(msg);
        data.setMessage(msg);
        //data.getParameters().add("popup", "true");
        //data.setScreenTemplate("ClosePage.vm");
    }
}
