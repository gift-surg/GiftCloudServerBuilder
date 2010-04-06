/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.ajax.writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WriterFactory {
	
	public static ResponseWriterI getWriter(HttpServletRequest request, HttpServletResponse response) {
		String format = request.getParameter("format");
			if(format!=null && format.equals("xml_results")){
				return new XMLWriter(request,response);
			}else if(format!=null && format.equals("json")){
				return new JSONWriter(request,response);
			}else{
				return new HTMLWriter(request,response);
			}
	}
}	
