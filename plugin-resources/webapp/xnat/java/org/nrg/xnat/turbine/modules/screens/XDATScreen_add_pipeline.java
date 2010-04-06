/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.turbine.modules.screens.AdminEditScreenA;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;

public class XDATScreen_add_pipeline extends AdminEditScreenA {

	
	
	public String getElementName() {
		return PipePipelinedetails.SCHEMA_ELEMENT_NAME;
	}
	
	public void finalProcessing(RunData data, Context context) {
		
	}
	
	public void doBuildTemplate(RunData data, Context context)      {
		try {
			ArrayList<GenericWrapperElement> elements = GenericWrapperElement.GetAllElements(false);
			ArrayList<GenericWrapperElement> myelements = new ArrayList<GenericWrapperElement>();
			//Remove the datatypes which cannt be created or applied to
			for (int i = 0; i < elements.size(); i++) {
				GenericWrapperElement element = elements.get(i);
				if (!element.getFullXMLName().startsWith("cat:") && !element.getFullXMLName().startsWith("pipe:") && !element.getFullXMLName().startsWith("arc:") && !element.getFullXMLName().startsWith("prov:") && !element.getFullXMLName().startsWith("xdat:") && !element.getFullXMLName().startsWith("wrk:"))  {
					myelements.add(element);
				}
			}
			context.put("elements",myelements);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
