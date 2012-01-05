package org.nrg.xnat.helpers.uri.archive;

import java.util.Map;

import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;

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

}
