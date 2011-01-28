package org.nrg.xnat.helpers.prearchive;

public interface PrearcSessionDataModifierI {

	public abstract void setStatus(SessionData sd,
			final PrearcUtils.PrearcStatus status);

}