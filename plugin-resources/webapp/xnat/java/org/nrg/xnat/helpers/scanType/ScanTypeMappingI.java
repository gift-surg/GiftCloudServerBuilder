/*
 * org.nrg.xnat.helpers.scanType.ScanTypeMappingI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.scanType;

import org.nrg.xdat.model.XnatImagescandataI;

public interface ScanTypeMappingI {
    /**
     * Unless the scan type is already set for the provided scan, set it.
     * Reads the scan's series description; if a mapping has already been defined
     * from that description to a scan type, uses that; otherwise, assigns the
     * standardized form (uppercase, no special characters) of the series description.
     * If the series description is null or empty, uses a default value for scan type.
     * @param scan scan in which scan type will be set
     */
	void setType(XnatImagescandataI scan);
}
