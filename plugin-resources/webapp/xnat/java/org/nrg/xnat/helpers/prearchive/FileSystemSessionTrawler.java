package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Retrieve the session data from the filesystem
 * @author aditya
 *
 */
public final class FileSystemSessionTrawler implements SessionDataProducerI {
	private final Logger logger = LoggerFactory.getLogger(FileSystemSessionTrawler.class);
	private String prearcPath;
	
	public FileSystemSessionTrawler (String prearcPath) {
		this.prearcPath = prearcPath;
	}

	static List<String> hidden=new ArrayList(){{add(PrearcUtils.TEMP_UNPACK);}};
	
	static FileFilter dbFiles = new FileFilter() {
		public boolean accept(File pathname){
			boolean dbExtension = false;
			if (pathname.getName().indexOf('.') != -1) {
				return false;
			}
			return true;
		}
	};
	static FileFilter hiddenAndDatabaseFileFilter=new FileFilter(){
		public boolean accept(File pathname) {
			return !hidden.contains(pathname.getName()) && !dbFiles.accept(pathname) && pathname.isDirectory();
		}
	};

	@Override
	public Collection<SessionData> get() throws IllegalStateException {
		SortedMap<java.util.Date, Collection<PrearcTableBuilder.Session>> sessions = new TreeMap<Date, Collection<PrearcTableBuilder.Session>>();
		ArrayList<SessionData> sds = new ArrayList<SessionData>();
		for (final File tsdir : new File(this.prearcPath).listFiles(hiddenAndDatabaseFileFilter)) {
			try {
				sessions = new PrearcTableBuilder().getPrearcSessions(tsdir);
			} catch (IOException e) {
				logger.error("Error getting prearchive sessions from the filesystem" , e);
				throw new IllegalStateException();
			} catch (SAXException e) {
				logger.error("Error getting prearchive sessions from the filesystem" , e);
				throw new IllegalStateException();
			}
			for (final Collection<PrearcTableBuilder.Session> ss : sessions.values()) {
				for (PrearcTableBuilder.Session s : ss) {
					SessionData _s = s.getSessionData(StringUtils.join(new String[]{PrearcDatabase.projectPath(s.getProject())}));
					sds.add(_s);
				}
			}
		}
		return sds;
	}

}
