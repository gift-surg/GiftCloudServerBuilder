// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.XNATUtils;

public class XDATScreen_add_xnat_projectData extends EditScreenA {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_add_xnat_projectData.class);
	
	public String getElementName() {
	    return "xnat:projectData";
	}
	
	public ItemI getEmptyItem(RunData data) throws Exception
	{
	    String s = getElementName();
		ItemI temp =  XFTItem.NewItem(s,TurbineUtils.getUser(data));
		return temp;
	}
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context) {

        Hashtable hash = XNATUtils.getInvestigatorsForCreate(getElementName(),data);
        context.put("investigators",hash);
        context.put("arc",ArcSpecManager.GetInstance());
        if (TurbineUtils.HasPassedParameter("destination", data)){
            context.put("destination", TurbineUtils.GetPassedParameter("destination", data));
        }
        try {
            ArrayList<ElementSecurity> root = new ArrayList<ElementSecurity>();
            ArrayList<ElementSecurity> subjectAssessors = new ArrayList<ElementSecurity>();
            ArrayList<ElementSecurity> mrAssessors = new ArrayList<ElementSecurity>();
            ArrayList<ElementSecurity> petAssessors = new ArrayList<ElementSecurity>();
            
            Collection<ElementSecurity> all =ElementSecurity.GetElementSecurities().values();
            for (ElementSecurity es: all){
                try {
                    if (es.getAccessible() || (item.getStringProperty("ID")!=null && es.matchesUsageEntry(item.getStringProperty("ID")))){
                        GenericWrapperElement g= es.getSchemaElement().getGenericXFTElement();
                        
                        String parents = g.getPrimaryElements();
                        
                        if(parents.indexOf("xnat:mrAssessorData")!=-1){
                            mrAssessors.add(es);
                        }else if(parents.indexOf("xnat:petAssessorData")!=-1){
                            petAssessors.add(es);
                        }else if(parents.indexOf("xnat:subjectAssessorData")!=-1){
                            subjectAssessors.add(es);
                        }else if (parents.indexOf("xnat:subjectData")!=-1 || parents.indexOf("xnat:experimentData")!=-1){
                            root.add(es);
                        }
                    }
                } catch (Throwable e) {
                    logger.error("",e);
                }
            }
            
            context.put("root", root);
            context.put("subjectAssessors", subjectAssessors);
            context.put("mrAssessors", mrAssessors);
            context.put("petAssessors", petAssessors);
        
			if (item.getProperty("ID")==null)
			{
			    context.put("page_title","New Project");
			}else{
                ArcProject p = ArcSpecManager.GetInstance().getProjectArc(item.getStringProperty("ID"));
                if (p!=null){
                    context.put("arcP", p);
                }
			    context.put("page_title","Edit Project");
			}
		} catch (Exception e) {
			logger.error("",e);
		}
	}

}
