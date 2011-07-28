package org.nrg.xnat.helpers.uri.archive.impl;

import java.util.Map;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.archive.ProjectURII;

public class ProjURI extends ArchiveURI implements ArchiveItemURI,ProjectURII{
	private XnatProjectdata project = null;
	
	public ProjURI(final Map<String, Object> props, final String uri) {
		super(props, uri);
	}

	protected void populateProject(){
		if(project==null){
			project=XnatProjectdata.getProjectByIDorAlias(props.get(URIManager.PROJECT_ID).toString(), null, false);
		}
	}
	
	public XnatProjectdata getProject(){
		this.populateProject();
		return project;
	}

	@Override
	public ItemI getSecurityItem() {
		return getProject();
	}
}
