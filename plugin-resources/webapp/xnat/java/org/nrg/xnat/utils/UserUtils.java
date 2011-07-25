package org.nrg.xnat.utils;

import java.io.File;
import java.sql.SQLException;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class UserUtils {
	public static XDATUser getDICOMStoreUser() throws UserNotFoundException, XFTInitException, ElementNotFoundException, DBPoolException, SQLException, FieldNotFoundException, Exception{
		return new XDATUser("admin");   // TODO: make this configurable
	}
	
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
