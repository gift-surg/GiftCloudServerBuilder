//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Mar 12, 2008
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.actions.AdminAction;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class SetEmailSpecs extends AdminAction {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        PopulateItem populater = null;
        populater = PopulateItem.Populate(data,"arc:ArchiveSpecification",true);
        XFTItem item = populater.getItem();
        item.setUser(TurbineUtils.getUser(data));
        
        ArcArchivespecification arc = new ArcArchivespecification(item);
        
        SaveItemHelper.authorizedSave(arc,TurbineUtils.getUser(data), false, false);
        
        ArcSpecManager.Reset();
    }

}
