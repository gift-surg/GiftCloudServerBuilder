package org.nrg.xnat.helpers.prearchive;

import java.io.IOException;
import java.util.Collection;
/**
 * Retrieve the session data from some permanent store
 * @author aditya
 *
 */
public interface SessionDataProducerI {
	Collection<SessionData> get() throws IOException;
}
