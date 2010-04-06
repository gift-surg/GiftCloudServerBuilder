// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu May 17 10:21:31 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.WrkXnatexecutionenvironmentParameterI;
import org.nrg.xdat.om.base.auto.AutoWrkXnatexecutionenvironment;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseWrkXnatexecutionenvironment extends AutoWrkXnatexecutionenvironment {

	public BaseWrkXnatexecutionenvironment(ItemI item)
	{
		super(item);
	}

	public BaseWrkXnatexecutionenvironment(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseWrkXnatexecutionenvironment(UserI user)
	 **/
	public BaseWrkXnatexecutionenvironment()
	{}

	public BaseWrkXnatexecutionenvironment(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public XnatPipelineLauncher getLauncher(UserI user) {
        XnatPipelineLauncher xnatLauncher = new XnatPipelineLauncher((XDATUser)user);
        xnatLauncher.setPipelineName(getPipeline());
        xnatLauncher.setStartAt(getStartat());
        ArrayList parameters = getParameters_parameter();
        for (int i = 0; i < parameters.size(); i++) {
            WrkXnatexecutionenvironmentParameterI aParameter = (WrkXnatexecutionenvironmentParameterI)parameters.get(i);
            xnatLauncher.setParameter(aParameter.getName(), aParameter.getParameter());
        }
        ArrayList notified = getNotify();
        for (int i = 0; i < notified.size(); i++) {
            String notifiedEmailId = (String)notified.get(i);
            if (!notifiedEmailId.equals(user.getEmail()) && !notifiedEmailId.equals(AdminUtils.getAdminEmailId())) {
                xnatLauncher.notify(notifiedEmailId);
            }
        }
        xnatLauncher.setDataType(getDatatype());
        xnatLauncher.setId(getId());
        xnatLauncher.setSupressNotification(getSupressnotification());
        if (this.getParameterfile_path() != null) {
            xnatLauncher.setParameterFile(getParameterfile_path());
        }
        return xnatLauncher;
    }
}
