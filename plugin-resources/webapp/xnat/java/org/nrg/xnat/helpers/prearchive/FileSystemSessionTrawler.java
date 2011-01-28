package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public Collection<SessionData> get() throws IllegalStateException {
		SortedMap<java.util.Date, Collection<PrearcTableBuilder.Session>> sessions = new TreeMap<Date, Collection<PrearcTableBuilder.Session>>();
		ArrayList<SessionData> sds = new ArrayList<SessionData>();
		long time = System.currentTimeMillis();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		for (final File tsdir : new File(this.prearcPath).listFiles()) {
			try {
				sessions = new PrearcTableBuilder().getPrearcSessions(tsdir);
			} catch (IOException e) {
				logger.error("Error getting prearchive sessions from the filesystem" , e);
				throw new IllegalStateException();
			} catch (SAXException e) {
				logger.error("Error getting prearchive sessions from the filesystem" , e);
				throw new IllegalStateException();
			}
			Date listFilesTime = new Date(System.currentTimeMillis() - time);
			for (final Collection<PrearcTableBuilder.Session> ss : sessions.values()) {
				for (PrearcTableBuilder.Session s : ss) {
					SessionData _s = s.getSessionData(StringUtils.join(new String[]{PrearcDatabase.projectPath(s.getProject()),"/".intern(),
																	   s.getTimestamp(),"/".intern(),
																	   s.getName()}));
					sds.add(_s);
				}
			}
			long nowTime = System.currentTimeMillis();
			long diff = nowTime - time;			
			StringBuilder sb = new StringBuilder();
			sb.append(tsdir.getName() + ":");
			sb.append(dateFormat.format(listFilesTime) + ":");
			sb.append(dateFormat.format(new Date(diff)));
			System.out.println(sb);
			
			time = System.currentTimeMillis();			
		}
		// TODO Auto-generated method stub
		return sds;
	}

}
