/*
 * org.nrg.dcm.id.DbBackedProjectIdentifier
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.dcm.id;


import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

public class DbBackedProjectIdentifier implements DicomProjectIdentifier {
    private final Logger logger = LoggerFactory.getLogger(DbBackedProjectIdentifier.class);
    private final Iterable<DicomDerivedString> extractors;
    private final ImmutableSortedSet<Integer> tags;

    public DbBackedProjectIdentifier(final Iterable<DicomDerivedString> identifiers) {
        this.extractors = Lists.newArrayList(identifiers);
        final ImmutableSortedSet.Builder<Integer> b = ImmutableSortedSet.naturalOrder();
        for (final DicomObjectFunction f : identifiers) {
            b.addAll(f.getTags());
        }
        tags = b.build();
    }
    
    public final XnatProjectdata apply(final XDATUser user, final DicomObject o) {
        for (final DicomDerivedString extractor : extractors) {
            final Object alias = extractor.apply(o);
            if (null != alias && ! "".equals(alias)) {
        	final XnatProjectdata p = XnatProjectdata.getProjectByIDorAlias(alias.toString(), user, false);
                if (null != p && canCreateIn(user, p)) {
                    return p;
                }
            }
        }
        return null;
    }

    private boolean canCreateIn(final XDATUser user, final XnatProjectdata p) {
        try {
            return PrearcUtils.canModify(user, p.getId());
        } catch (Exception e) {
            logger.error("Unable to check permissions for " + user + " in " + p, e);
            return false;
        }
    }

    public final SortedSet<Integer> getTags() { return tags; }
}
