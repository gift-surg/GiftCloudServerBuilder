//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Mar 11, 2005
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xft.XFTItem;

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
