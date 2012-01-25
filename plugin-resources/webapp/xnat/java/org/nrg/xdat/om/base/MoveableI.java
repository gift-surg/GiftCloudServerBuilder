package org.nrg.xdat.om.base;

import java.io.File;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;


public interface MoveableI {
	public String getProject();
	public XnatProjectdata getProjectData();
	public String getLabel();
	public String getId();
	public XFTItem getCurrentDBVersion(boolean withChildren);
	public void setProject(String v);
	public void setLabel(String v);
}
