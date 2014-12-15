package org.nrg.xnat.utils;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/9/14 4:46 PM
 */

import org.apache.log4j.Logger;
import org.nrg.config.entities.Configuration;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PetTracerListUtils {
	final static Logger logger = Logger.getLogger(PetTracerListUtils.class);

	public static List<String> getPetTracerList(String project) {
		try {
			ConfigService configService = XDAT.getConfigService();
			Configuration projectConfig = configService.getConfig("tracers",
					"tracers",
					XnatProjectdata.getProjectInfoIdFromStringId(project));
			if (projectConfig != null
					&& projectConfig.getStatus().equals("enabled")) {
				return Arrays.asList(projectConfig.getContents().split("\\s+"));
			} else {
				Configuration siteConfig = configService.getConfig("tracers",
						"tracers");
				if (siteConfig != null) {
					return Arrays
							.asList(siteConfig.getContents().split("\\s+"));
				} else {
					return new ArrayList<String>();
				}
			}
		} catch (Exception e) {
			logger.error(e);
			return new ArrayList<String>();
		}
	}
}
