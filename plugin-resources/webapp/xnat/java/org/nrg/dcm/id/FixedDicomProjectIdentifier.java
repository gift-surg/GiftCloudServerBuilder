/*
 * org.nrg.dcm.id.FixedDicomProjectIdentifier
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.dcm.id;

import com.google.common.collect.ImmutableSortedSet;
import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;

import java.util.SortedSet;

public final class FixedDicomProjectIdentifier implements DicomProjectIdentifier {
    private static final ImmutableSortedSet<Integer> tags = ImmutableSortedSet.of();
    private final String name;
    private XnatProjectdata project;
    
    public FixedDicomProjectIdentifier(final XnatProjectdata project) {
        this.project = project;
        this.name = project.getName();
    }
    
    public FixedDicomProjectIdentifier(final String name) {
	this.project = null;
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.nrg.dcm.id.DicomObjectFunction#getTags()
     */
    @Override
    public SortedSet<Integer> getTags() { return tags; }

    /* (non-Javadoc)
     * @see org.nrg.dcm.id.DicomProjectIdentifier#apply(org.nrg.xdat.security.XDATUser, org.dcm4che2.data.DicomObject)
     */
    @Override
    public XnatProjectdata apply(final XDATUser user, final DicomObject o) {
	if (null == project) {
	    project = XnatProjectdata.getProjectByIDorAlias(name, user, false);
	}
	return project;
    }
}
