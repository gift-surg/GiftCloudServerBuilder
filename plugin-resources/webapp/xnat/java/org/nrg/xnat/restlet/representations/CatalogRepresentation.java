// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;
import org.xml.sax.SAXException;

public class CatalogRepresentation extends OutputRepresentation {
	static org.apache.log4j.Logger logger = Logger.getLogger(ItemXMLRepresentation.class);
	CatCatalogBean cat = null;
	boolean includeSchemaLocations=true;
	
	public CatalogRepresentation(CatCatalogBean i,MediaType mt,boolean includeSchemaLocations) {
		super(mt);
		cat=i;	
		this.includeSchemaLocations=includeSchemaLocations;
	}

	public CatalogRepresentation(CatCatalogBean i,MediaType mt) {
		super(mt);
		cat=i;	
	}
	
	@Override
	public void write(OutputStream out) throws IOException {
			try {
				PrintWriter pw = new PrintWriter(out);
				cat.toXML(pw, false);
				pw.close();
			} catch (IllegalArgumentException e) {
				logger.error("",e);
			} catch (TransformerFactoryConfigurationError e) {
				logger.error("",e);
			}
	}

	
}
