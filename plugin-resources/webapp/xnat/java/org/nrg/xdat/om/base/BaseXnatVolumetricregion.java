//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.nrg.xdat.om.XnatVolumetricregionSubregion;
import org.nrg.xdat.om.base.auto.AutoXnatVolumetricregion;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatVolumetricregion extends AutoXnatVolumetricregion {

	public BaseXnatVolumetricregion(ItemI item)
	{
		super(item);
	}

	public BaseXnatVolumetricregion(UserI user)
	{
		super(user);
	}

	public BaseXnatVolumetricregion()
	{}

	public BaseXnatVolumetricregion(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public BaseXnatVolumetricregionSubregion getSubRegionByName(String name)
    {
        List al = this.getSubregions_subregion();
        Iterator iter = al.iterator();
        while (iter.hasNext())
        {
            XnatVolumetricregionSubregion sub = (XnatVolumetricregionSubregion)iter.next();
            if (sub.getName().equalsIgnoreCase(name))
            {
                return sub;
            }
        }
        
        return null;
    }
}
