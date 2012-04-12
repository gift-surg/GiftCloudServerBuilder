/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import java.util.regex.Pattern;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class ContainedAssignmentDicomIdentifier extends DelegateDicomIdentifier {
    private final static String START = "(?:\\A|(?:.*[\\s,;]))", OPTWS = "\\s*", END = "(?:(?:[\\s,;].*\\Z)|\\Z)",
    START_GROUP = "(", END_GROUP = ")";
    private final static String DEFAULT_VALUE_PATTERN = "[\\w\\-]*";
    private final static String DEFAULT_OP = "\\:";

    public ContainedAssignmentDicomIdentifier(final int tag,
            final String id, final String op, final String valuePattern,
            int patternFlags) {
        super(new PatternDicomIdentifier(tag,
                Pattern.compile(new StringBuilder(START)
                .append(id)
                .append(OPTWS).append(op).append(OPTWS)
                .append(START_GROUP).append(valuePattern).append(END_GROUP)
                .append(END)
                .toString(), patternFlags), 1));
    }

    public ContainedAssignmentDicomIdentifier(final int tag, final String id, final String op, final int patternFlags) {
        this(tag, id, op, DEFAULT_VALUE_PATTERN, patternFlags);
    }

    public ContainedAssignmentDicomIdentifier(final int tag, final String id, final String op) {
        this(tag, id, op, 0);
    }

    public ContainedAssignmentDicomIdentifier(final int tag, final String id, final int patternFlags) {
        this(tag, id, DEFAULT_OP, DEFAULT_VALUE_PATTERN, patternFlags);
    }

    public ContainedAssignmentDicomIdentifier(final int tag, final String id) {
        this(tag, id, 0);
    }
}
