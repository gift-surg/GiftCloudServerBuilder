//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 7, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.services.db.TurbineDB;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class SetArcSpecs extends SecureAction {

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
        
        arc.save(TurbineUtils.getUser(data), false, false);
        
        ArcSpecManager.Reset();
    }

}
