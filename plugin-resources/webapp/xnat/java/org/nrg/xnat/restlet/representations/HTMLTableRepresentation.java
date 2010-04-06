// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xft.XFTTable;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

public class HTMLTableRepresentation extends OutputRepresentation {
	XFTTable table = null;
	Hashtable<String, Object> tableProperties = null;
	Map<String,Map<String,String>> cp=new Hashtable<String,Map<String,String>>();
	boolean allowTD=true;
	
	public HTMLTableRepresentation(XFTTable table,MediaType mediaType,boolean td) {
		super(mediaType);
		this.table=table;
		this.allowTD=td;
	}
	
	public HTMLTableRepresentation(XFTTable table,Hashtable<String, Object> metaFields,MediaType mediaType,boolean td) {
		super(mediaType);
		this.table=table;
		this.tableProperties=metaFields;
		this.allowTD=td;
	}

	public HTMLTableRepresentation(XFTTable table,Map<String,Map<String,String>> columnProperties,Hashtable<String,Object> metaFields,MediaType mediaType,boolean td) {
		super(mediaType);
		this.table=table;
		this.tableProperties=metaFields;
		if(columnProperties!=null)this.cp=columnProperties;
		this.allowTD=td;
	}
	
	@Override
	public void write(OutputStream os) throws IOException {
		OutputStreamWriter sw = new OutputStreamWriter(os);
		BufferedWriter writer = new BufferedWriter(sw);
	    writer.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
	    writer.write("<body>");
	    table.toHTML(allowTD,writer,cp);
	    writer.write("</body></html>");
	    writer.flush();
	}

}
