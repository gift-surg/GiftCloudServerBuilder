// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Jan 04 15:44:10 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.base.auto.AutoXnatImageassessordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatImageassessordata extends AutoXnatImageassessordata {

	public BaseXnatImageassessordata(ItemI item)
	{
		super(item);
	}

	public BaseXnatImageassessordata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImageassessordata(UserI user)
	 **/
	public BaseXnatImageassessordata()
	{}

	public BaseXnatImageassessordata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}



    private XnatImagesessiondata mr = null;

    public XnatImagesessiondata getImageSessionData()
    {
        if (mr==null)
        {
            ArrayList al = XnatImagesessiondata.getXnatImagesessiondatasByField("xnat:imageSessionData/ID",this.getImagesessionId(),this.getUser(),false);
            if (al.size()>0)
            {
                mr = (XnatImagesessiondata)al.get(0);
            }
        }

        return mr;
    }
    


    
    public boolean validateSessionId(){
        String session_id = this.getImagesessionId();
        
        if (session_id!=null){
			session_id=StringUtils.RemoveChar(session_id, '\'');
            String query = "SELECT ID FROM xnat_imageSessiondata WHERE ID='";
            String login =null;
            if (this.getUser()!=null){
                login = this.getUser().getUsername();
            }
            
            try {
               final String idCOUNT= (String)PoolDBUtils.ReturnStatisticQuery(query + session_id + "';", "id", this.getDBName(), login);
                if (idCOUNT!=null){
                    return true;
                }
                
                final String project = this.getProject();
                if (project!=null){
                	query = "SELECT id FROM xnat_experimentData WHERE label='" +
                    session_id +"' AND project='" + project + "';";
		            String new_session_id= (String)PoolDBUtils.ReturnStatisticQuery(query, "id", this.getDBName(), login);
		            if (new_session_id!=null){
		                this.setImagesessionId(new_session_id);
		                return true;
		            }
                	
                    query = "SELECT sharing_share_xnat_experimentda_id FROM xnat_experimentData_share WHERE label='" +
                            session_id +"' AND project='" + project + "';";
                    new_session_id= (String)PoolDBUtils.ReturnStatisticQuery(query, "sharing_share_xnat_experimentda_id", this.getDBName(), login);
                    if (new_session_id!=null){
                        this.setImagesessionId(new_session_id);
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.error("",e);
            } catch (Exception e) {
                logger.error("",e);
            }
        }
        
        return false;
    }
    
    public ArrayList getCatalogSummary() throws Exception{
		String query="SELECT xnat_abstractresource_id,label,element_name ";
    	query+=", 'resources'::TEXT AS category, '" + this.getId()+"'::TEXT AS cat_id";
		query+=" FROM xnat_experimentdata_resource map " +
		" LEFT JOIN xnat_abstractresource abst ON map.xnat_abstractresource_xnat_abstractresource_id=abst.xnat_abstractresource_id" +
		" LEFT JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id";
		query+= " WHERE xnat_experimentdata_id='"+this.getId() + "'";
		
		XFTTable t = XFTTable.Execute(query, this.getDBName(), "system");
		
		return t.rowHashs();
    }
    
    
    public void deleteFiles() throws IOException{
    	super.deleteFiles();
    	
    	for(XnatAbstractresourceI abstRes:this.getResources_resource()){
    		((XnatAbstractresource)abstRes).deleteFromFileSystem(ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject()));
    	}
    	
    	for(XnatAbstractresourceI abstRes:this.getOut_file()){
    		((XnatAbstractresource)abstRes).deleteFromFileSystem(ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject()));
    	}
    }


	public File getExpectedSessionDir() throws InvalidArchiveStructure,UnknownPrimaryProjectException{
		return this.getImageSessionData().getExpectedSessionDir();
	}

	@Override
	public void preSave() throws Exception{
		if(StringUtils.IsEmpty(this.getId())){
			throw new IllegalArgumentException();
		}	
		
		if(StringUtils.IsEmpty(this.getLabel())){
			throw new IllegalArgumentException();
		}
		
		if(StringUtils.IsAlphaNumericUnderscore(getId())){
			throw new IllegalArgumentException("Identifiers cannot use special characters.");
		}
		
		if(StringUtils.IsAlphaNumericUnderscore(getLabel())){
			throw new IllegalArgumentException("Labels cannot use special characters.");
		}
		
		if(this.getImageSessionData()==null){
			throw new Exception("Unable to identify image session for:" + this.getImagesessionId());
		}
		
		final XnatProjectdata proj = this.getPrimaryProject(false);
		if(proj==null){
			throw new Exception("Unable to identify project for:" + this.getProject());
		}
		
		final String expectedPath=this.getExpectedSessionDir().getAbsolutePath().replace('\\', '/');
		
		for(final XnatAbstractresourceI res: this.getResources_resource()){
			final String uri;
			if(res instanceof XnatResource){
				uri=((XnatResource)res).getUri();
			}else if(res instanceof XnatResourceseries){
				uri=((XnatResourceseries)res).getPath();
			}else{
				continue;
			}
			
			FileUtils.ValidateUriAgainstRoot(uri,expectedPath,"URI references data outside of the project:" + uri);
		}
		
		for(final XnatAbstractresourceI res: this.getOut_file()){
			final String uri;
			if(res instanceof XnatResource){
				uri=((XnatResource)res).getUri();
			}else if(res instanceof XnatResourceseries){
				uri=((XnatResourceseries)res).getPath();
			}else{
				continue;
			}
			
			FileUtils.ValidateUriAgainstRoot(uri,expectedPath,"URI references data outside of the project:" + uri);
		}
	}

	public String getResourceCatalogRootPathByLabel( String label) {
		String rtn = super.getResourceCatalogRootPathByLabel(label);
        if (rtn == null) {
        	//Check if catalog is at the out file level
        	Iterator misc = this.getOut_file().iterator();
            while(misc.hasNext())          {
                Object file = misc.next();
           	    if (file instanceof XnatResourcecatalog) {
           	    	String tag = ((XnatResourcecatalog)file).getLabel();
           	    	if (tag != null && tag.equals(label)) {
           	    		rtn =((XnatResourcecatalog)file).getUri();
           	    		int index = rtn.lastIndexOf("/");
           	    		if (index != -1)
           	    			rtn = rtn.substring(0, index);
           	    		break;
           	    	}
        	    }
            }
        }
        return rtn;
	}


}
