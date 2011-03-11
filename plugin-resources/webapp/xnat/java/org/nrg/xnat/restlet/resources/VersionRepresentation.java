package org.nrg.xnat.restlet.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.nrg.xft.XFT;
import org.nrg.xnat.restlet.resources.prearchive.PrearcSessionResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionRepresentation extends Resource {
     private final Logger logger = LoggerFactory.getLogger(VersionRepresentation.class);
    
	 public VersionRepresentation(Context context, Request request,
	            Response response) {
	        super(context, request, response);
	 }
	 
	 public Representation getRepresentation(final Variant variant){
		 return new StringRepresentation(getXNATVersion());
	 }

		private String getXNATVersion() {
			final String path = location(XFT.GetConfDir(), "VERSION");
			FileReader fr = null;
			try {
				fr = new FileReader(path);
				return (new BufferedReader(fr)).readLine();
			} catch (Exception e) {
				logger.warn("Issue reading VERSION file", e);
				return "could not retrieve";
			} finally {
				if (fr != null) {
					try {
						fr.close();
					} catch (Exception e) {
						// ignore it
					}
				}
			}
		}

		private String location(String... pathParts) {
			return StringUtils.join(pathParts, File.separator);
		}
}
