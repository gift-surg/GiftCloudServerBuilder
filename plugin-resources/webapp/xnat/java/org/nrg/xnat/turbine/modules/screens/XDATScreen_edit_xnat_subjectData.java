//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Jul 19, 2005
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author Tim
 *
 */
public class XDATScreen_edit_xnat_subjectData extends EditScreenA {

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
     */
    public String getElementName() {
        return "xnat:subjectData";
    }
    
    public ItemI getEmptyItem(RunData data) throws Exception
	{
	    String s = getElementName();
		ItemI temp =  XFTItem.NewItem(s,TurbineUtils.getUser(data));
		return temp;
	}

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context) {
        try {

            XnatSubjectdata subject = new XnatSubjectdata(item);
            context.put("subject",subject);
            if (TurbineUtils.HasPassedParameter("destination", data)){
                context.put("destination", TurbineUtils.GetPassedParameter("destination", data));
            }
            
            if (subject.getProperty("ID")==null)
            {
                context.put("page_title","New archive subject");
            }else{
                context.put("page_title","Edit archive subject");
            }
            
            if (data.getParameters().getString("project")!=null){
                context.put("project", data.getParameters().getString("project"));
            }
        } catch (Exception e) {
        }
    }

}
