/*
 * org.nrg.xdat.om.base.BaseXnatSubjectassessordata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatSubjectassessordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.DateUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatSubjectassessordata extends AutoXnatSubjectassessordata {

	public BaseXnatSubjectassessordata(ItemI item)
	{
		super(item);
	}

	public BaseXnatSubjectassessordata(UserI user)
	{
		super(user);
	}

	public BaseXnatSubjectassessordata()
	{}

	public BaseXnatSubjectassessordata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    private XnatSubjectdata subject = null;
	public XnatSubjectdata getSubjectData()
	{
	    if (subject==null)
	    {
            if (getSubjectId()!=null)
            {
                ArrayList al = XnatSubjectdata.getXnatSubjectdatasByField("xnat:subjectData/ID",this.getSubjectId(),this.getUser(),false);
                if (al.size()>0)
                {
                    subject = (XnatSubjectdata)al.get(0);
                }
            }
	    }
	    return subject;
	}


    public String getSubjectAge() {
        if (this.getAge()!=null){
            Double d = getAge();
            NumberFormat formatter = NumberFormat.getInstance();
            formatter.setGroupingUsed(false);
            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(2);
            return formatter.format(d);
        }

        XnatSubjectdata s = this.getSubjectData();
        if (s == null) {
            return "--";
        } else {
            try {
                Object o = this.getDate();
                if (o instanceof String)
                {
                    Date expt_date = DateUtils.parseDateTime((String)o);
                    return s.getAge(expt_date);
                }else{
                    Date expt_date = (Date) this.getDate();
                    return s.getAge(expt_date);
                }
            } catch (Exception e) {
                logger.error("", e);
                if (s.getAge()!=null)
                {
                    return s.getAge().toString();
                }else
                    return "--";

            }
        }
    }

	@Override
	public void preSave() throws Exception{
		super.preSave();

		if(this.getSubjectData()==null){
			throw new Exception("Unable to identify subject for:" + this.getSubjectId());
		}
	}
}
