//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Mar 13, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class XDATScreen_download_xnat_petSessionData extends SecureReport {

    @Override
    public void finalProcessing(RunData data, Context context) {
        XnatPetsessiondata mr = ((XnatPetsessiondata)om);
        context.put("archive",mr.listArchiveToHTML(TurbineUtils.GetRelativeServerPath(data)));
        data.getSession().setAttribute("download_session", mr);
    }

}
