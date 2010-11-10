package org.nrg.xnat.helpers.prearchive;

import java.util.Date;

import org.nrg.xft.XFTTable;

public interface ProjectPrearchiveI {

	public abstract Date getLastMod();

	public abstract XFTTable getContent();

}