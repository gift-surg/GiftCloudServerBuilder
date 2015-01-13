/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.utils.SaveItemHelper;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.base.Strings;

/**
 * Does the relevant processing when entering a new pseudonym.
 * 
 * @author Dzhoshkun Shakir
 *
 */
public class SubjectPseudonymProcessor extends SubjectPseudonymResource {
	String ppid;
	
	public SubjectPseudonymProcessor(Context context, Request request,
			Response response) {
		super(context, request, response);
		ppid = new String((String) getParameter(request, "PPID"));
		populate((String) getParameter(request, "SUBJECT_ID"), 
				ppid);
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}
	
	@Override
	public void handlePut() {
		if (subject == null) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "No subject could be found");
			return;
		}
		
		if (pseudonym != null) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Provided PPID exists");
			return;
		}

		try {
			XFTItem item = this.loadItem("ext:subjectPseudonym", true, null);
			ExtSubjectpseudonym newPseudonym;
			if (item.instanceOf("ext:subjectPseudonym")) {
				newPseudonym = new ExtSubjectpseudonym(item);
			}
			else {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
									"No appropriate item could be retrieved from storage");
				return;
			}

			if (!user.canEdit(subject)) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,
									"Specified user account has insufficient edit privileges for this project.");
				return;
			}
			else {
				if (!Strings.isNullOrEmpty(ppid)) {
					newPseudonym.setId(ppid); // TODO - new ID, generate ?
					newPseudonym.setIdentifier(ppid);
					newPseudonym.setProperty("pseudonymized_subject_ID", subject.getId());
				}
				else {
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty or null PPID string provided");
					return;
				}
			}
			
			PersistentWorkflowI wrk = PersistentWorkflowUtils
					.getOrCreateWorkflowData(null, user, newPseudonym.getItem(),
							this.newEventInstance(
									EventUtils.CATEGORY.DATA,
									"Inserted new pseudonym for a subject."));
			try {
				if (SaveItemHelper.authorizedSave(newPseudonym.getItem(), user, false,
						true, wrk.buildEvent())) {
					PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
					MaterializedView.DeleteByUser(user);
				}

				this.returnXML(newPseudonym.getItem());
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
	public boolean allowDelete() {
		return true;
	}
	
	@Override
	public void handleDelete() {
		// TODO
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultElementName() {
		// TODO Auto-generated method stub
		return null;
	}
}
