package org.nrg.xnat.actions.postArchive;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.archive.PrearcSessionArchiver;

import java.util.Map;

/**
 * ClearStudyRoutingAction class.
 *
 * @author Rick Herrick <rick.herrick@wustl.edu> on 10/28/2014.
 */
@SuppressWarnings("unused")
public class ClearStudyRoutingAction implements PrearcSessionArchiver.PostArchiveAction {
    @Override
    public Boolean execute(final XDATUser user, final XnatImagesessiondata src, final Map<String, Object> params) {
        final String studyInstanceUid = src.getUid();
        return StringUtils.isNotBlank(studyInstanceUid) && XDAT.getContextService().getBean(StudyRoutingService.class).close(studyInstanceUid);
    }
}
