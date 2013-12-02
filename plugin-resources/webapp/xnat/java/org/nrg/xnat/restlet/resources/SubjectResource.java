/*
 * org.nrg.xnat.restlet.resources.SubjectResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.resources;

import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.BaseXnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.EventRequirementAbsent;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class SubjectResource extends ItemResource {
    private static final String PRIMARY = "primary";

    protected XnatProjectdata proj = null;
    protected String subID = null;
    protected XnatSubjectdata sub = null;
    protected XnatSubjectdata existing = null;

    public SubjectResource(Context context, Request request, Response response) {
        super(context, request, response);

        String pID = (String) getParameter(request, "PROJECT_ID");
        if (pID != null) {
            proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
        }

        subID = (String) getParameter(request, "SUBJECT_ID");

        if (proj != null) {
            existing = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID, user, false);
        }

        if (existing == null) {
            existing = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);
            if (existing != null && (proj != null && !existing.hasProject(proj.getId()))) {
                existing = null;
            }
        }

        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));

        this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.SUBJECT_DATA, false));
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        try {
            XFTItem template = null;
            if (existing != null) {
                template = existing.getItem();
            }

            XFTItem item = this.loadItem("xnat:subjectData", true, (this.isQueryVariableFalse("loadExisting")) ? null : template);

            if (item == null) {
                item = XFTItem.NewItem("xnat:subjectData", user);
            }

            if (item.instanceOf("xnat:subjectData")) {
                sub = new XnatSubjectdata(item);

                if (filepath != null && !filepath.equals("")) {
                    if (filepath.startsWith("projects/")) {
                        if (!user.canRead(sub)) {
                            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Specified user account has insufficient privileges for subjects in this project.");
                            return;
                        }

                        String newProjectS = filepath.substring(9);
                        XnatProjectdata newProject = XnatProjectdata.getXnatProjectdatasById(newProjectS, user, false);
                        String newLabel = this.getQueryVariable("label");

                        if (newProject != null) {
                            XnatProjectparticipant matched = null;
                            int index = 0;
                            for (XnatProjectparticipantI pp : sub.getSharing_share()) {
                                if (pp.getProject().equals(newProject.getId())) {
                                    matched = ((XnatProjectparticipant) pp);
                                    if (newLabel != null && (pp.getLabel() == null || (!pp.getLabel().equals(newLabel)))) {
                                        XnatSubjectdata temp = XnatSubjectdata.GetSubjectByProjectIdentifier(newProject.getId(), newLabel, null, false);
                                        if (temp != null) {
                                            this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Label already in use:" + newLabel);
                                            return;
                                        }

                                        pp.setLabel(newLabel);
                                        BaseXnatSubjectdata.SaveSharedProject((XnatProjectparticipant) pp, sub, user, newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.CONFIGURED_PROJECT_SHARING));

                                        if (!this.isQueryVariableTrue(PRIMARY)) {
                                            this.returnDefaultRepresentation();
                                            return;
                                        }
                                    }
                                    break;
                                }
                                index++;
                            }

                            if (newLabel != null) {
                                XnatSubjectdata existing = XnatSubjectdata.getXnatSubjectdatasById(sub.getId(), user, false);
                                if (!sub.getLabel().equals(existing.getLabel())) {
                                    sub.setLabel(existing.getLabel());
                                }
                            }

                            if (this.isQueryVariableTrue(PRIMARY)) {
                                if (!user.canDelete(sub)) {
                                    this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Specified user account has insufficient privileges for subjects in this project.");
                                    return;
                                }

                                EventMetaI c = BaseXnatSubjectdata.ChangePrimaryProject(user, sub, newProject, newLabel, newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.MODIFY_PROJECT));

                                if (matched != null) {
                                    SaveItemHelper.authorizedRemoveChild(sub.getItem(), "xnat:subjectData/sharing/share", matched.getItem(), user, c);
                                    sub.removeSharing_share(index);
                                }
                            } else {
                                if (matched == null) {
                                    if (newLabel != null) {
                                        XnatSubjectdata temp = XnatSubjectdata.GetSubjectByProjectIdentifier(newProject.getId(), newLabel, null, false);
                                        if (temp != null) {
                                            this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Label already in use:" + newLabel);
                                            return;
                                        }
                                    }
                                    if (user.canCreate(sub.getXSIType() + "/project", newProject.getId())) {
                                        XnatProjectparticipant pp = new XnatProjectparticipant((UserI) user);
                                        pp.setProject(newProject.getId());
                                        if (newLabel != null) pp.setLabel(newLabel);
                                        pp.setSubjectId(sub.getId());
                                        BaseXnatSubjectdata.SaveSharedProject(pp, sub, user, newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.CONFIGURED_PROJECT_SHARING));
                                    } else {
                                        this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Specified user account has insufficient create privileges for subjects in the " + newProject.getId() + " project.");
                                        return;
                                    }
                                } else {
                                    this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Already assigned to project:" + newProject.getId());
                                    return;
                                }
                            }

                            this.returnDefaultRepresentation();
                        } else {
                            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to identify project: " + newProjectS);
                        }
                    } else {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                } else {

                    if (this.proj == null && sub.getProject() != null) {
                        proj = XnatProjectdata.getXnatProjectdatasById(sub.getProject(), user, false);
                    }

                    if (this.proj != null) {
                        if (sub.getProject() == null || sub.getProject().equals("")) {
                            sub.setProject(this.proj.getId());

                            if (sub.getLabel() == null || sub.getLabel().equals("")) {
                                sub.setLabel(this.subID);
                            }
                        } else if (sub.getProject().equals(this.proj.getId())) {
                            if (sub.getLabel() == null || sub.getLabel().equals("")) {
                                sub.setLabel(this.subID);
                            }
                        } else {
                            boolean matched = false;
                            for (XnatProjectparticipantI pp : sub.getSharing_share()) {
                                if (pp.getProject().equals(this.proj.getId())) {
                                    matched = true;

                                    if (pp.getLabel() == null || pp.getLabel().equals("")) {
                                        pp.setLabel(this.subID);
                                    }
                                    break;
                                }
                            }

                            if (!matched) {
                                XnatProjectparticipant pp = new XnatProjectparticipant((UserI) user);
                                pp.setProject(this.proj.getId());
                                pp.setLabel(this.subID);
                            }
                        }
                    } else {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "Submitted subject record must include the project attribute.");
                        return;
                    }

                    if (existing == null) {
                        if (sub.getId() != null) {
                            existing = XnatSubjectdata.getXnatSubjectdatasById(sub.getId(), user, false);
                        }

                        if (existing == null && sub.getProject() != null && sub.getLabel() != null) {
                            existing = XnatSubjectdata.GetSubjectByProjectIdentifier(sub.getProject(), sub.getLabel(), user, false);
                        }

                        if (existing == null) {
                            for (XnatProjectparticipantI pp : sub.getSharing_share()) {
                                existing = XnatSubjectdata.GetSubjectByProjectIdentifier(pp.getProject(), pp.getLabel(), user, false);
                                if (existing != null) {
                                    break;
                                }
                            }
                        }
                    }


                    if (existing == null) {
                        if (!user.canCreate(sub)) {
                            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Specified user account has insufficient create privileges for subjects in this project.");
                            return;
                        }
                        //IS NEW
                        if (sub.getId() == null || sub.getId().equals("")) {
                            sub.setId(XnatSubjectdata.CreateNewID());
                        }


                    } else {
                        if (!existing.getProject().equals(sub.getProject())) {
                            this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Project must be modified through separate URI.");
                            return;
                        }

                        if (!user.canEdit(sub)) {
                            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Specified user account has insufficient edit privileges for subjects in this project.");
                            return;
                        }
                        if (sub.getId() == null || sub.getId().equals("")) {
                            sub.setId(existing.getId());
                        }
                    }

                    if (this.getQueryVariable("gender") != null) {
                        sub.setProperty("xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/gender", this.getQueryVariable("gender"));
                    }

                    if (!StringUtils.IsEmpty(sub.getLabel()) && !StringUtils.IsAlphaNumericUnderscore(sub.getId())) {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Invalid character in subject label.");
                        return;
                    }


                    final ValidationResults vr = sub.validate();

                    if (vr != null && !vr.isValid()) {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, vr.toFullString());
                        return;
                    }
                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, sub.getItem(), newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.getAddModifyAction(sub.getXSIType(), (existing == null))));
                    EventMetaI c = wrk.buildEvent();

                    try {
                        if (SaveItemHelper.authorizedSave(sub, user, false, this.isQueryVariableTrue("allowDataDeletion"), c)) {

                            WorkflowUtils.complete(wrk, c);
                            user.clearLocalCache();
                            MaterializedView.DeleteByUser(user);
                        }
                    } catch (Exception e) {
                        WorkflowUtils.fail(wrk, c);
                        throw e;
                    }

                    postSaveManageStatus(sub);

                    this.returnString(sub.getId(), (existing == null) ? Status.SUCCESS_CREATED : Status.SUCCESS_OK);
                }
            } else {
                this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "Only xnat:Subject documents can be PUT to this address.");
            }
        } catch (SAXParseException e) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e.getMessage());
        } catch (InvalidValueException e) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
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
        if (sub == null && subID != null) {
            sub = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);

            if (sub == null && proj != null) {
                sub = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID, user, false);
            }
        }
        if (sub == null) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified subject.");
            return;
        }

        XnatProjectdata newProject = null;

        if (filepath != null && !filepath.equals("")) {
            if (filepath.startsWith("projects/")) {
                String newProjectS = filepath.substring(9);
                newProject = XnatProjectdata.getXnatProjectdatasById(newProjectS, user, false);
                if (newProject == null) {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to identify project: " + newProjectS);
                    return;
                }
            } else {
                this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                return;
            }
        } else if (!sub.getProject().equals(proj.getId())) {
            newProject = proj;
        }

        PersistentWorkflowI wrk;
        try {
            wrk = WorkflowUtils.buildOpenWorkflow(user, sub.getItem(), newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.getDeleteAction(sub.getXSIType())));
            EventMetaI c = wrk.buildEvent();

            try {
                String msg = sub.delete((newProject != null) ? newProject : proj, user, this.isQueryVariableTrue("removeFiles"), c);
                if (msg != null) {
                    WorkflowUtils.fail(wrk, c);
                    this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, msg);
                } else {
                    WorkflowUtils.complete(wrk, c);
                }
            } catch (Exception e) {
                try {
                    WorkflowUtils.fail(wrk, c);
                } catch (Exception e1) {
                    logger.error("", e1);
                }
                logger.error("", e);
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            }
        } catch (EventRequirementAbsent e1) {
            logger.error("", e1);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, e1.getMessage());
        }
    }

    @Override
    public Representation represent(Variant variant) {
        MediaType mt = overrideVariant(variant);

        if (sub == null && subID != null) {
            sub = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);

            if (sub == null && proj != null) {
                sub = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID, user, false);
            }
        }

        if (sub != null) {
            String filepath = this.getRequest().getResourceRef().getRemainingPart();
            if (filepath != null && filepath.contains("?")) {
                filepath = filepath.substring(0, filepath.indexOf("?"));
            }

            if (filepath != null && filepath.startsWith("/")) {
                filepath = filepath.substring(1);
            }
            if (filepath != null && filepath.equals("status")) {
                return returnStatus(sub, mt);
            } else if (filepath != null && filepath.startsWith("projects")) {
                XFTTable t = new XFTTable();
                ArrayList<String> al = new ArrayList<String>();
                al.add("label");
                al.add("ID");
                al.add("Secondary_ID");
                al.add("Name");
                t.initTable(al);

                Object[] row = new Object[4];
                row[0] = sub.getLabel();
                XnatProjectdata primary = sub.getPrimaryProject(false);
                row[1] = primary.getId();
                row[2] = primary.getSecondaryId();
                row[3] = primary.getName();
                t.rows().add(row);

                for (Map.Entry<XnatProjectdataI, String> entry : sub.getProjectDatas().entrySet()) {
                    row = new Object[4];
                    row[0] = entry.getValue();
                    row[1] = entry.getKey().getId();
                    row[2] = entry.getKey().getSecondaryId();
                    row[3] = entry.getKey().getName();
                    t.rows().add(row);
                }

                return representTable(t, mt, new Hashtable<String, Object>());
            } else {
                return this.representItem(sub.getItem(), mt);
            }
        } else {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
                    "Unable to find the specified subject.");
            return null;
        }

    }

    @Override
    public Representation representItem(XFTItem item, MediaType mt) {
        Representation representation = super.representItem(item, mt);

        if (representation != null && proj != null && representation instanceof TurbineScreenRepresentation && StringUtils.HasContent(proj.getId())) {
            // provides appropriate rendering if the caller is querying this subject in the context of a shared project
            ((TurbineScreenRepresentation) representation).setRunDataParameter("project", proj.getId());
        }

        return representation;
    }

}
