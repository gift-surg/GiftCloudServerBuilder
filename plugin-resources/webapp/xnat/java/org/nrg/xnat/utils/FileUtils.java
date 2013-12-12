/*
 * org.nrg.xnat.utils.FileUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.utils;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xft.XFT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

public class FileUtils {

	public static void moveToCache(final String project, final String subdir, final File src) throws IOException {
		// should include a timestamp in folder name
		if (src.exists()) {
			final File cache = (StringUtils.isEmpty(subdir)) ? new File(XFT.GetCachePath(), project) : new File(new File(XFT.GetCachePath(), project), subdir);

			final File dest = new File(cache, org.nrg.xft.utils.FileUtils.renameWTimestamp(src.getName(),Calendar.getInstance().getTime()));

			org.nrg.xft.utils.FileUtils.MoveDir(src, dest, false);
		}
	}

	public static File buildCachepath(String project, final String subdir, final String destName) {
		if (project == null)
			project = "Unknown";
		final File cache = (StringUtils.isEmpty(subdir)) ? new File(XFT.GetCachePath(), project) : new File(new File(XFT.GetCachePath(), project), subdir);

		final File dest = new File(cache, org.nrg.xft.utils.FileUtils.renameWTimestamp(destName,Calendar.getInstance().getTime()));

		return dest;
	}

	public static void copyToCache(final String project, final String subdir, final File src) throws IOException {
		// should include a timestamp in folder name
		if (src.exists()) {
			org.nrg.xft.utils.FileUtils.CopyDir(src, buildCachepath(project, subdir, src.getName()), false);
		}
	}

	/**
         * This attempts to retrieve the XNAT version from the tags file in the {@link
         * XFT#GetConfDir() default configuration folder}. Failing that, it will use
         * the VERSION file. The tags file is copied in from the .hgtags file in the
         * Mercurial repository and works for installations that are built from source
         * in a connected HG repository.
	 * 
	 * @return The current version of XNAT as a String.
	 */
        public static String getXNATVersion() throws IOException {
                final String location = XFT.GetConfDir();

                if (StringUtils.isEmpty(location)) {
			throw new IOException("Can't look for version in empty location.");
		}

		// First try to get the tags file at the indicated location.
		boolean usingTags = true;
		File file = new File(location + File.separator + "tags");
		if (!file.exists()) {
			usingTags = false;
			file = new File(location + File.separator + "VERSION");
			if (!file.exists()) {
				throw new IOException("Can find neither tags nor VERSION file at the indicated location: " + location);
			}
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));

			// If we're using the tags file, the process is a bit more
			// complicated.
			if (usingTags) {
				// Go through each line until we get to the end (readLine()
				// returns null).
				String current, last = null;
				while ((current = reader.readLine()) != null) {
					// Cache each non-null line.
					last = current;
				}

				// If the last non-null line was empty, then we don't know
				// what's going on.
				if (StringUtils.isEmpty(last)) {
					return "Unknown version";
				}

				// Split on the space, the last line should be something like
				// "123456789abcdef0 1.5.0"
				String[] components = last.split(" ");

				// If it didn't meet that criteria, we don't know what's going
				// on.
				if (components == null || components.length != 2) {
					return "Unknown version";
				}

				// If we got back a two-element array, the second element should
				// be the version as indicated by the HG tag.
				return components[1];
			}
			// If we're using the VERSION file...
			else {
				// It's pretty simple, just read it and spit it back out.
				return (new BufferedReader(reader)).readLine();
			}
		} catch (Exception e) {
			throw new IOException("Error reading file at the indicated location: " + location, e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					logger.warn("Exception encountered while reading version from file", e);
				}
			}
		}
	}


	public static <T extends String> File buildCacheSubDir(T... directories) {
		File last = new File(XFT.GetCachePath());
		
		for(final String dir:directories){
			if(!StringUtils.isEmpty(dir)){
				last=new File(last,dir);
			}
		}

		return last;
	}

	private static final Log logger = LogFactory.getLog(FileUtils.class);
}
