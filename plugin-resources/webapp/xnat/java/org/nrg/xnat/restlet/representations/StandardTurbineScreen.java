/**
 * 
 */
package org.nrg.xnat.restlet.representations;

import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.nrg.xdat.security.XDATUser;
import org.restlet.data.MediaType;
import org.restlet.data.Request;

/**
 * @author tolsen01
 *
 */
public class StandardTurbineScreen extends TurbineScreenRepresentation {
	final String screen;
	/**
	 * @param mediaType
	 * @param request
	 * @param _user
	 * @throws TurbineException
	 */
	public StandardTurbineScreen(MediaType mediaType, Request request,
			XDATUser _user, String screen, Map<String,String> data_props) throws TurbineException {
		super(mediaType, request, _user);
		this.screen=screen;
		for(Map.Entry<String,String> entry: data_props.entrySet()){
			data.getParameters().setString(entry.getKey(), entry.getValue());
		}
	}
	
	public RunData getData(){
		return data;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.representations.TurbineScreenRepresentation#getScreen()
	 */
	@Override
	public String getScreen() {
		return screen;
	}

}
