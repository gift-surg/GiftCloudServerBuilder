// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.restlet.data.MediaType;
import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;

public class ItemHTMLRepresentation extends TurbineScreenRepresentation {
	static org.apache.log4j.Logger logger = Logger.getLogger(ItemHTMLRepresentation.class);
	XFTItem item = null;

	public ItemHTMLRepresentation(XFTItem i,MediaType mt,Request request,XDATUser _user) {
		super(mt,request,_user);
		item=i;
	}
	
	@Override
	public void write(OutputStream out) throws IOException {
		try {
			HttpServletRequest _request = ((ServletCall)((HttpRequest) request).getHttpCall()).getRequest(); 
			HttpServletResponse _response = ((ServletCall)((HttpRequest) request).getHttpCall()).getResponse(); 
			
			RunData data = populateRunData(_request,_response,user);
			TurbineUtils.setDataItem(data, item);
			 
			try {
				if(item.getProperty("project")!=null){
					data.getParameters().setString("project", item.getStringProperty("project"));
				}
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
			
			try {
				String screen =DisplayItemAction.GetReportScreen(item.getItem().getGenericSchemaElement());
            	data.setScreenTemplate(screen);
            	turbineScreen(data,out);
			} catch (ElementNotFoundException e) {
				logger.error("",e);
			}
		} catch (TurbineException e) {
			logger.error("",e);
		} catch (Exception e) {
			logger.error("",e);
		}
	}
	
}
