/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import java.util.List;
import java.util.regex.Pattern;

import org.dcm4che2.data.Tag;
import org.nrg.dcm.ContainedAssignmentExtractor;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.TextExtractor;

import com.google.common.collect.ImmutableList;

/**
 * DicomObjectIdentifier that implements the standard XNAT 1.4/1.5 object identification
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class ClassicDicomObjectIdentifier extends CompositeDicomObjectIdentifier {
    private static final ImmutableList<Extractor> aaExtractors, sessionExtractors, subjectExtractors;
    static {
        final ImmutableList.Builder<Extractor> aab = new ImmutableList.Builder<Extractor>();
        aab.add(new ContainedAssignmentExtractor(Tag.PatientComments, "AA", Pattern.CASE_INSENSITIVE),
                new ContainedAssignmentExtractor(Tag.StudyComments, "AA", Pattern.CASE_INSENSITIVE));
        aaExtractors = aab.build();

        final ImmutableList.Builder<Extractor> sessb = new ImmutableList.Builder<Extractor>();
        sessb.add(new ContainedAssignmentExtractor(Tag.PatientComments, "Session", Pattern.CASE_INSENSITIVE),
                new ContainedAssignmentExtractor(Tag.StudyComments, "Session", Pattern.CASE_INSENSITIVE),
                new TextExtractor(Tag.PatientID));
        sessionExtractors = sessb.build();

        final ImmutableList.Builder<Extractor> subjb = new ImmutableList.Builder<Extractor>();
        subjb.add(new ContainedAssignmentExtractor(Tag.PatientComments, "Subject", Pattern.CASE_INSENSITIVE),
                new ContainedAssignmentExtractor(Tag.StudyComments, "Subject", Pattern.CASE_INSENSITIVE),
                new TextExtractor(Tag.PatientName));
        subjectExtractors = subjb.build();
    }

    public static final List<Extractor> getAAExtractors() { return aaExtractors; }
    public static final List<Extractor> getSessionExtractors() { return sessionExtractors; }
    public static final List<Extractor> getSubjectExtractors() { return subjectExtractors; }

    public ClassicDicomObjectIdentifier() {
        super(new Xnat15DicomProjectIdentifier(), subjectExtractors, sessionExtractors, aaExtractors);
    }
}
