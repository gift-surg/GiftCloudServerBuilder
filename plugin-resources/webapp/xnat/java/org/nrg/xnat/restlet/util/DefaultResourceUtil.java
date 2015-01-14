/**
 * 
 */
package org.nrg.xnat.restlet.util;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.utils.SaveItemHelper;
import org.restlet.data.Status;

import com.google.common.base.Strings;

/**
 * @author dzhoshkun
 *
 */
public class DefaultResourceUtil implements ResourceUtilI {
	XdatUser user;
	
	/**
	 * 
	 * @param user
	 */
	public DefaultResourceUtil(XdatUser user) {
		this.user = user;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getSubject(java.lang.String)
	 */
	@Override
	public XnatSubjectdata getSubject(String label) throws IllegalAccessException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(java.lang.String)
	 */
	@Override
	public XnatSubjectdata getMatchingSubject(String pseudoId) throws IllegalAccessException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public XnatSubjectdata getMatchingSubject(ExtSubjectpseudonym pseudonym)
			throws IllegalAccessException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getPseudonym(java.lang.String)
	 */
	@Override
	public ExtSubjectpseudonym getPseudonym(String pseudoId) throws IllegalAccessException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#addPseudonym(org.nrg.xdat.om.XnatSubjectdata, org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public void addPseudoId(XnatSubjectdata subject,
			String pseudoId) throws IllegalAccessException, Exception {
		// TODO Auto-generated method stub
//		try {
//			XFTItem item = this.loadItem("ext:subjectPseudonym", true, null);
//			ExtSubjectpseudonym newPseudonym;
//			if (item.instanceOf("ext:subjectPseudonym")) {
//				newPseudonym = new ExtSubjectpseudonym(item);
//			}
//			else {
//				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
//									"No appropriate item could be retrieved from storage");
//				return;
//			}
//
//			if (!user.canEdit(subject)) {
//				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,
//									"Specified user account has insufficient edit privileges for this project.");
//				return;
//			}
//			else {
//				if (!Strings.isNullOrEmpty(ppid)) {
//					newPseudonym.setId(ppid); // TODO - new ID, generate ?
//					newPseudonym.setIdentifier(ppid);
//					newPseudonym.setProperty("pseudonymized_subject_ID", subject.getId());
//				}
//				else {
//					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty or null PPID string provided");
//					return;
//				}
//			}
//			
//			PersistentWorkflowI wrk = PersistentWorkflowUtils
//					.getOrCreateWorkflowData(null, user, newPseudonym.getItem(),
//							this.newEventInstance(
//									EventUtils.CATEGORY.DATA,
//									"Inserted new pseudonym for a subject."));
//			try {
//				if (SaveItemHelper.authorizedSave(newPseudonym.getItem(), user, false,
//						true, wrk.buildEvent())) {
//					PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
//					MaterializedView.DeleteByUser(user);
//				}
//
//				this.returnXML(newPseudonym.getItem());
//			} catch (Exception e) {
//				PersistentWorkflowUtils.fail(wrk, wrk.buildEvent());
//				throw e;
//			}
//		} catch (ClientException | ServerException | ElementNotFoundException e) {
//			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//			logger.error("", e);
//		} catch (InvalidItemException e) {
//			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//			logger.error("", e);
//		} catch (Exception e) {
//			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//			logger.error("", e);
//		}
	}

}
