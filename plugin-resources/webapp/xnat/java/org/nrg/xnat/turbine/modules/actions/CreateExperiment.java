//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 3, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class CreateExperiment extends SecureAction {
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CreateExperiment.class);

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        if (TurbineUtils.HasPassedParameter("destination", data)){
            context.put("destination", TurbineUtils.GetPassedParameter("destination", data));
        }

        if (TurbineUtils.HasPassedParameter("tag", data)){
            context.put("tag", TurbineUtils.GetPassedParameter("tag", data));
        }
        
        String part_id = null;
        String project= null;
        String parent_expt_id= null;
        String visit= null;
        
        if(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_field",data))!=null)
        {
            if(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_field",data)).equalsIgnoreCase("xnat:subjectData.ID"))
            {
                if(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data))!=null)
                {
                    part_id= ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data));
                }
            }
        }
        
        if (part_id==null){
            if (TurbineUtils.HasPassedParameter("subject_id", data)){
                part_id= (String)TurbineUtils.GetPassedParameter("subject_id", data);
            }
            if (TurbineUtils.HasPassedParameter("part_id", data)){
                part_id= (String)TurbineUtils.GetPassedParameter("part_id", data);
            }
        }
        
        if (part_id!=null){
                context.put("part_id", part_id);
                data.getParameters().setString("part_id", part_id);
        }
        
        if (TurbineUtils.HasPassedParameter("parent_expt_id", data)){
            parent_expt_id= (String)TurbineUtils.GetPassedParameter("parent_expt_id", data);
            context.put("parent_expt_id", parent_expt_id);
        }
        
        if (TurbineUtils.HasPassedParameter("project", data)){
            project= (String)TurbineUtils.GetPassedParameter("project", data);
            context.put("project", project);
        }
        
        if (TurbineUtils.HasPassedParameter("visit", data)){
        	visit= (String)TurbineUtils.GetPassedParameter("visit", data);
        	context.put("visit", visit);
        }
        
        String dataType = (String)TurbineUtils.GetPassedParameter("data_type", data);
        SchemaElement se =SchemaElement.GetElement(dataType);
        data.setScreenTemplate("XDATScreen_edit_" + se.getFormattedName() + ".vm");
    }

}
