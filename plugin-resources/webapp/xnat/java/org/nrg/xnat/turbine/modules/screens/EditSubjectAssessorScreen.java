//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 15, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;

public abstract class EditSubjectAssessorScreen extends EditScreenA {

    @Override
    public void finalProcessing(RunData data, Context context) {
        try {
            if (item != null)
            {
                XnatSubjectassessordata mr=null;
                ItemI part = TurbineUtils.GetParticipantItem(data);
                if (part !=null)
                {
                    mr= new XnatSubjectassessordata(item);
                    context.put("part",new XnatSubjectdata(part));
                }else{
                    mr = new XnatSubjectassessordata(item);
                    context.put("notes",mr.getNote());
                    context.put("part",mr.getSubjectData());
                }
                
                if(mr.getProject()==null){
                	if(context.get("project")!=null){
                		mr.setProject((String)context.get("project"));
                	}
                }
            }
            

        }catch(Exception e)
        {
        }
    }

}
