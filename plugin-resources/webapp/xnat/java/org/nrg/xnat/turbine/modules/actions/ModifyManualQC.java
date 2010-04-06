package org.nrg.xnat.turbine.modules.actions;

import org.nrg.xdat.turbine.modules.actions.ModifyItem;

public class ModifyManualQC extends ModifyItem {

	@Override
	public boolean allowDataDeletion() {
		// the user should have the ability to "clear out" data on the edit
		// form. Be careful if secondary data is ever attached to the QC item,
		// as it would be cleared out.
		return true;
	}

}
