//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 12, 2006
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

/**
 * @author timo
 *
 */
public class LaunchUploadApplet extends SecureReport {
    @Override
    public void finalProcessing(final RunData data, final Context context) {
    }

    @Override
    public void noItemError(RunData data, Context context){
        // Do nothing, we're totally cool with not having an item.
    }
}
