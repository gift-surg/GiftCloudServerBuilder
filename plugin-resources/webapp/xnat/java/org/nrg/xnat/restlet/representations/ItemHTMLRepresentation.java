// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import org.apache.log4j.Logger;
import org.apache.turbine.util.TurbineException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.restlet.data.MediaType;
import org.restlet.data.Request;

public class ItemHTMLRepresentation extends TurbineScreenRepresentation {
	static org.apache.log4j.Logger logger = Logger.getLogger(ItemHTMLRepresentation.class);
	private final String screen;
	
	public ItemHTMLRepresentation(XFTItem i,MediaType mt,Request request,XDATUser _user) throws TurbineException,ElementNotFoundException {
		super(mt,request,_user);
		
		TurbineUtils.setDataItem(data, i);
		 
		try {
			if(i.getProperty("project")!=null){
				data.getParameters().setString("project", i.getStringProperty("project"));
			}
		} catch (Throwable e1) {
			logger.error("",e1);
		}
		
		
		screen = DisplayItemAction.GetReportScreen(i.getItem().getGenericSchemaElement());
	}

	@Override
	public String getScreen() {
		return screen;
	}
	
}
