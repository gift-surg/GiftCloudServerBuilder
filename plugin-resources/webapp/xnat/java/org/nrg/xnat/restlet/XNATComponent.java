/**
 * 
 */
package org.nrg.xnat.restlet;

import org.restlet.Component;

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
		
		getHosts().addAll(XNATRestletFactory.buildVirtualHosts(this.getContext()));
	}
}
