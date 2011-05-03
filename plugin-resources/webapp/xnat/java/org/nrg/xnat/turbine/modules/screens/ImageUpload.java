//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 12, 2006
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/**
 * @author timo
 *
 */
public class ImageUpload extends SecureScreen {
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    protected void doBuildTemplate(final RunData data, final Context context)
    throws MalformedURLException {
        final Date d = Calendar.getInstance().getTime();

        final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_hhmmss");
        context.put("uploadID", formatter.format(d));

        final ArcArchivespecification arc = ArcSpecManager.GetInstance();
        context.put("arc", arc);
        final URL url = new URL(arc.getSiteUrl());
        context.put("hostname", url.getHost());
    }
}
