/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import java.util.SortedSet;

import org.dcm4che2.data.DicomObject;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class DelegateDicomIdentifier implements DicomDerivedString {
    private final DicomDerivedString identifier;
    
    public DelegateDicomIdentifier(DicomDerivedString identifier) {
        this.identifier = identifier;
    }
    
    /* (non-Javadoc)
     * @see org.nrg.dcm.id.DicomObjectFunction#getTags()
     */
    public final SortedSet<Integer> getTags() { return identifier.getTags(); }

    /* (non-Javadoc)
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    public final String apply(DicomObject o) { return identifier.apply(o); }
}
