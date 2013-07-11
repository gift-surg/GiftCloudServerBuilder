/*
 * org.nrg.dcm.id.DicomObjectFunction
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.dcm.id;

import java.util.SortedSet;


public interface DicomObjectFunction {
    SortedSet<Integer> getTags();
}
