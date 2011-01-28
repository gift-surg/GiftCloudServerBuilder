package org.nrg.xnat.helpers.prearchive;

/**
 * Sync sessions with the filesystem.
 * @author aditya
 *
 */
public final class FileSystemSessionData extends SessionDataDelegate implements SessionDataProducerI, SessionDataModifierI{
	public FileSystemSessionData (String basePath) {
		super(new FileSystemSessionTrawler(basePath), new FileSystemSessionDataModifier(basePath));
	}	
}
