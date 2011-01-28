package org.nrg.xnat.helpers.prearchive;

import java.io.IOException;
import java.util.Collection;

import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;

/**
 * A delegate object that combines functions for getting session data 
 * from and writing session data to some permanent store.
 * 
 * @author aditya
 *
 */
public abstract class SessionDataDelegate implements SessionDataProducerI, SessionDataModifierI {
	private SessionDataProducerI sp;
	private SessionDataModifierI sm;

	public SessionDataDelegate(SessionDataProducerI sp, SessionDataModifierI sm) {
		this.sp = sp;
		this.sm = sm;
	}
	
	public void setSp (SessionDataProducerI sp) {this.sp = sp;};
	public void setSm (SessionDataModifierI sm) {this.sm = sm;}
	public SessionDataProducerI getSp() {return sp;}
	public SessionDataModifierI getSm() {return sm;};
	public Collection<SessionData> get() throws IOException {
		return this.sp.get();
	}
	public void move(SessionData s, String newProj) throws java.io.SyncFailedException {
		this.sm.move(s, newProj);		
	}
	public void delete(SessionData sd) throws java.io.SyncFailedException {
		this.sm.delete(sd);
	}
	public void setStatus(SessionData sd, PrearcStatus status) {
		this.sm.setStatus(sd, status);
	}
}
