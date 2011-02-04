package org.nrg.xnat.helpers.prearchive;

import java.lang.reflect.Field;
import java.util.Date;

import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;

/**
 * SessionData is simply a container of values that represent a session. 
 * It is a stripped down version of the existing PrearcTableBuilder.Session
 * class.
 * 
 * It is currently (01/12/2011) used by the database cache implementation to 
 * hold values read from the database. 
 * 
 * It is preferable as a value container because while PrearcTableBuilder.Session 
 * also provides similar functionality but it also parses the session.xml 
 * and has unnecessary information from an XnatImagesessiondataI instance (eg.
 * the DICOM patient id).
 *  
 * @author aditya siram 
 */
public final class SessionData {
	public Date uploadDate;
	public Date lastBuiltDate;
	public PrearcStatus status;
	public SessionDataTriple sessionTriple = new SessionDataTriple();
	public Date scan_date;
	public String scan_time;
	public String subject;
	public String url;
	public SessionData() {
	}
	public String getName() {
		return sessionTriple.name;
	}
	public SessionData setName(String name) {
		this.sessionTriple.setName(name);
		return this;
	}
	public SessionData setName(Object o) {
		this.sessionTriple.setName(o);
		return this;
	}
	
	public String getTimestamp() {
		return sessionTriple.timestamp;
	}
	public SessionData setTimestamp(String timestamp) {
		this.sessionTriple.setTimestamp(timestamp);
		return this;
	}
	public SessionData setTimestamp(Object o) {
		this.sessionTriple.setTimestamp(o);
		return this;
	}
	
	public Date getUploadDate() {
		return uploadDate;
	}
	public SessionData setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
		return this;
	}
	
	public SessionData setUploadDate(Object o) {
		if (null != o) {
			java.sql.Timestamp ts = (java.sql.Timestamp)o;
			this.setUploadDate(PrearcUtils.timestamp2Date(ts));
		}
		return this;
	}
	
	public Date getLastBuiltDate() {
		return lastBuiltDate;
	}
	public SessionData setLastBuiltDate(Date lastBuiltDate) {
		this.lastBuiltDate = lastBuiltDate;
		return this;
	}
	public SessionData setLastBuiltDate(Object o) {
		if (null != o) {
			java.sql.Timestamp ts = (java.sql.Timestamp)o;
			this.setLastBuiltDate(PrearcUtils.timestamp2Date(ts));
		}
		return this;
	}
	
	public PrearcStatus getStatus() {
		return status;
	}
	public SessionData setStatus(PrearcStatus status) {
		this.status = status;
		return this;
	}
	public SessionData setStatus(Object o) {
		if (null != o) {
			this.setStatus((PrearcUtils.PrearcStatus)o);
		}
		return this;
	}
	public String getProject() {
		return sessionTriple.project;
	}
	public SessionData setProject(String project) {
		if (project != null) {
			this.sessionTriple.setProject(project);
		}
		else {
			this.sessionTriple.setProject(PrearcUtils.COMMON);
		}
		return this;
	}
	public SessionData setProject(Object o) {
		this.sessionTriple.setProject(o);
		return this;
	}
	
	public Date getScan_date() {
		return scan_date;
	}
	public SessionData setScan_date(Date scan_date) {
		this.scan_date = scan_date;
		return this;
	}
	public SessionData setScan_date(Object o) {
		if (null != o) {
			this.setScan_date((Date)o);
		}
		return this;
	}
	public String getScan_time() {
		return scan_time;
	}
	public SessionData setScan_time(String scan_time) {
		this.scan_time = scan_time;
		return this;
	}
	public SessionData setScan_time(Object o) {
		if (null != o) {
			this.setScan_time((String)o);
		}
		return this;
	}
	public String getSubject() {
		return subject;
	}
	public SessionData setSubject(String subject) {
		this.subject = subject;
		return this;
	}
	public SessionData setSubject(Object o) {
		if (null != o) {
			this.setSubject((String)o);
		}
		return this;
	}
	public String getExternalUrl() {
		return String.format("/prearchive/projects/%s/%s/%s",getProject(),getTimestamp(),getName());
	}
	public String getUrl() {
		return url;
	}
	public SessionData setUrl(String url) {
		this.url = url;
		return this;
	}
	public SessionData setUrl(Object o) {
		if (null != o) {
			this.setUrl((String)o);
		}
		return this;
	}
	
	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder();
		Field[] fs = SessionData.class.getDeclaredFields();
		for (int i = 0; i < fs.length; i++) {
			sb.append(fs[i].toString());
			sb.append(":");
			try {
				sb.append(fs[i].get(this).toString());
			}
			catch (IllegalAccessException e) {
				
			}
			sb.append("\n");
		}
		return sb.toString();		
	}
	
	public boolean nullCheck () throws IllegalArgumentException, IllegalAccessException {
		Field[] fs = SessionData.class.getDeclaredFields();
		String s = null;
		for (int i = 0; i < fs.length; i++) {
			if (fs[i].get(this) == null) {
				return false;
			}
		}
		return true;
	}
}