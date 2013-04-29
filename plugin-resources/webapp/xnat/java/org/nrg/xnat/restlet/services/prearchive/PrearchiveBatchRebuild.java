/**
 * 
 */
package org.nrg.xnat.restlet.services.prearchive;

import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.xdat.XDAT;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.*;
import org.nrg.xnat.restlet.services.Importer;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PrearchiveBatchRebuild extends BatchPrearchiveActionsA {
    static Logger logger = Logger.getLogger(PrearchiveBatchRebuild.class);

	public PrearchiveBatchRebuild(Context context, Request request, Response response) {
		super(context, request, response);
				
	}

	@Override
	public void handlePost() {
		try {
			loadBodyVariables();

			//maintain parameters
			loadQueryVariables();
		} catch (ClientException e) {
			this.getResponse().setStatus(e.getStatus(),e);
			return;
		}

        final List<SessionDataTriple> ss=new ArrayList<SessionDataTriple>();
		
		for(final String src:srcs){
            File sessionDir = null;
            try {
                SessionDataTriple s=buildSessionDataTriple(src);
                ss.add(s);
                sessionDir = PrearcUtils.getPrearcSessionDir(user, s.getProject(), s.getTimestamp(), s.getFolderName(), false);

                if (PrearcDatabase.setStatus(s.getFolderName(), s.getTimestamp(), s.getProject(), PrearcUtils.PrearcStatus.QUEUED)) {
                    SessionData sess = new SessionData();
                    sess.setTimestamp(s.getTimestamp());
                    sess.setProject(s.getProject());
                    sess.setFolderName(s.getFolderName());

                    SessionXmlRebuilderRequest request = new SessionXmlRebuilderRequest(user, sess, sessionDir);
                    XDAT.sendJmsRequest(request);
                }
            } catch (Exception exception) {
                logger.error("Error when setting prearchive session status to QUEUED", exception);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,exception);
            }
        }

		
		final Response response = getResponse();
		try {
			response.setEntity(updatedStatusRepresentation(ss,overrideVariant(getPreferredVariant())));
		} catch (Exception e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
			return;
		}
	}
}
