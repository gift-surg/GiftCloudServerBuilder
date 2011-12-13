/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.id;

import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface DicomProjectIdentifier extends DicomObjectFunction {
    XnatProjectdata apply(XDATUser user, DicomObject o);
}
