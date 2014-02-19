/*
 * org.nrg.xnat.turbine.modules.screens.EditSubjectAssessorScreen
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;


import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.utils.ScanQualityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class EditSubjectAssessorScreen extends EditScreenA {
    private final Logger logger = LoggerFactory.getLogger(EditSubjectAssessorScreen.class);

    @Override
    public void finalProcessing(final RunData data, final Context context) {
        try {
            final String project;
            if (item != null) {
                final XnatSubjectassessordata assessor;
                final ItemI part = TurbineUtils.GetParticipantItem(data);
                if (part !=null) {
                    assessor= new XnatSubjectassessordata(item);
                    context.put("part",new XnatSubjectdata(part));
                } else {
                    assessor = new XnatSubjectassessordata(item);
                    context.put("notes",assessor.getNote());
                    context.put("part",assessor.getSubjectData());
                }

                if(assessor.getProject()==null){
                    if(context.get("project")!=null){
                        assessor.setProject((String)context.get("project"));
                    }
                }
                project = assessor.getProject();
            } else {
                project = (String)context.get("project");
            }
            context.put("qualityLabels", ScanQualityUtils.getQualityLabels(project, TurbineUtils.getUser(data)));
        } catch(Throwable t) {
            logger.warn("error in preparing subject assessor edit screen", t);
        }
    }
}
