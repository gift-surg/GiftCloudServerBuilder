package org.nrg.xnat.helpers.prearchive;

import java.util.HashMap;
import java.util.Map;

public class SessionDataTriple {
	public String name;
	public String timestamp;
	public String project;

	public SessionDataTriple() {
	}
	public String getName() {
		return this.name;
	}
	public SessionDataTriple setName(String name) {
		this.name = name;
		return this;
	}
	public SessionDataTriple setName(Object o) {
		if (o != null) {
			this.setName((String)o);
		}
		return this;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
	public SessionDataTriple setTimestamp(String timestamp) {
		this.timestamp = timestamp;
		return this;
	}
	public SessionDataTriple setTimestamp(Object o) {
		if (o != null) {
			this.setTimestamp((String)o);
		}
		return this;
	}
	
	public String getProject() {
		return this.project;
	}
	public SessionDataTriple setProject(String project) {
		if (project != null) {
			this.project = project;
		}
		else {
			this.project = PrearcUtils.COMMON;
		}
		return this;
	}
	public SessionDataTriple setProject(Object o) {
		this.setProject((String)o);
		return this;
	}
	
	public static SessionDataTriple makeTriple (String sess, String timestamp, String proj) {
		return new SessionDataTriple().setName(sess).setProject(proj).setTimestamp(timestamp);
	}
	
	public Map<String,String> toMap () {
		Map<String,String> ret = new HashMap<String,String>();
		ret.put("PROJECT_ID", this.getProject());
		ret.put("SESSION_TIMESTAMP", this.getTimestamp());
		ret.put("SESSION_LABEL", this.getName());
		return ret;
	}
	public static SessionDataTriple fromMap (Map<String,String> m) {
		return new SessionDataTriple().setName(m.get("SESSION_LABEL"))
		                              .setProject(m.get("PROJECT_ID"))
		                              .setTimestamp(m.get("SESSION_TIMESTAMP"));
	}
}