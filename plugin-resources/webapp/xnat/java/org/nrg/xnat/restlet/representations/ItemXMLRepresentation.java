// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Calendar;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.nrg.xdat.services.XMLSearch;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;
import org.xml.sax.SAXException;

public class ItemXMLRepresentation extends OutputRepresentation {
	static org.apache.log4j.Logger logger = Logger.getLogger(ItemXMLRepresentation.class);
	XFTItem item = null;
	boolean includeSchemaLocations=true;
	private boolean allowDBAccess=true;
	private boolean hidden_fields=true;
	
	public ItemXMLRepresentation(XFTItem i,MediaType mt,boolean includeSchemaLocations,boolean writeHiddenFields) {
		super(mt);
		item=i;	
		this.includeSchemaLocations=includeSchemaLocations;
		hidden_fields=writeHiddenFields;
	}

	public ItemXMLRepresentation(XFTItem i,MediaType mt) {
		super(mt);
		item=i;	
	}
	
	public void setAllowDBAccess(boolean b){
		this.allowDBAccess=b;
	}
	
	@Override
	public void write(OutputStream out) throws IOException {
			try {
				SAXWriter writer = new SAXWriter(out,this.allowDBAccess);
				if(includeSchemaLocations){
					writer.setAllowSchemaLocation(true);
					writer.setLocation(TurbineUtils.GetFullServerPath() + "/" + "schemas/");
				}
				writer.setWriteHiddenFields(hidden_fields);
				writer.write(item);
			} catch (TransformerConfigurationException e) {
				logger.error("",e);
			} catch (IllegalArgumentException e) {
				logger.error("",e);
			} catch (TransformerFactoryConfigurationError e) {
				logger.error("",e);
			} catch (FieldNotFoundException e) {
				logger.error("",e);
			} catch (SAXException e) {
				logger.error("",e);
			}
	}

	
}
