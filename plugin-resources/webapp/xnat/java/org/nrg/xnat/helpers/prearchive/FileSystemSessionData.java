/*
 * org.nrg.xnat.helpers.prearchive.FileSystemSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

public final class FileSystemSessionData extends SessionDataDelegate implements SessionDataProducerI, SessionDataModifierI{
	public FileSystemSessionData (String basePath) {
		super(new FileSystemSessionTrawler(basePath), new FileSystemSessionDataModifier(basePath));
	}	
}
