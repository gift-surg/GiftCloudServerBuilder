//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Apr 1, 2005
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

/**
 * @author Tim
 *
 */
public class XDATScreen_mr_super_search extends SecureScreen {

    /* (non-Javadoc)
     * @see org.cnl.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void doBuildTemplate(RunData data, Context context) {
        try {
            Hashtable hash = ElementSecurity.GetDistinctIdValuesFor("xnat:investigatorData","default",TurbineUtils.getUser(data).getLogin());
            context.put("investigators",hash);
            SchemaElement se = SchemaElement.GetElement("xnat:mrSessionData");
            context.put("sessionElement",se);
            SchemaElement subject = SchemaElement.GetElement("xnat:subjectData");
            context.put("subjectElement",subject);
        } catch (Exception e) {
        }
    }

}
