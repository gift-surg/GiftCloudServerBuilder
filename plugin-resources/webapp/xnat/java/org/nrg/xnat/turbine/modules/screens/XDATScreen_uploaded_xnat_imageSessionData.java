/** 
 * $Id: XDATScreen_uploaded_xnat_imageSessionData.java,v 1.7 2010/03/30 20:05:46 timo Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnat.turbine.modules.screens;


import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class XDATScreen_uploaded_xnat_imageSessionData extends
	EditScreenA {
    private final static String PREARC_PAGE = "XDATScreen_prearchives.vm";
    private static final float BYTES_PER_MB = 1024*1024;
    
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

        final Collection<Map<String,Object>> scanprops = new LinkedList<Map<String,Object>>();
        for (final XnatImagescandataI scan : session.getSortedScans()) {
            long scanSize = 0;
            final Collection<File> files = ((XnatImagescandata)scan).getJavaFiles(session.getPrearchivepath());
            for (final File file : files) {
        	scanSize += file.length();
            }
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put("files", (long)files.size());
            props.put("size", String.format("%.1f", scanSize/BYTES_PER_MB));
            scanprops.add(props);
        }
        context.put("scanprops", scanprops);
        
        String tag=null;
        if(data.getParameters().containsKey("tag")){
            tag = data.getParameters().get("tag");
        }else{
            tag = getStringIdentifierForPassedItem(data);
            data.getParameters().add("tag", tag);
        }
        data.getSession().setAttribute(tag, session);
        context.put("tag", tag);
        
		//review label
		String label = session.getLabel();
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
