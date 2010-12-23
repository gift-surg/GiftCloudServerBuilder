// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.utils;

import java.io.File;

import org.nrg.xft.ItemI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

public interface ArchivableItem extends ItemI{
	public String getArchiveDirectoryName();
	public File getExpectedCurrentDirectory() throws InvalidArchiveStructure;
}
