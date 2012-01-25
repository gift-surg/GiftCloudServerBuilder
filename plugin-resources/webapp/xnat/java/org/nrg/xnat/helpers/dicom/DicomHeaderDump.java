package org.nrg.xnat.helpers.dicom;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.DicomObjectToStringParam;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.util.TagUtils;
import org.nrg.xft.XFTTable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
/**
 * Class that marshals the header of a DICOM file into an XFTTable. 
 * Currently only two levels of nesting are supported, meaning that top level
 * Sequence elements are supported but not nested ones.
 * 
 * @author aditya
 *
 */
public final class DicomHeaderDump {
	final String file; // path to the DICOM file
	// columns of the XFTTable
	final String[] columns = {
			"tag1",  // tag name, never empty.
			"tag2",  // for normal, non-sequence DICOM tags this is the empty string.
			"vr",   // DICOM Value Representation  
			"value", // Contents of the tag
			"desc"   // Description of the tag
	};
	
	/**
	 * @param file Path to the DICOM file
	 */
	DicomHeaderDump(String file) {
		this.file = file;
	}
	
	/**
	 * Read the header of the DICOM file ignoring the pixel data.
	 * @param f 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	DicomObject getHeader(File f) throws IOException, FileNotFoundException {
		StopTagInputHandler stopBeforePixelData = new StopTagInputHandler(Tag.PixelData);
		DicomInputStream dis = new DicomInputStream(f);
		dis.setHandler(stopBeforePixelData);
		DicomObject o = dis.readDicomObject();
		return o;
	}
	
	/**
	 * Convert a tag into a row of the XFTTable.
	 * @param o Necessary so we can get to the description of the tag
	 * @param e The current DICOM element
	 * @param parentTag If non null, this is a nested DICOM tag. 
	 * @param maxLen The maximum number of characters to read from the description and value 
	 * @return
	 */
	String[] makeRow(DicomObject o, DicomElement e, String parentTag , int maxLen) {
		String tag = TagUtils.toString(e.tag());
		String value = "";

		// If this element has nested tags it doesn't have a value and trying to 
		// extract one using dcm4che will result in an UnsupportedOperationException 
		// so check first.
		if (!e.hasDicomObjects()) {
			value = e.getValueAsString(null, maxLen);	
		}
		else {
			value = "";
		} 
		
		String vr = e.vr().toString();
		String desc = o.nameOf(e.tag());
		List<String> l = new ArrayList<String>();
		if (parentTag == null) {
			String[] _s = {tag,"",vr,value,desc};
			l.addAll(Arrays.asList(_s));
		}
		else {
			String[] _s = {parentTag, tag, vr, value, desc};
			l.addAll(Arrays.asList(_s));
		}
		String[] row = l.toArray(new String[l.size()]);
		return row;
	}
	
	/**
	 * Render the DICOM header to an XFTTable supporting one level of tag nesting. 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public XFTTable render () throws IOException, FileNotFoundException {
		XFTTable t = new XFTTable();
		t.initTable(columns);
		if (this.file == null) {
			return t;
		}

		DicomObject header = this.getHeader(new File(this.file));
		DicomObjectToStringParam formatParams = DicomObjectToStringParam.getDefaultParam();

		for (Iterator<DicomElement> it = header.iterator(); it.hasNext();) {
			DicomElement e = it.next();
			if (!e.hasDicomObjects()){
				t.insertRow(makeRow(header, e, null, formatParams.valueLength));		
			}
			else {
				for (int i = 0; i < e.countItems(); i++) {
					DicomObject o = e.getDicomObject(i);
					t.insertRow(makeRow(header, e, TagUtils.toString(e.tag()), formatParams.valueLength));
					for (Iterator<DicomElement> it1 = o.iterator(); it1.hasNext();) {
						DicomElement e1 = it1.next();
						t.insertRow(makeRow(header, e1, TagUtils.toString(e.tag()), formatParams.valueLength));
					}
				}
			}
		}
		return t;
	}
}
