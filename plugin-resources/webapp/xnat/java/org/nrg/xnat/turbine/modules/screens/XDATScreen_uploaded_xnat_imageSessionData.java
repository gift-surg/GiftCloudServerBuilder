/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_uploaded_xnat_imageSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.screens;


import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.utils.XNATUtils;

public final class XDATScreen_uploaded_xnat_imageSessionData extends
	EditScreenA {
    private final static String PREARC_PAGE = "XDATScreen_prearchives.vm";
    
    final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_uploaded_xnat_imageSessionData.class);


    
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void finalProcessing(final RunData data, final Context context) {
	if (null == item) {
	    logger.error("finalProcessing invoked with null item");
            data.setMessage("Unable to load session item");
            data.setScreenTemplate(PREARC_PAGE);
	    return;
	}
		
	final String dataType = item.getXSIType();
	if (null == dataType) {
	    logger.error("item has null data type");
	    data.setMessage("session item has null data type");
	    data.setScreenTemplate(PREARC_PAGE);
	    return;
	}
	
	context.put("datatype", dataType);
        context.put("protocols", XNATUtils.getProjectsForCreate(dataType, data));
        //context.put("pages", pages.get(dataType));
        //context.put("scanFields", scanFields.get(dataType));

        XnatImagesessiondata session = (XnatImagesessiondata) BaseElement.GetGeneratedItem(item);
        context.put("notes", session.getNote());
        final ItemI part = TurbineUtils.GetParticipantItem(data);
        if (part != null) {
            final XnatSubjectdata xsd = (part instanceof XnatSubjectdata) ? (XnatSubjectdata)part : new XnatSubjectdata(part);
            session.setSubjectId(xsd.getId());
            context.put("part", xsd);
        } else {
            context.put("part",session.getSubjectData());
        }         

        String tag;
        if(data.getParameters().containsKey("tag")){
            tag = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("tag",data));
        }else{
            tag = getStringIdentifierForPassedItem(data);
            data.getParameters().add("tag", tag);
        }
        data.getSession().setAttribute(tag, session);
        context.put("tag", tag);
        

        
        if(TurbineUtils.HasPassedParameter("src", data)){
        	context.put("src", TurbineUtils.GetPassedParameter("src", data));
        }
        
		//review label
		if(session.getLabel()==null){
			if(session.getId()!=null){
				session.setLabel(session.getId());
			}else if(session.getDcmpatientid()!=null){
				session.setLabel(session.getDcmpatientid());
	        }
	            }
	            
		context.put("edit_screen", "XDATScreen_uploaded_xnat_imageSessionData.vm");
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
     */
    @Override
    public String getElementName() {
        return "xnat:imageSessionData";
    }
}
