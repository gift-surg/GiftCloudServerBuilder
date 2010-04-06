/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.ajax.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xft.XFTTable;

public interface  ResponseWriterI {
	
	public  void write(XFTTable table,  String title) throws IOException; 
	
	public   void setMetaFields(Hashtable<String, ArrayList<String>> meta) ;
}
