
/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/AjaxItemSearch.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
dynamicJSLoad("SAXDriver","xmlsax-min.js");
	
dynamicJSLoad("SAXEventHandler","SAXEventHandler-min.js");

function realtimeXMLLoad(xmlSearch,allowChildren,ajax_servlet_path){
	
	  if (window.XMLHttpRequest) {
	    var listreq = new XMLHttpRequest();
	  } else if (window.ActiveXObject) {
	    var listreq = new ActiveXObject("Microsoft.XMLHTTP");
	  }
	  
	 var url = ajax_servlet_path + "?remote-class=org.nrg.xdat.ajax.XMLSearch";
     url = url + "&remote-method=execute";
     url = url + "&search="+xmlSearch;
     url = url + "&allowMultiples="+allowChildren;
	  
	 listreq.open("GET", url, false);
	 listreq.send(null);
	 
	 if (listreq!==false) {     
		if (listreq.status==200) {        
			// eval the code in the global space (man this has cost me time to figure out how to do it grrr)
			var arr,src='',parser = new SAXDriver();
			var handler = new SAXEventHandler();
			
			parser.setDocumentHandler(handler);
			parser.setErrorHandler(handler);
			parser.setLexicalHandler(handler);
			
			parser.parse(listreq.responseText);// start parsing                        
			
			if (handler.items.length>0){
				return handler.items;
			}else{
				return handler.root;
			}
		}
		
	 }

	 return null;
}