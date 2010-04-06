//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 18, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectdataI;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
public class BrowseProjects extends SecureScreen {
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
    }

}
