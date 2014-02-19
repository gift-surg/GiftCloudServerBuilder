/*
 * org.nrg.dcm.id.DbBackedProjectIdentifier
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/5/13 2:38 PM
 */
package org.nrg.dcm.id;


import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.GradualDicomImporter;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

public class DbBackedProjectIdentifier implements DicomProjectIdentifier {
    private final Logger logger = LoggerFactory.getLogger(DbBackedProjectIdentifier.class);
    private final Iterable<DicomDerivedString> extractors;
    private final ImmutableSortedSet<Integer> tags;
    private Cache projectCache = null;

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
            	//added caching here to prevent duplicate project queries in every file transaction
            	//the cache is shared with the one in gradual dicom importer, which does a similar query.
            	if (null == projectCache) {
                    projectCache=GradualDicomImporter.getUserProjectCache(user);
                }
                if (null != alias) {
                    final Element pe = projectCache.get(alias);
                    if (null != pe) {
                        return (XnatProjectdata)pe.getValue();
                    } else {
		            	final XnatProjectdata p = XnatProjectdata.getProjectByIDorAlias(alias.toString(), user, false);
		            	if (null != p && canCreateIn(user,p)) {
		                    projectCache.put(new Element(alias, p));
		                    return p;
		            	}else{
		            		projectCache.put(new Element(alias, null));
		            	}
                    }
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
