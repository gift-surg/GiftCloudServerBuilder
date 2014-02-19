/*
 * org.nrg.xnat.utils.UserUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.utils;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

import java.io.File;

public class UserUtils {
 	public static String getUserCacheUploadsPath(final XDATUser user){
		return ArcSpecManager.GetInstance().getGlobalCachePath() + "USERS" + File.separator + user.getXdatUserId();
	}
	
	public static File getUserCacheFile(final XDATUser user, final String directory, final String file){
		return new File(new File(getUserCacheUploadsPath(user),directory),file);
	}
	
	public static File getUserCacheFile(final XDATUser user, final String directory){
		return new File(getUserCacheUploadsPath(user),directory);
	}
}
