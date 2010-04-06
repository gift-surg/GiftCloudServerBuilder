//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.XnatAddfieldI;
import org.nrg.xdat.om.XnatExperimentdataFieldI;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public class BaseXnatMrassessordata extends org.nrg.xdat.om.base.auto.AutoXnatMrassessordata {

	public BaseXnatMrassessordata(ItemI item)
	{
		super(item);
	}

	public BaseXnatMrassessordata(UserI user)
	{
		super(user);
	}

	public BaseXnatMrassessordata()
	{}

	public BaseXnatMrassessordata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}


	public XnatMrsessiondata getMrSessionData()
	{
	    return (XnatMrsessiondata)this.getImageSessionData();
	}

    Hashtable parametersByName = null;
    public Hashtable getAddParametersByName(){
        if (parametersByName == null){
            parametersByName=new Hashtable();
            Iterator iter = this.getParameters_addparam().iterator();
            while (iter.hasNext()){
                XnatAddfieldI field = (XnatAddfieldI)iter.next();
                parametersByName.put(field.getName(), field);
            }
        }

        return parametersByName;
    }

    public Object getAddParameterByName(String s){
        XnatAddfieldI field = (XnatAddfieldI)getAddParametersByName().get(s);
        if (field!=null){
            return field.getAddfield();
        }else{
            return null;
        }
    }
}
