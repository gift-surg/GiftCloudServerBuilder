/**
 * 
 */
package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.xml.sax.SAXException;

/**
 * @author timo
 *
 */
public class PrearcTableBuilder implements PrearcTableBuilderI {
	static Logger logger = Logger.getLogger(PrearcTableBuilder.class);

	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.helpers.prearchive.PrearcTableBuilderI#buildTable(java.lang.String[])
	 */
	public ProjectPrearchiveI buildTable(final String project, final XDATUser user, final String urlBase) throws IOException, InvalidPermissionException, Exception {
		final XFTTable table = new XFTTable();
		table.initTable(PREARC_HEADERS);
		
		final File prearc= PrearcUtils.getPrearcDir(user, project);
		Date lastMod=null;
		if(prearc.exists()){
			lastMod=new Date(prearc.lastModified());
			
			for (final Collection<Session> ss : getPrearcSessions(prearc).values()) {
				for (final Session s : ss) {
					if(s.getLastBuiltDate().after(lastMod)){
						lastMod=s.getLastBuiltDate();
					}
					
					table.rows().add(buildRow(s,urlBase));
				}
			}
		}else{
			return null;
		}
		
		return new ProjectPrearchive(lastMod,table);
	}
	
	public final static String[] PREARC_HEADERS = {"project".intern(),"last_mod".intern(),"uploaded".intern(),"scan_date".intern(),"scan_time".intern(),"subject".intern(),"session".intern(),"status".intern(),"url".intern()};
	
	public static Object[] buildRow(final Session s,final String urlBase){
		Object[] row = new Object[PREARC_HEADERS.length];
		row[0]=s.getProject();
		row[1]=s.getLastBuiltDate();
		row[2]=s.getUploadDate();
		row[3]=s.getDate();
		row[4]=s.getTime();
		row[5]=s.getSubjectId();
		row[6]=s.getLabel();
		row[7]=s.getStatus();
		row[8]=StringUtils.join(new String[]{urlBase,"/".intern(),s.getTimestamp(),"/".intern(),s.getName()});
		
		return row;
	}

	
	private static XnatImagesessiondataI parseSession(final File s) throws IOException, SAXException{
		XDATXMLReader parser = new XDATXMLReader();
		return (XnatImagesessiondataI)parser.parse(s);
	}

	public class ProjectPrearchive implements ProjectPrearchiveI {
		private Date lastMod;
		private XFTTable content;
		
		public ProjectPrearchive(final Date l, final XFTTable c){
			lastMod=l;
			content=c;
		}

		/* (non-Javadoc)
		 * @see org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI#getLastMod()
		 */
		public Date getLastMod() {
			return lastMod;
		}

		/* (non-Javadoc)
		 * @see org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI#getContent()
		 */
		public XFTTable getContent() {
			return content;
		}
		
		
	}

	public String[] getColumns() {
		return PREARC_HEADERS;
	}
	


	private class Session implements Comparable<Session> {
		private final String name;
		
		private String timestamp;

		private final Date uploadDate;

		private final Date lastBuiltDate;

		private final File sessionXML;
		
		private XnatImagesessiondataI session=null;
		
		private PrearcStatus status;

		Session(final File sessdir) {
			name = sessdir.getName();

			sessionXML = new File(sessdir.getPath() + ".xml");
			if (sessionXML.exists()) {
				lastBuiltDate = new Date(sessionXML.lastModified());
			} else {
				lastBuiltDate = new Date();
			}

			final DateFormat format = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
			Date t_uploadDate;
			try {
				t_uploadDate = format.parse(sessdir.getParentFile().getName());
			} catch (final ParseException e) {
				logger.error("Unable to parse upload date from session parent " + sessdir.getParentFile(), e);
				t_uploadDate = null;
			}
			uploadDate = t_uploadDate;

			
			status=PrearcUtils.checkSessionStatus(sessionXML);
			try {
				session=parseSession(sessionXML);

				final String sessionID = session.getId();
				if (null == sessionID || "".equals(sessionID) || "NULL".equals(sessionID)) {
					status = PrearcStatus.READY;
				} else {
					status = PrearcStatus.ARCHIVING;
				}
			} catch (Exception e) {
				status=PrearcStatus.ERROR;
				logger.error("",e);
			}
		}

		public Date getLastBuiltDate() {
			return lastBuiltDate;
		}

		public String getName() {
			return name;
		}

		public Date getUploadDate() {
			return uploadDate;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

		public String getProject(){
			return (session!=null)?session.getProject():null;
		}

		public Object getDate(){
			return (session!=null)?session.getDate():null;
		}

		public Object getTime(){
			return (session!=null)?session.getTime():null;
		}

		public String getSubjectId(){
			return (session!=null)?session.getSubjectId():null;
		}

		public String getLabel(){
			return (session!=null)?session.getLabel():null;
			
		}
		
		public PrearcStatus getStatus(){
			return status;
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(final Session other) {
			return getLastBuiltDate().compareTo(other.getLastBuiltDate());
		}
	}

	private SortedMap<Date, Collection<Session>> getPrearcSessions(final File prearcDir) throws IOException, SAXException {
		final SortedMap<Date, Collection<Session>> sessions = new TreeMap<Date, Collection<Session>>();
		for (final File tsdir : prearcDir.listFiles(PrearcUtils.isTimestampDirectory)) {
			for (final File sessdir : tsdir.listFiles(PrearcUtils.isDirectory)) {
				final Session session = new Session(sessdir);
				final Date builtDate = session.getLastBuiltDate();
				
				session.setTimestamp(tsdir.getName());
				
				if (!sessions.containsKey(builtDate)) {
					sessions.put(builtDate, new ArrayList<Session>(1));
				}
				sessions.get(builtDate).add(session);
			}
		}
		return sessions;
	}
}
