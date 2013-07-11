/*
 * org.nrg.dcm.id.ClassicDicomObjectIdentifier
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.dcm.id;

import com.google.common.collect.ImmutableList;
import org.dcm4che2.data.Tag;
import org.nrg.dcm.ContainedAssignmentExtractor;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.TextExtractor;

import java.util.List;
import java.util.regex.Pattern;

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
