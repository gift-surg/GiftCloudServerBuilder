/*
 * org.nrg.dcm.id.CompositeDicomObjectIdentifier
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.dcm.id;

import org.nrg.dcm.Extractor;

public class ScriptedCompositeDicomObjectIdentifier extends CompositeDicomObjectIdentifier {

//    private static final ImmutableList<Extractor> _sessionExtractors;
//
//    static {
//        final ImmutableList.Builder<Extractor> sessb = new ImmutableList.Builder<Extractor>();
//        final Extractor[] extractors = {new ContainedAssignmentExtractor(Tag.PatientComments, "Session", Pattern.CASE_INSENSITIVE),
//                new ContainedAssignmentExtractor(Tag.StudyComments, "Session", Pattern.CASE_INSENSITIVE),
//                new TextExtractor(Tag.PatientID)};
//        final List<Integer> tags = Arrays.asList(Tag.Modality, Tag.SeriesDescription, Tag.SeriesDescriptionCodeSequence, Tag.PatientComments, Tag.StudyComments, Tag.PatientName);
//        sessb.add(new ScriptedSessionAssignmentExtractor("splitPetMrSession", tags, extractors));
//        _sessionExtractors = sessb.build();
//    }
//
//    public static List<Extractor> getSessionExtractors() {
//        return _sessionExtractors;
//    }

    public ScriptedCompositeDicomObjectIdentifier(final DicomProjectIdentifier projectID,
                                                  final Iterable<Extractor> subjectExtractors,
                                                  final Iterable<Extractor> sessionExtractors,
                                                  final Iterable<Extractor> aaExtractors) {
        super(projectID, subjectExtractors, sessionExtractors, aaExtractors);
        for (final Extractor extractor : sessionExtractors) {
            if (extractor instanceof ScriptedSessionAssignmentExtractor) {
                ((ScriptedSessionAssignmentExtractor) extractor).setIdentifier(this);
            }
        }
    }
}
