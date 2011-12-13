/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import org.dcm4che2.data.DicomObject;

import com.google.common.base.Function;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface DicomDerivedString extends DicomObjectFunction,Function<DicomObject,String> {
}
