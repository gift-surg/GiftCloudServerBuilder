/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.action.ActionException;
import org.nrg.transaction.TransactionException;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.om.ExtPseudonymizedsubjectdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.base.BaseExtPseudonymizedsubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.merge.ProjectAnonymizer;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.representations.TurbineScreenRepresentation;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXParseException;

/**
 * @author dzhoshkun
 *
 */
public class SubjectPseudonymResource extends SecureResource {

	XnatProjectdata proj = null;
	ExtPseudonymizedsubjectdata subject = null;
	ExtSubjectpseudonym pseudonym = null;
	String pseudonymID = null;
	ExtSubjectpseudonym existingPseudonym;
	String subID = null;

	public SubjectPseudonymResource(Context context, Request request,
			Response response) {
		super(context, request, response);

		String pID = (String) getParameter(request, "PROJECT_ID");
		if (pID != null) {
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}

		if (proj == null) {
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return;
		}

		subID = (String) getParameter(request, "SUBJECT_ID");
		if (subID != null) {
			subject = (ExtPseudonymizedsubjectdata) ExtPseudonymizedsubjectdata
					.GetSubjectByProjectIdentifier(proj.getId(), subID, user,
							false);

			if (subject == null) {
				subject = ExtPseudonymizedsubjectdata
						.getExtPseudonymizedsubjectdatasById(subID, user, false);
				if (subject != null
						&& (proj != null && !subject.hasProject(proj.getId()))) {
					subject = null;
				}
			}
		}

		pseudonymID = (String) getParameter(request, "PSEUDONYM_ID");
		if (pseudonymID != null) {
			if (existingPseudonym == null) {
				existingPseudonym = ExtSubjectpseudonym
						.getExtSubjectpseudonymsByExtSubjectpseudonymId(
								pseudonymID, user, false);
			}

			// TODO ___ 1.999) why are these needed ?
			// org.nrg.xnat.restlet.resources.SubjectPseudoymResource.SubjectPseudoymResource(Context,
			// Request, Response)
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		} else {
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(
				XMLPathShortcuts.EXPERIMENT_DATA, false));
	}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
		// TODO ___ 2)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymResource.handlePut()
	}

	@Override
	public Representation represent(Variant variant) {
		// TODO ___ 2.0)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymResource.represent(Variant)
		return null;
	}

	@Override
	public Representation representItem(XFTItem item, MediaType mt) {
		// TODO ___ 2.1)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymResource.representItem(XFTItem,
		// MediaType)
		return null;
	}

}
