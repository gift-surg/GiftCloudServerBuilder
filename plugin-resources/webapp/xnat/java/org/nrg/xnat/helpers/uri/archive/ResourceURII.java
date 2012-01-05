package org.nrg.xnat.helpers.uri.archive;

import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;


public interface ResourceURII extends ArchiveItemURI{	
	//add this when we get time
	//public abstract XnatAbstractresourceI getResource(final String qualifier, final String XNAME);
	
	public String getResourceLabel();
	public String getResourceFilePath();
}
