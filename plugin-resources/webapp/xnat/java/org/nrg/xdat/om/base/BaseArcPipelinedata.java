// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.base.auto.AutoArcPipelinedata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseArcPipelinedata extends AutoArcPipelinedata {

	public BaseArcPipelinedata(ItemI item)
	{
		super(item);
	}

	public BaseArcPipelinedata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcPipelinedata(UserI user)
	 **/
	public BaseArcPipelinedata()
	{}

	public BaseArcPipelinedata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

	   public String getCommand(XFTItem itemOfExpectedXsiType) throws ElementNotFoundException, FieldNotFoundException, XFTInitException   {
	    	String rtn = "";
	    	if (getLocation() != null) {
	    		rtn = " -pipeline " +  getLocation()  + " " ;
	    	}
	    	//rtn +=  getName();
	    	ArrayList<ArcPipelineparameterdata> parameters = this.getParameters_parameter();
	    	for (int i = 0; i < parameters.size(); i++) {
	    		ArcPipelineparameterdata param = parameters.get(i);
	    		String schemaLink = param.getSchemalink();
	    		String values = null;
	    		if (schemaLink != null) {
	    			Object o = itemOfExpectedXsiType.getProperty(schemaLink, true);
	    			if (o != null && values == null ) values = "";
	    			try {
	        			ArrayList<XFTItem>  matches = (ArrayList<XFTItem>) o;
	    				for (int j = 0; j < matches.size(); j++) {
	        				values += matches.get(j) + ",";
	        			}
	    			}catch(ClassCastException  cce) {
	    				values += o + ",";
	    			}
	    			if (values != null && values.endsWith(",")) {
	    				values = values.substring(0, values.length() -1);
	    			}
	    		}else {
	    			values = param.getCsvvalues();
	    		}
	    		if (values != null) {
	    			rtn += " -parameter " + param.getName() + "=" + values;
	    		}
	    	}
	    	return rtn;
	    }
	
    public boolean hasCutomwebpage() {
        boolean rtn = false;
        if (getCustomwebpage() !=null)
            rtn = true;
        return rtn;
    }
}
