package org.nrg.xnat.comparators;

import java.util.Comparator;

import org.nrg.xdat.model.XnatImagescandataI;

public class CompareImageScanBySeriesUID implements Comparator<XnatImagescandataI> {

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
        
        return value1.getUid().compareTo(value2.getUid());
	}


}
