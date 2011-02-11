/**
 * 
 */
package org.nrg.xnat.restlet;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.VirtualHost;

/**
 * @author tolsen01
 *
 */
public class XNATComponent extends Component {

	/**
	 * 
	 */
	public XNATComponent() {
		super();
		
		Application app = new XNATApplication(this.getContext());
		
		VirtualHost vhHost = new VirtualHost(this.getContext());
		vhHost.attach("/REST", app);
		vhHost.attach("/data",app);
		//vhHost.attach("*/data/*", XNATApplication.class);
		
		getHosts().add(vhHost);
	}

	
}
