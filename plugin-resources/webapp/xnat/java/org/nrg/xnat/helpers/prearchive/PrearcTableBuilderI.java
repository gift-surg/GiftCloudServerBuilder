/**
 * 
 */
package org.nrg.xnat.helpers.prearchive;

import java.io.IOException;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.InvalidPermissionException;

/**
 * @author timo
 *
 */
public interface PrearcTableBuilderI {
	/**
	 * I would much rather create an interface that the Representation implementations use so that I wouldn't 
	 * have to keep using this ugly XFTTable thing.  But, that is beyond the scope here, and this will work.
	 * @param projects
	 * @return
	 */
	public ProjectPrearchiveI buildTable(final String project, final XDATUser user, final String urlBase)throws IOException, InvalidPermissionException, Exception;
	
	public String[] getColumns();
}
