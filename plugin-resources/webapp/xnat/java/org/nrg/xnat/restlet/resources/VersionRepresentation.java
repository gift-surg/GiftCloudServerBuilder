package org.nrg.xnat.restlet.resources;

import java.io.IOException;

import org.nrg.xft.XFT;
import org.nrg.xnat.utils.FileUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionRepresentation extends Resource {
	private final Logger logger = LoggerFactory.getLogger(VersionRepresentation.class);

	public VersionRepresentation(Context context, Request request, Response response) {
		super(context, request, response);
		this.getVariants().add(new Variant(MediaType.ALL));
	}

	@Override
	public Representation represent(Variant variant) {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting XNAT version from the default configuration folder");
		}
		try {
			return new StringRepresentation(FileUtils.getXNATVersion());
		} catch (IOException exception) {
			return new StringRepresentation("Unknown version");
		}
	}
}
