package org.nrg.xnat.helpers.file;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 3/12/14 12:17 PM
 */

import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.helpers.resource.direct.ResourceModifierA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MoveStoredFileRequest implements Serializable {
    public MoveStoredFileRequest(ResourceModifierA resourceModifier, Object resourceIdentifier, List<FileWriterWrapperI> writers, XDATUser user, Number workflowId, boolean delete, String[] notifyList, String type, String filepath, XnatResourceInfo resourceInfo, boolean extract) {
        this.resourceModifier = resourceModifier;
        this.resourceIdentifier = resourceIdentifier != null ? resourceIdentifier.toString() : null;
        for (FileWriterWrapperI writer : writers) {
            try {
                StoredFile storedFile = (StoredFile) writer;
                this.writers.add(storedFile);
            } catch (Exception e) { /* Not a stored file for some reason */ }
        }
        this.user = user;
        this.workflowId = workflowId.toString();
        this.delete = delete;
        this.notifyList = notifyList;
        this.type = type;
        this.filepath = filepath;
        this.resourceInfo = resourceInfo;
        this.extract = extract;
    }

    public ResourceModifierA getResourceModifier() {
        return resourceModifier;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public List<StoredFile> getWriters() {
        return writers;
    }

    public XDATUser getUser() {
        return user;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public boolean isDelete() {
        return delete;
    }

    public String[] getNotifyList() {
        return notifyList;
    }

    public String getType() {
        return type;
    }

    public String getFilepath() {
        return filepath;
    }

    public XnatResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public boolean isExtract() {
        return extract;
    }

    private static final long serialVersionUID = 42L;
    private final ResourceModifierA resourceModifier;
    private final String resourceIdentifier;
    private final List<StoredFile> writers = new ArrayList<StoredFile>();
    private final XDATUser user;
    private final String workflowId;
    private final boolean delete;
    private final String[] notifyList;
    private final String type;
    private final String filepath;
    private final XnatResourceInfo resourceInfo;
    private final boolean extract;
}
