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
