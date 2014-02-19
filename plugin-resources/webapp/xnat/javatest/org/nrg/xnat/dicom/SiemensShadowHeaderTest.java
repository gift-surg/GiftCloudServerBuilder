/*
 * org.nrg.xnat.dicom.SiemensShadowHeaderTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.dicom;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.junit.Test;
import org.nrg.dcm.DicomUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.helpers.dicom.SiemensShadowHeader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public final class SiemensShadowHeaderTest extends DicomDataUsingTestCase {
    private static final String[] columns = {
        "tag",  // tag name, never empty.
        "subtag",  // for normal, non-sequence DICOM tags this is the empty string.
        "vr",   // DICOM Value Representation  
        "value", // Contents of the tag
        "description"   // Description of the tag
    };

    private DicomObject getOneObject() {
        for (final File f : this) {
            DicomObject o;
            try {
                o = DicomUtils.read(f, Tag.PixelData);
                if (null != o) {
                    return o;
                }
            } catch (IOException skip) {}
        }
        return null;
    }

    @Test
    public void testImageInstanceShadowHeader() throws IOException {
        XFTTable table = new XFTTable();
        table.initTable(columns);
        table.resetRowCursor();
        assertFalse(table.hasMoreRows());

        final DicomObject o = getOneObject();
        if (null == o) {
            fail("Could not find a valid DICOM test object");
        }

        SiemensShadowHeader.addRows(table, o, o.get(0x00291010), Collections.<String>emptySet());
        table.resetRowCursor();
        assertTrue(table.hasMoreRows());
        assertEquals(80, table.getNumRows());
        final Set<String> fields = Sets.newHashSet();
        for (int i = 0; table.hasMoreRows(); i++) {
            final Object[] row = table.nextRow();
            assertEquals("(0029,1010)", row[0]);
            fields.add((String)row[1]);
        }
        assertEquals(table.getNumRows(), fields.size());
    }

    @Test
    public void testImageInstanceShadowHeaderSelectedFields() throws IOException {
        XFTTable table = new XFTTable();
        table.initTable(columns);
        table.resetRowCursor();
        assertFalse(table.hasMoreRows());
        
        final DicomObject o = getOneObject();
        if (null == o) {
            fail("Could not find a valid DICOM test object");
        }

        final Set<String> fields = ImmutableSet.of("SlicePosition_PCS", "MultistepIndex");
        
        SiemensShadowHeader.addRows(table, o, o.get(0x00291010), fields);
        table.resetRowCursor();
        assertTrue(table.hasMoreRows());
        assertEquals(fields.size(), table.getNumRows());
        assertTrue(fields.contains(table.nextRow()[1]));
        assertTrue(fields.contains(table.nextRow()[1]));
        assertFalse(table.hasMoreRows());
    }
}
