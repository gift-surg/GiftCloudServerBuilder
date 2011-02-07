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
import org.nrg.xdat.bean.XnatImagesessiondataBean;
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
	@Override
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
		row[6]=PrearcTableBuilder.Session.pickName(s);
		row[7]=s.getStatus();
		row[8]=StringUtils.join(new String[]{urlBase,"/".intern(),s.getTimestamp(),"/".intern(),s.getName()});
		
		return row;
	}

	
	public static XnatImagesessiondataBean parseSession(final File s) throws IOException, SAXException{
		XDATXMLReader parser = new XDATXMLReader();
		return (XnatImagesessiondataBean) parser.parse(s);
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
		@Override
		public Date getLastMod() {
			return lastMod;
		}

		/* (non-Javadoc)
		 * @see org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI#getContent()
		 */
		@Override
		public XFTTable getContent() {
			return content;
		}
		
		
	}

	@Override
	public String[] getColumns() {
		return PREARC_HEADERS;
	}
	

	public static String printSession (Session s) {
		ArrayList<String> as = new ArrayList<String>();
		as.add("--Session--");
		as.add("Name : " + s.getName());
		as.add("Status : " + s.getStatus());
		as.add("SubjectId: " + s.getSubjectId());
		as.add("Scan Time : " + s.getTimestamp().toString());
		as.add("Uploaded : " + s.getUploadDate().toString());
		return StringUtils.join(as.toArray(new String[as.size()]), "\n");
	}

	public static class Session implements Comparable<Session> {
		private final File sessionXML;
		
		private XnatImagesessiondataI session=null;
		
		private SessionData data = new SessionData();

		Session(final File sessdir) {
			data.sessionTriple.name = sessdir.getName();

			sessionXML = new File(sessdir.getPath() + ".xml");
			if (sessionXML.exists()) {
				data.lastBuiltDate = new Date(sessionXML.lastModified());
			} else {			
				data.lastBuiltDate = new Date();
			}

			final DateFormat format = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
			Date t_uploadDate;
			try {
				t_uploadDate = format.parse(sessdir.getParentFile().getName());
			} catch (final ParseException e) {
				logger.error("Unable to parse upload date from session parent " + sessdir.getParentFile(), e);
				t_uploadDate = null;
			}
			data.uploadDate = t_uploadDate;

			
			data.status=PrearcUtils.checkSessionStatus(sessionXML);

			if(!sessionXML.exists()){
				if(PrearcStatus.potentiallyReady(data.status))data.status=PrearcStatus.RECEIVING;
			}else{
				try {
					session=parseSession(sessionXML);
					
					final String sessionID = session.getId();
					if (null == sessionID || "".equals(sessionID) || "NULL".equals(sessionID)) {
						data.status = PrearcStatus.READY;
					} else {
						data.status = PrearcStatus.ARCHIVING;
					}
				} catch (Exception e) {
					if(PrearcStatus.potentiallyReady(data.status))data.status=PrearcStatus.ERROR;
				}
			}
		}
		
		public SessionData getSessionData (String urlBase) {
			//populate the rest of the session data object.
			data.setProject(this.getProject());
			data.setScan_date(this.getDate());
			data.setScan_time(this.getTime());
			data.setSubject(this.getSubjectId());
			data.setName(this.getName());
			data.setUrl(StringUtils.join(new String[]{urlBase,"/".intern(),data.getTimestamp(),"/".intern(),this.getName()}));
			return this.data;
		}
		
		public static String pickName(final PrearcTableBuilder.Session s) {
			String ret = "";
			if (StringUtils.isNotEmpty(s.getLabel())) {
				ret = s.getLabel();
			}
			if (StringUtils.isEmpty(ret) && StringUtils.isNotEmpty(s.getPatientId())) {
				ret = s.getPatientId();
			}
			if (StringUtils.isEmpty(ret) && StringUtils.isNotEmpty(s.getPatientName())) {
				ret = s.getPatientName();
			}
			
			if (StringUtils.isEmpty(ret)){
				return s.getName();
			}
			return ret;
		}

		public void setName(String name) {
			this.data.sessionTriple.name = name;
		}
		public Date getLastBuiltDate() {
			return data.lastBuiltDate;
		}

		public String getName() {
			return data.sessionTriple.name;
		}

		public Date getUploadDate() {
			return data.uploadDate;
		}

		public String getTimestamp() {
			return data.sessionTriple.timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.data.sessionTriple.timestamp = timestamp;
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
		
		public String getPatientId() {
			return (session!=null)?session.getDcmpatientid():null;
		}
		
		public String getPatientName() {
			return (session!=null)?session.getDcmpatientname():null;
		}
		
		public PrearcStatus getStatus(){
			return data.status;
		}
		
		
		
		
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(final Session other) {
			return getLastBuiltDate().compareTo(other.getLastBuiltDate());
		}
	}

	public SortedMap<Date, Collection<Session>> getPrearcSessions(final File prearcDir) throws IOException, SAXException {
		final SortedMap<Date, Collection<Session>> sessions = new TreeMap<Date, Collection<Session>>();
		if(PrearcUtils.isTimestampDirectory.accept(prearcDir)){
			for (final File sessdir : prearcDir.listFiles(PrearcUtils.isDirectory)) {
				final Session session = buildSessionObject(sessdir,prearcDir.getName());
				
				final Date builtDate = session.getLastBuiltDate();
				
				if (!sessions.containsKey(builtDate)) {
					sessions.put(builtDate, new ArrayList<Session>(1));
				}
				sessions.get(builtDate).add(session);
			}
		}else{		
		for (final File tsdir : prearcDir.listFiles(PrearcUtils.isTimestampDirectory)) {
			for (final File sessdir : tsdir.listFiles(PrearcUtils.isDirectory)) {
				final Session session = buildSessionObject(sessdir,tsdir.getName());

				final Date builtDate = session.getLastBuiltDate();
				if (!sessions.containsKey(builtDate)) {
					sessions.put(builtDate, new ArrayList<Session>(1));
				}
				sessions.get(builtDate).add(session);
			}
		}
		}
		return sessions;
	}
	
	public static Session buildSessionObject(final File sessdir,final String timestamp){
		final Session session = new Session(sessdir);		
		session.setTimestamp(timestamp);
		return session;
	}
}
