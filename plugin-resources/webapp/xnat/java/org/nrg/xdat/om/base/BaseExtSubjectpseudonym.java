/*
 * GENERATED FILE
 * Created on Fri Nov 21 09:46:40 GMT 2014
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.turbine.utils.ArchivableItem;

import java.io.File;
import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseExtSubjectpseudonym extends AutoExtSubjectpseudonym {

	public BaseExtSubjectpseudonym(ItemI item)
	{
		super(item);
	}

	public BaseExtSubjectpseudonym(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseExtSubjectpseudonym(UserI user)
	 **/
	public BaseExtSubjectpseudonym()
	{}

	public BaseExtSubjectpseudonym(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
}
