//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Jul 20, 2005
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

/**
 * @author Tim
 *
 */
public class XDATScreen_add_experiment extends SecureScreen {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    protected void doBuildTemplate(RunData data, Context context) throws Exception {

//        ArrayList elements = new ArrayList();
//        Iterator editable_Displays = TurbineUtils.getUser(data).getEditableElementDisplays().iterator();
//        while (editable_Displays.hasNext())
//        {
//            ElementDisplay ed = (ElementDisplay)editable_Displays.next();
//            SchemaElement se = ed.getSchemaElement();
//            if (se.getGenericXFTElement().getExtendedElements().contains("xnat:subjectAssessorData"))
//            {
//                elements.add(se.getProperName());
//            }
//        }
//        elements.trimToSize();

        if(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("confirmed",data))!=null)
        {
            context.put("confirmed",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("confirmed",data)));
        }

        if (TurbineUtils.HasPassedParameter("destination", data)){
            context.put("destination", TurbineUtils.GetPassedParameter("destination", data));
        }

        if (TurbineUtils.HasPassedParameter("tag", data)){
            context.put("tag", TurbineUtils.GetPassedParameter("tag", data));
        }

        String part_id = null;
        String project= null;

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

        if (part_id!=null){
            if (TurbineUtils.HasPassedParameter("subject_id", data)){
                part_id= (String)TurbineUtils.GetPassedParameter("subject_id", data);
            }
            if (TurbineUtils.HasPassedParameter("part_id", data)){
                part_id= (String)TurbineUtils.GetPassedParameter("part_id", data);
            }
        }

        if (part_id!=null){
                context.put("part_id", part_id);
        }

        if (TurbineUtils.HasPassedParameter("project", data)){
            project= (String)TurbineUtils.GetPassedParameter("project", data);
            context.put("project", project);
        }

    }

}
