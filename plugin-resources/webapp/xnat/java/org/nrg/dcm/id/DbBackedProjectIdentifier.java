/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;


import java.util.SortedSet;

import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
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
                final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(alias, user, false);
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
