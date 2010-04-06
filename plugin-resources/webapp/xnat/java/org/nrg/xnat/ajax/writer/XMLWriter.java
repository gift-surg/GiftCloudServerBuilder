/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.ajax.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.xft.XFTTable;

public class XMLWriter implements ResponseWriterI {

	HttpServletRequest request;
	HttpServletResponse response;
	Hashtable<String, ArrayList<String>> metaFields;

	public XMLWriter(HttpServletRequest req, HttpServletResponse resp) {
		this.request = req;
		this.response = resp;
		metaFields = new Hashtable<String,ArrayList<String>>();
	}
	
	public   void setMetaFields(Hashtable<String, ArrayList<String>> metaFields) {
	}
	
	public   void write(XFTTable table, String title) throws IOException  {
		ServletOutputStream out = response.getOutputStream();
		OutputStreamWriter sw = new OutputStreamWriter(out);
		BufferedWriter writer = new BufferedWriter(sw);
		response.setContentType("text/xml");
		table.toXMLList(writer, title);
		writer.flush();
		writer.close();
	}
}
