//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 22, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;

public class ModifySubjectAssessorDataAllowDataDeletion extends ModifySubjectAssessorData{
	static Logger logger = Logger.getLogger(ModifySubjectAssessorDataAllowDataDeletion.class);

    public boolean allowDataDeletion(){
        return true;
    }
}
