package org.nrg.xnat.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.axis.utils.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xft.XFT;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class FileUtils {

	public static void moveToCache(final String project, final String subdir, final File src) throws IOException{
		//should include a timestamp in folder name
		if(src.exists()){
			final File cache=(StringUtils.isEmpty(subdir))?new File(XFT.GetCachePath(),project):new File(new File(XFT.GetCachePath(),project),subdir);
			
			final File dest= new File(cache,org.nrg.xft.utils.FileUtils.renameWTimestamp(src.getName()));
						
			org.nrg.xft.utils.FileUtils.MoveDir(src,dest,false);
		}
	}
	
	public static File buildCachepath(final String project, final String subdir,final String destName){
		final File cache=(StringUtils.isEmpty(subdir))?new File(XFT.GetCachePath(),project):new File(new File(XFT.GetCachePath(),project),subdir);
		
		final File dest= new File(cache,org.nrg.xft.utils.FileUtils.renameWTimestamp(destName));
		
		return dest;
	}

	public static void copyToCache(final String project, final String subdir, final File src) throws IOException{
		//should include a timestamp in folder name
		if(src.exists()){
			org.nrg.xft.utils.FileUtils.CopyDir(src,buildCachepath(project,subdir,src.getName()),false);
		}
	}

}
