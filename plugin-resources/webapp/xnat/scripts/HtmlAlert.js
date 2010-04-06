// $Id: HtmlAlert.js,v 1.4 2010/03/30 20:05:47 timo Exp $
// Copyright (c) 2008 Washington University School of Medicine
// Author: Kevin A. Archie <karchie@npg.wustl.edu>
//
// Creates a new window to display an HTML message

function HtmlAlert(w, title, text) {
	var win = w.open("", title, "height=300,width=500,scrollbars=yes,resizable=yes");
	win.document.write(text);
}