package org.nrg.xnat.restlet;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.VirtualHost;

public class XNATVirtualHost extends VirtualHost {

	public XNATVirtualHost(Context parentContext) {
		super(parentContext);
		
		attachApplications(parentContext);
	}
	
	public void attachApplications(Context parentContext){
		Application app=XNATRestletFactory.buildDefaultApplication(parentContext);
		
		attach("/REST", app);
		attach("/data",app);
	}

}
