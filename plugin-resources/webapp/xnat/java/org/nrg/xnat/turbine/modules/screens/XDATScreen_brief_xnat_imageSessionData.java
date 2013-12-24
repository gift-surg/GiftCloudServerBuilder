/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_brief_xnat_imageSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */

/**
 * 
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.XnatImagesessiondataBean;



/**
 * @author tolsen01
 *
 */
public class XDATScreen_brief_xnat_imageSessionData extends PrearchiveSessionScreen {
	/* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void finalProcessing(XnatImagesessiondataBean session, RunData data, Context context) throws Exception{

        
    }
}
