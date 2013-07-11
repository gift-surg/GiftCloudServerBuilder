/*
 * org.nrg.xnat.restlet.XNATVirtualHost
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.restlet;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.VirtualHost;

public class XNATVirtualHost extends VirtualHost {

	public XNATVirtualHost(Context parentContext) {
		super(parentContext);
		
		attachApplications();
	}
	
	public void attachApplications(){
		Application app=XNATRestletFactory.buildDefaultApplication(this.getContext().createChildContext());
		
		attach("/REST", app);
		attach("/data",app);
	}

}
