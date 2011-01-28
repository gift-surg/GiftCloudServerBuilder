package org.nrg.xnat.comparators;

import java.util.Comparator;

import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.base.BaseXnatImagescandata;


public class CompareImageScanByID implements Comparator<XnatImagescandataI>{

	@Override
	public int compare(XnatImagescandataI value1, XnatImagescandataI value2) {
		if (value1 == null){
            if (value2 == null)
            {
                return 0;
            }else{
                return -1;
            }
        }
        if (value2== null)
        {
        	return 1;
	    }
        
        return value1.getId().compareTo(value2.getId());
	}
}
