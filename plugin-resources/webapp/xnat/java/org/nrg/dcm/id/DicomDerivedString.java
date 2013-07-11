/*
 * org.nrg.dcm.id.DicomDerivedString
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.dcm.id;

import com.google.common.base.Function;
import org.dcm4che2.data.DicomObject;

public interface DicomDerivedString extends DicomObjectFunction,Function<DicomObject,String> {
}
