// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 28 15:02:06 CST 2007
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatFielddefinitiongroupField extends AutoXnatFielddefinitiongroupField {

	public BaseXnatFielddefinitiongroupField(ItemI item)
	{
		super(item);
	}

	public BaseXnatFielddefinitiongroupField(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatFielddefinitiongroupField(UserI user)
	 **/
	public BaseXnatFielddefinitiongroupField()
	{}

	public BaseXnatFielddefinitiongroupField(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public String getCleanedXMLPath(){
        String xmlPath = this.getXmlpath();
        while(xmlPath.indexOf("[")>-1){
            xmlPath= xmlPath.substring(0,xmlPath.indexOf("[")) + xmlPath.substring(xmlPath.indexOf("]")+1);
        }
        return xmlPath;
    }
}
