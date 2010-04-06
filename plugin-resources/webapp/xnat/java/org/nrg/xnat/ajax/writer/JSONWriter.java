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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.xft.XFTTable;

public class JSONWriter implements ResponseWriterI{

	HttpServletRequest request;
	HttpServletResponse response;
	Hashtable<String, ArrayList<String>> metaFields;
	
	public JSONWriter(HttpServletRequest req, HttpServletResponse resp) {
		this.request = req;
		this.response = resp;
		metaFields = new Hashtable<String,ArrayList<String>>();
	}


	
	public   void setMetaFields(Hashtable<String, ArrayList<String>> metaFields) {
		this.metaFields = metaFields;
	}
	
	public   void write(XFTTable table, String title) throws IOException  {
		ServletOutputStream out = response.getOutputStream();
		OutputStreamWriter sw = new OutputStreamWriter(out);
		BufferedWriter writer = new BufferedWriter(sw);
		response.setContentType("application/json");
		
		writer.write("({\"ResultSet\":{\"Result\":");
		table.toJSON(writer);
		if (metaFields != null  && metaFields.size() > 0) {
			writer.write(", ");
			String appendMeta = "";
			Enumeration<String> keys = metaFields.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				ArrayList<String> value = metaFields.get(key);
				if (value != null) {
					appendMeta += "\"" + key + "\": " ;
					appendMeta += flattenValue(value);
					appendMeta += ",";
				}
			}
			if (appendMeta.endsWith(",")) {
				int i = appendMeta.lastIndexOf(",");
				if (i != -1) {
					appendMeta = appendMeta.substring(0, i);
				}
			}
			writer.write(appendMeta);
		}
		writer.write("}})");
		writer.flush();
		writer.close();
	}
	
	private String flattenValue(ArrayList<String> values) {
		if (values == null) return "\"\"";
		if (values.size() == 1) {
			String rtn = "\"" + values.get(0) + "\"";
			return rtn;
		}
		int lastIndex = values.size() - 1 ; 
		String rtn = "{\"" + values.get(lastIndex - 1) + "\":\"" + values.get(lastIndex) + "\"}";
		for (int i = lastIndex - 2 ; i >= 0; i--) {
			rtn = "{\""  + values.get(i) + "\":" + rtn + "}";
		}
		return rtn;
	}
	
}
