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
	public String[] getColumns();
}
