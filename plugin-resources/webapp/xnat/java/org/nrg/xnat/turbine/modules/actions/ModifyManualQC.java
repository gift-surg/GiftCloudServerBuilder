/*
 * org.nrg.xnat.turbine.modules.actions.ModifyManualQC
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.actions;


public class ModifyManualQC extends ModifyImageAssessorData {

	@Override
	public boolean allowDataDeletion() {
		// the user should have the ability to "clear out" data on the edit
		// form. Be careful if secondary data is ever attached to the QC item,
		// as it would be cleared out.
		return true;
	}

}
