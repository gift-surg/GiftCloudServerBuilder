/*
 * org.nrg.xnat.turbine.modules.actions.EditImageSessionAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xft.XFTItem;

import java.util.ArrayList;

/**
 * @author Tim
 *
 */
public class EditImageSessionAction extends ModifySubjectAssessorData {
	static Logger logger = Logger.getLogger(EditImageSessionAction.class);


    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#preSave(org.nrg.xft.XFTItem, org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void preSave(XFTItem item, RunData data, Context context) throws Exception {
        try {
            if (item.getProperty("note")==null)
            {
                item.setProperty("note","NULL");
            }
            
            ArrayList scans = item.getChildItems("scans.scan");
            for (int i=0;i<scans.size();i++)
            {
                XFTItem scan = (XFTItem)scans.get(i);
                if (scan.getProperty("note")==null)
                {
                    scan.setProperty("note","NULL");
                }
            }
        } catch (RuntimeException e1) {
            logger.error("",e1);
        }
    }

    
}
