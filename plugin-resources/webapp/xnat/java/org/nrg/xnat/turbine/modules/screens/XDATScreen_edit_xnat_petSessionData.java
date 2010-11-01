//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Feb 9, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;

public class XDATScreen_edit_xnat_petSessionData extends EditSubjectAssessorScreen {
    private static final float BYTES_PER_MB = 1024*1024;
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_xnat_petSessionData.class);
    public String getElementName() {
        return "xnat:petSessionData";
    }
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context) {
        super.finalProcessing(data,context);
        XnatImagesessiondata session = new XnatImagesessiondata(item);
        final Collection<Map<String,Object>> scanprops = new LinkedList<Map<String,Object>>();
        for (final XnatImagescandataI scan : session.getSortedScans()) {
            long scanSize = 0;
            final Collection<File> files = ((XnatImagescandata)scan).getJavaFiles(session.getPrearchivepath());
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
