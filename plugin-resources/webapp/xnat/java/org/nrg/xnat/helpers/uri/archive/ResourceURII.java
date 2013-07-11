/*
 * org.nrg.xnat.helpers.uri.archive.ResourceURII
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.uri.archive;

import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;


public interface ResourceURII extends ArchiveItemURI{	
	public abstract XnatAbstractresourceI getXnatResource();

	public XnatProjectdata getProject();
	
	/**
	 * Label of this resource
	 * @return
	 */
	public String getResourceLabel();
	
	/**
	 * Path to data file within this resource
	 * @return
	 */
	public String getResourceFilePath();
	
}
