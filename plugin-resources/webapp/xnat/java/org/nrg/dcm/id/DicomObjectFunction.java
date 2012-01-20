/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import java.util.SortedSet;


/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface DicomObjectFunction {
    SortedSet<Integer> getTags();
}
