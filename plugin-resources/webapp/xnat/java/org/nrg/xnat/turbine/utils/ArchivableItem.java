// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.utils;

import java.io.File;

import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xft.ItemI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

public interface ArchivableItem extends ItemI{
	public String getArchiveDirectoryName();
	public File getExpectedCurrentDirectory() throws InvalidArchiveStructure, UnknownPrimaryProjectException;
	public String getXSIType();
	public String getId();
	public String getProject();
	public String getArchiveRootPath() throws UnknownPrimaryProjectException;
}
