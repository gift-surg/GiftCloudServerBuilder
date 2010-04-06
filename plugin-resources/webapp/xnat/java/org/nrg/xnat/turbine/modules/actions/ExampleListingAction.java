//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jun 28, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.turbine.modules.actions.BundleAction;
import org.nrg.xdat.turbine.modules.actions.DisplaySearchAction;
import org.nrg.xdat.turbine.modules.actions.ListingAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class ExampleListingAction  extends ListingAction {

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ListingAction#getDestinationScreenName(org.apache.turbine.util.RunData)
     */
    @Override
    public String getDestinationScreenName(RunData data) {
        return "ExampleListingActionScreen.vm";
    }
    
}

