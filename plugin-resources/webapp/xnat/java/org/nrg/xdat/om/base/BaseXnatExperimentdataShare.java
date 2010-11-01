// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Apr 03 13:48:02 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatExperimentdataShare;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatExperimentdataShare extends AutoXnatExperimentdataShare {

	public BaseXnatExperimentdataShare(ItemI item)
	{
		super(item);
	}

	public BaseXnatExperimentdataShare(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatExperimentdataShare(UserI user)
	 **/
	public BaseXnatExperimentdataShare()
	{}

	public BaseXnatExperimentdataShare(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    private String name = null;
    private String description = null;
    private String secondaryID = null;
    private boolean initd = false;

    public void loadProjectDetails(){
        if (!initd)
        {
            initd=true;
            try {
                XFTTable table = XFTTable.Execute("SELECT name,description,secondary_ID FROM xnat_projectData WHERE ID ='" + this.getProject() + "';", this.getDBName(), null);

                if (table.size()>0)
                {
                    Object[] row = (Object[])table.rows().get(0);
                    name = (String)row[0];
                    description = (String)row[1];
                    secondaryID = (String)row[2];
                }
            } catch (SQLException e) {
                logger.error("",e);
            } catch (DBPoolException e) {
                logger.error("",e);
            }
        }
    }

    public XnatProjectdataI getProjectData(){
        return XnatProjectdata.getXnatProjectdatasById(this.getProject(), this.getUser(), false);
    }


    /**
     * @return the description
     */
    public String getDescription() {
        loadProjectDetails();
        return description;
    }

    /**
     * @return the name
     */
    public String getProjectName() {
        loadProjectDetails();
        return name;
    }

    /**
     * @return the secondaryID
     */
    public String getProjectSecondaryID() {
        loadProjectDetails();
        return secondaryID;
    }



    /**
     * @return the secondaryID
     */
    public String getProjectDisplayID() {
        loadProjectDetails();
        if (secondaryID!=null){
            return secondaryID;
        }else{
           return getProject();
        }
    }
}
