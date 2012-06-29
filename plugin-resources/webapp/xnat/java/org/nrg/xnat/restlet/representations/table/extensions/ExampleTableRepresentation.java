// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations.table.extensions;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xft.XFTTable;
import org.nrg.xnat.restlet.XnatTableRepresentation;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

/**
 * An alternative representation of the XFTTable which can be downloaded via REST.  You can use the specified mediaType in your ACCEPT_HEADER or using the format tag (format=application/x-csv-custom1).
 * 
 * Use this as a template for your own implementations.  They should be placed in projects/{project}/src/java/org/nrg/xnat/restlet/representations/table/extensions or preferably in a module.
 * 
 * When defining a custom mediaType, we highly encourage that you use a pattern like application/x-{something}.  Both mediaType and mediaTypeDescription are required.
 * 
 * @author Tim Olsen <tim@deck5consulting.com>
 */
@XnatTableRepresentation(
		mediaType="application/x-csv-custom1",
		mediaTypeDescription="Custom CSV"
	)
public class ExampleTableRepresentation extends OutputRepresentation {
	XFTTable table = null;
	Hashtable<String,Object> tableProperties = null;
	Map<String,Map<String,String>> cp=new Hashtable<String,Map<String,String>>();
	
	/**
	 * Includes the minimal required fields which are the table to be written and the mediaType to be associated with it.
	 * @param table
	 * @param mediaType
	 */
	public ExampleTableRepresentation(XFTTable table,MediaType mediaType) {
		super(mediaType);
		this.table=table;
	}
	
	/**
	 * Optional constructor.  It includes the other parameters plus a set of column parameters which could be put on the header row.  This might include something like a sorted=true property on the column that was sorted.
	 * 
	 * @param table
	 * @param metaFields
	 * @param mediaType
	 */
	public ExampleTableRepresentation(XFTTable table,Hashtable<String,Object> tableProperties,MediaType mediaType) {
		super(mediaType);
		this.table=table;
		this.tableProperties=tableProperties;
	}
	
	/**
	 * This is the typical constructor used.  It includes the other parameters plus a set of column parameters which could be put on the header row.  This might include something like a sorted=true property on the column that was sorted.
	 * 
	 * @param table
	 * @param columnProperties
	 * @param metaFields
	 * @param mediaType
	 */
	public ExampleTableRepresentation(XFTTable table,Map<String,Map<String,String>> columnProperties,Hashtable<String,Object> tableProperties,MediaType mediaType) {
		super(mediaType);
		this.table=table;
		this.tableProperties=tableProperties;
		if(columnProperties!=null){
			this.cp.putAll(columnProperties);
		}
	}

	/* (non-Javadoc)
	 * This is where the magic happens.  Most likely, this is the only method you should have to modify.  
	 * @see org.restlet.resource.Representation#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		OutputStreamWriter sw = new OutputStreamWriter(os);
		BufferedWriter writer = new BufferedWriter(sw);
				
		//get the column index of a specific field I'm interested in.
		int index1=table.getColumnIndex("ID");
		
		for(Object[] row : table.rows()){
			for(int i=0;i<row.length;i++){
				if(i>0){
					writer.write(",");
				}
				if(row[i]!=null){
					if(i==index1){
						//do something special because it matched our selected index
						writer.write(row[i].toString());
					}else{
						writer.write(row[i].toString());
					}
				}
			}
			writer.write("\n");
		}
		
	    writer.flush();
	}
}