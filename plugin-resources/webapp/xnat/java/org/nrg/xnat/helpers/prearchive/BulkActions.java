/*
 * org.nrg.xnat.helpers.prearchive.BulkActions
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

import java.util.ArrayList;
import java.util.List;

public final class BulkActions {
	static List<Object> scheduledActions = java.util.Collections.synchronizedList(new ArrayList<Object>());
	
	// prevent instantiation
	private BulkActions() {} 
}
