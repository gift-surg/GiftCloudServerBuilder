package org.nrg.xnat.utils;

import java.io.File;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class UserUtils {
 	public static String getUserCacheUploadsPath(final XDATUser user){
		return ArcSpecManager.GetInstance().getGlobalCachePath() + "USERS" + File.separator + user.getXdatUserId();
	}
	
	public static File getUserCacheFile(final XDATUser user, final String directory, final String file){
		return new File(new File(getUserCacheUploadsPath(user),directory),file);
	}
}
