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
 */
public class XDATScreen_upload_images extends SecureScreen {

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

        String subjectId = null;

        if (TurbineUtils.HasPassedParameter("search_field", data) && ((String) TurbineUtils.GetPassedParameter("search_field", data)).equalsIgnoreCase("xnat:subjectData.ID")) {
            if (TurbineUtils.HasPassedParameter("search_value", data)) {
                subjectId = (String) TurbineUtils.GetPassedParameter("search_value", data);
            }
        } else if (TurbineUtils.HasPassedParameter("subject_id", data)) {
            subjectId = (String) TurbineUtils.GetPassedParameter("subject_id", data);
        } else if (TurbineUtils.HasPassedParameter("part_id", data)) {
            subjectId = (String) TurbineUtils.GetPassedParameter("part_id", data);
        }

        if (subjectId != null) {
            context.put("subject_id", subjectId);
        }

        if (TurbineUtils.HasPassedParameter("project", data)){
            context.put("project", TurbineUtils.GetPassedParameter("project", data));
        }

    }

}
