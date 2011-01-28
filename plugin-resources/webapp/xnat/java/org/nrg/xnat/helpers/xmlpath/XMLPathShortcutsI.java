package org.nrg.xnat.helpers.xmlpath;

import java.util.Map;

public interface XMLPathShortcutsI {
	public Map<String,Object> identifyFields(final Map<String,Object> params, final String TYPE,boolean readOnly);
	public Map<String,String> getShortcuts(final String type, final boolean readOnly);
}
