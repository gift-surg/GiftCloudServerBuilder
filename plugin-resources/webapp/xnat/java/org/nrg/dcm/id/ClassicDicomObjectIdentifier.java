/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import java.util.SortedSet;
import java.util.regex.Pattern;

import javax.inject.Provider;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.dcm.ChainExtractor;
import org.nrg.dcm.ContainedAssignmentExtractor;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.TextExtractor;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.Labels;

import com.google.common.collect.ImmutableSortedSet;

/**
 * DicomObjectIdentifier that implements the standard XNAT 1.4/1.5 object identification
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class ClassicDicomObjectIdentifier implements
        DicomObjectIdentifier<XnatProjectdata> {
    private static final DicomProjectIdentifier projectID = new Xnat15DicomProjectIdentifier();
    
    private static final Extractor aaExtractor = new ChainExtractor(
            new ContainedAssignmentExtractor(Tag.PatientComments, "AA", Pattern.CASE_INSENSITIVE),
            new ContainedAssignmentExtractor(Tag.StudyComments, "AA", Pattern.CASE_INSENSITIVE));

    private static final Extractor sessionExtractor = new ChainExtractor(
            new ContainedAssignmentExtractor(Tag.PatientComments, "Session", Pattern.CASE_INSENSITIVE),
            new ContainedAssignmentExtractor(Tag.StudyComments, "Session", Pattern.CASE_INSENSITIVE),
            new TextExtractor(Tag.PatientID));

    private static final Extractor subjectExtractor = new ChainExtractor(
            new ContainedAssignmentExtractor(Tag.PatientComments, "Subject", Pattern.CASE_INSENSITIVE),
            new ContainedAssignmentExtractor(Tag.StudyComments, "Subject", Pattern.CASE_INSENSITIVE),
            new TextExtractor(Tag.PatientName));

    private static SortedSet<Integer> tags;
    static {
        ImmutableSortedSet.Builder<Integer> builder = ImmutableSortedSet.naturalOrder();
        builder.addAll(projectID.getTags());
        builder.addAll(aaExtractor.getTags());
        builder.addAll(sessionExtractor.getTags());
        builder.addAll(subjectExtractor.getTags());
        tags = builder.build();
    }
    
    private XDATUser user;
    
    public XnatProjectdata getProject(final DicomObject o) {
        return projectID.apply(user, o);
    }
    
    public String getSessionLabel(final DicomObject o) {
        return Labels.toLabelChars(sessionExtractor.extract(o));
    }
    
    public String getSubjectLabel(final DicomObject o) {
        return Labels.toLabelChars(subjectExtractor.extract(o));
    }

    public SortedSet<Integer> getTags() {
        return tags;
    }
    
    private static final Pattern TRUE = Pattern.compile("t(?:rue)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE = Pattern.compile("f(?:alse)?", Pattern.CASE_INSENSITIVE);

    public Boolean requestsAutoarchive(DicomObject o) {
        final String aa = aaExtractor.extract(o);
        if (null == aa) {
            return null;
        } else if (TRUE.matcher(aa).matches()) {
            return true;
        } else if (FALSE.matcher(aa).matches()) {
            return false;
        } else {
            return null;
        }
    }
    
    public void setUser(final XDATUser user) {
        this.user = user;
    }
    
    public void setUserProvider(final Provider<XDATUser> user) {
        this.user = user.get();
    }
}
