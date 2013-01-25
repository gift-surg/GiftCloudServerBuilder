package org.nrg.xnat.helpers.uri.archive;

import java.util.List;
import java.util.Map;

import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;

import com.google.common.collect.Lists;

public abstract class ResourceURIA extends ArchiveURI implements ResourceURII {

	public ResourceURIA(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	@Override
	public String getResourceLabel() {
		return (String)props.get(URIManager.XNAME);
	}

	@Override
	public String getResourceFilePath() {
		return (String)props.get(UriParserUtils._REMAINDER);
	}

	@Override
	public List<XnatAbstractresourceI> getResources(boolean includeAll) {
		return Lists.newArrayList(this.getXnatResource());
	}

}
