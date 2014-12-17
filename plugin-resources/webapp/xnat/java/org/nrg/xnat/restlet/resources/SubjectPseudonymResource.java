/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.om.ExtPseudonymizedsubjectdata;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.utils.SaveItemHelper;
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
	String pseudoId;
	ExtPseudonymizedsubjectdata subject;
	ExtSubjectpseudonym existingPseudonym, pseudonym;

	public SubjectPseudonymResource(Context context, Request request,
			Response response) {
		super(context, request, response);

		String subjectId = (String) getParameter(request, "SUBJECT_ID");
		pseudoId = (String) getParameter(request, "PSEUDONYM");
		if (subjectId != null && pseudoId != null && !pseudoId.isEmpty()) {
			subject = ExtPseudonymizedsubjectdata
					.GetPseudonymizedSubjectByLabel(subjectId, user, false);
			if (subject != null) {
				existingPseudonym = ExtSubjectpseudonym.GetPseudonym(subject,
						pseudoId, user, false);
			}
		}

		this.getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
		XFTItem template = null;
		if (existingPseudonym != null) {
			template = existingPseudonym.getItem().getCurrentDBVersion();
		}

		try {
			XFTItem item = this.loadItem("ext:subjectPseudonym", true, template);
			if (item.instanceOf("ext:subjectPseudonym")) {
				pseudonym = new ExtSubjectpseudonym(item);
			}
			
			if (existingPseudonym != null) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Submitted pseudonym exists");
				return;
			}
			else {
				if (!user.canEdit(subject)) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,
										"Specified user account has insufficient edit privileges for this project.");
					return;
				}
				else {
					if (!pseudoId.isEmpty()) {
						pseudonym.setId(pseudoId);
						pseudonym.setIdentifier(pseudoId);
						pseudonym.setProperty("pseudonymized_subject_ID", subject.getId());
					}
					else {
						this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty pseudo ID string provided");
						return;
					}
				}
			}
			
			PersistentWorkflowI wrk = PersistentWorkflowUtils
					.getOrCreateWorkflowData(null, user, pseudonym.getItem(),
							this.newEventInstance(
									EventUtils.CATEGORY.DATA,
									"Inserted new pseudonym for a subject."));
			try {
				if (SaveItemHelper.authorizedSave(pseudonym.getItem(), user, false,
						true, wrk.buildEvent())) {
					PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
					MaterializedView.DeleteByUser(user);
				}

				this.returnXML(pseudonym.getItem());
			} catch (Exception e) {
				PersistentWorkflowUtils.fail(wrk, wrk.buildEvent());
				throw e;
			}
		} catch (ClientException | ServerException | ElementNotFoundException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("", e);
		} catch (InvalidItemException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("", e);
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("", e);
		}
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
