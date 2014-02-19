/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/HtmlAlert.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */

function HtmlAlert(w, title, text) {
	var win = w.open("", title, "height=300,width=500,scrollbars=yes,resizable=yes");
	win.document.write(text);
}