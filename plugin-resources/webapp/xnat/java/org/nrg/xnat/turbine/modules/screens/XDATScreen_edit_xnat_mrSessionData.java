/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_edit_xnat_mrSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Tim
 *
 */
public class XDATScreen_edit_xnat_mrSessionData extends EditSubjectAssessorScreen {
    private static final float BYTES_PER_MB = 1024*1024;
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_xnat_mrSessionData.class);
    /**
     * 
     */
    public XDATScreen_edit_xnat_mrSessionData() {
        super();
    }
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
     */
    public String getElementName() {
        return "xnat:mrSessionData";
    }
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context) {
        super.finalProcessing(data,context);
        XnatImagesessiondata session = new XnatImagesessiondata(item);

        String rootPath;
		try {
			rootPath =session.getArchivePath();
		} catch (UnknownPrimaryProjectException e) {
			rootPath=null;
		}
        
        final Collection<Map<String,Object>> scanprops = new LinkedList<Map<String,Object>>();
        for (final XnatImagescandataI scan : session.getSortedScans()) {
            long scanSize = 0;
            final Collection<File> files = ((XnatImagescandata)scan).getJavaFiles(rootPath);
            for (final File file : files) {
        	scanSize += file.length();
            }
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put("files", (long)files.size());
            props.put("size", String.format("%.1f", scanSize/BYTES_PER_MB));
            scanprops.add(props);
        }
        context.put("scanprops", scanprops);
    }
}
