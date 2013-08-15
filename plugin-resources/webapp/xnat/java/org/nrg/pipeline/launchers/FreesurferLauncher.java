/*
 * org.nrg.pipeline.launchers.FreesurferLauncher
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
*/

package org.nrg.pipeline.launchers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.utils.PipelineFileUtils;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.ArrayList;

public class FreesurferLauncher extends PipelineLauncher{
    ArrayList<String> mprageScans = null;
    static org.apache.log4j.Logger logger = Logger.getLogger(FreesurferLauncher.class);

    public FreesurferLauncher(ArrayList<String> mprs) {
        mprageScans = mprs;
    }

    public FreesurferLauncher(RunData data, XnatMrsessiondata mr) {
        mprageScans = getCheckBoxSelections(data,mr,"MPRAGE");
    }

    public boolean launch(RunData data, Context context) {
        return false;
    }


    public boolean launch(RunData data, Context context, XnatMrsessiondata mr) {
        try {
            XnatPipelineLauncher xnatPipelineLauncher = XnatPipelineLauncher.GetLauncher(data, context, mr);

            String pipelineName = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("freesurfer_pipelinename",data));
            String cmdPrefix = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("cmdprefix",data));

            xnatPipelineLauncher.setPipelineName(pipelineName);
            xnatPipelineLauncher.setSupressNotification(true);

            String buildDir = PipelineFileUtils.getBuildDir(mr.getProject(), true);
            buildDir +=  "fsrfer"  ;

            xnatPipelineLauncher.setBuildDir(buildDir);
            xnatPipelineLauncher.setNeedsBuildDir(false);

            Parameters parameters = Parameters.Factory.newInstance();

            if (TurbineUtils.HasPassedParameter("custom_command", data)) {
                ParameterData param = parameters.addNewParameter();
                param.setName("custom_command");
                param.addNewValues().setUnique(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("custom_command",data)));
            }else {

                ParameterData param = parameters.addNewParameter();
                param.setName("sessionId");
                param.addNewValues().setUnique(mr.getLabel());

                param = parameters.addNewParameter();
                param.setName("isDicom");
                param.addNewValues().setUnique(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("isDicom",data)));

                // param = parameters.addNewParameter();
                // param.setName("mprs");
                // param.addNewValues.setUnique(mprageScans);

                param = parameters.addNewParameter();
                param.setName("useall_t1s");
                param.addNewValues().setUnique(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("useall_t1s",data)));

                xnatPipelineLauncher.setParameter("mprs",mprageScans);
            }

            String emailsStr = TurbineUtils.getUser(data).getEmail() + "," + data.getParameters().get("emailField");
            String[] emails = emailsStr.trim().split(",");
            for (int i = 0 ; i < emails.length; i++) {
                if (emails[i]!=null && !emails[i].equals(""))  xnatPipelineLauncher.notify(emails[i]);
            }

            String paramFileName = getName(pipelineName);
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            String s = formatter.format(date);

            paramFileName += "_params_" + s + ".xml";

            String paramFilePath = saveParameters(buildDir + File.separator + mr.getLabel(),paramFileName,parameters);

            xnatPipelineLauncher.setParameterFile(paramFilePath);

            boolean rtn = xnatPipelineLauncher.launch(cmdPrefix);
            return rtn;
        }catch(Exception e) {
            logger.error(e.getCause() + " " + e.getLocalizedMessage());
            return false;
        }
    }

}
