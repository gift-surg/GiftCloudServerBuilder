//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataField;
import org.nrg.xdat.om.XnatExperimentdataFieldI;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatFielddefinitiongroup;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectdataI;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.base.auto.AutoXnatExperimentdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.identifier.IDGeneratorFactory;
import org.nrg.xft.identifier.IDGeneratorI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ArchivableItem;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public class BaseXnatExperimentdata extends AutoXnatExperimentdata implements ArchivableItem {

	public BaseXnatExperimentdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatExperimentdata(UserI user)
	{
		super(user);
	}

	public BaseXnatExperimentdata()
	{}

	public BaseXnatExperimentdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
    
    public String getArchiveDirectoryName(){
    	if(this.getLabel()!=null)
    		return this.getLabel();
    	else 
    		return this.getId();
    }


    public String getFormatedDate(){
        try {
            final Date d = this.getDateProperty("date");
            if (d!=null)
            {
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("MM/dd/yyyy");
                return formatter.format(d);
            }
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        } catch (FieldNotFoundException e) {
            logger.error("",e);
        } catch (ParseException e) {
            logger.error("",e);
        }
        return null;
    }

	public String getFreeFormDate(String dateParam){
		try{
			Date now = Calendar.getInstance().getTime();
			DateFormat dateFormat = new SimpleDateFormat(dateParam);
			String dateStr = dateFormat.format(now); 
			return dateStr;
		} catch (Exception e1) {logger.error(e1);return null;}
	}

    Hashtable fieldsByName = null;
    public Hashtable getFieldsByName(){
        if (fieldsByName == null){
            fieldsByName=new Hashtable();
            for(final XnatExperimentdataField field: this.getFields_field()){
                fieldsByName.put(field.getName(), field);
            }
        }

        return fieldsByName;
    }

    public Object getFieldByName(final String s){
        XnatExperimentdataFieldI field = (XnatExperimentdataFieldI)getFieldsByName().get(s);
        if (field!=null){
            return field.getField();
        }else{
            return null;
        }
    }

    public String getIdentifier(String project){
        return getIdentifier(project,false);
    }

    public String getIdentifier(final String project,final boolean returnNULL){
        if (project!=null){
        	if (this.getProject().equals(project)){
        		if (this.getLabel()!=null){
        			return this.getLabel();
        		}
        	}

        	for (final XnatExperimentdataShare pp : this.getSharing_share())
            {
            	if (pp.getProject().equals(project))
                {
                    if (pp.getLabel()!=null){
                        return pp.getLabel();
                    }
                }
            }
        }

        if (returnNULL){
            return null;
        }else{
            return getId();
        }
    }

    public XnatProjectdataI getProject(final String projectID, final boolean preLoad)
    {
        XnatExperimentdataShare ep = null;
        for (final XnatExperimentdataShare pp : this.getSharing_share())
        {
        	if (pp.getProject().equals(projectID))
            {
                ep=pp;
                break;
            }
        }

        try {
            if (ep!=null){
                return XnatProjectdata.getXnatProjectdatasById(ep.getProject(), this.getUser(), preLoad);
            }else if (this.getProject().equals(projectID)){
                return XnatProjectdata.getXnatProjectdatasById(this.getProject(), this.getUser(), preLoad);
            }
        } catch (RuntimeException e) {
            logger.error("",e);
        }

        return null;
    }

    public XnatProjectdata getPrimaryProject(boolean preLoad){
        if (this.getProject()!=null){
            return (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(getProject(), this.getUser(), preLoad);
        }else{
            return (XnatProjectdata)getFirstProject();
        }
    }


    public XnatProjectdataI getFirstProject()
    {
        XnatExperimentdataShare ep = null;
        if (!this.getSharing_share().isEmpty()){
        	ep = this.getSharing_share().get(0);
        }
        
        try {
            if (ep!=null){
                return XnatProjectdata.getXnatProjectdatasById(ep.getProject(), this.getUser(), false);
            }
        } catch (RuntimeException e) {
            logger.error("",e);
        }

        return null;
    }



    public String getIdentifiers(){
        Hashtable ids = new Hashtable();
        
        if (this.getProject()!=null){
        	if (this.getLabel()!=null){
        		ids.put(this.getLabel(), this.getProject());
        	}else{
        		ids.put(this.getId(), this.getProject());
        	}
        }
        
        for (final XnatExperimentdataShare pp : this.getSharing_share())
        {

            if (pp.getLabel()!=null){
                if (ids.containsKey(pp.getLabel()))
                {
                    ids.put(pp.getLabel(), ids.get(pp.getLabel()) + "," + pp.getProject());
                }else{
                    ids.put(pp.getLabel(), pp.getProject());
                }
            }else{
                if (ids.containsKey(this.getId()))
                {
                    ids.put(this.getId(), ids.get(this.getId()) + "," + pp.getProject());
                }else{
                    ids.put(this.getId(), pp.getProject());
                }
            }
        }

        String identifiers = new String();

        Enumeration enumer = ids.keys();
        int counter=0;
        while (enumer.hasMoreElements()){
            String key =(String) enumer.nextElement();
            if (counter++>0)identifiers=identifiers + ", ";
            identifiers=identifiers + key + " ("+ ids.get(key) + ")";
        }

        return identifiers;
    }
    
    


    public String name = null;
    public String description = null;
    public String secondaryID = null;
    private boolean initd = false;

    public void loadProjectDetails(){
        if (!initd)
        {
            initd=true;
            Object[] row=this.loadProjectDetails(this.getProject());
            if (row!=null)
            {
                name = (String)row[0];
                description = (String)row[1];
                secondaryID = (String)row[2];
            }
        }
    }
    
    public Object[] loadProjectDetails(String s){
    	try{
	    	XFTTable table = XFTTable.Execute("SELECT name,description,secondary_ID FROM xnat_projectData WHERE ID ='" + s + "';", this.getDBName(), null);
	
	        if (table.size()>0)
	        {
	            return (Object[])table.rows().get(0);
	        }
    	} catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        }
        
        return null;
    }

    public XnatProjectdata getProjectData(){
        return (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(this.getProject(), this.getUser(), false);
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

    public Hashtable<XnatProjectdataI,String> getProjectDatas(){
        Hashtable<XnatProjectdataI,String> hash = new Hashtable<XnatProjectdataI,String>();
        for(final XnatExperimentdataShare pp : this.getSharing_share()){
            if (pp.getLabel()==null)
            	if (this.getId()!=null)
            		hash.put(pp.getProjectData(), this.getId());
            	else
            		hash.put(pp.getProjectData(), "");
            else
                hash.put(pp.getProjectData(), pp.getLabel());
        }

        return hash;
    }


    public Collection<XnatFielddefinitiongroup> getFieldDefinitionGroups(String dataType){
        Hashtable<String,XnatFielddefinitiongroup> groups = new Hashtable<String,XnatFielddefinitiongroup>();
        Hashtable<XnatProjectdataI,String> projects = getProjectDatas();

        if (projects.size()==0){
            if (this.getProject()!=null){
                projects.put(this.getPrimaryProject(false), "");
            }
        }

        for(Map.Entry<XnatProjectdataI,String> entry : projects.entrySet()){
            XnatAbstractprotocol prot = ((XnatProjectdata)entry.getKey()).getProtocolByDataType(dataType);
            if (prot!=null && prot instanceof XnatDatatypeprotocol){
                XnatDatatypeprotocol dataProt=(XnatDatatypeprotocol)prot;
                for(XnatFielddefinitiongroup group : dataProt.getDefinitions_definition()){
                    groups.put(group.getId(), group);
                }
            }
        }

        return groups.values();
    }

    public static XnatExperimentdata GetExptByProjectIdentifier(String project, String identifier,XDATUser user,boolean preLoad){
        if(StringUtils.IsEmpty(identifier)){
        	return null;
        }
        
    	CriteriaCollection cc=new CriteriaCollection("OR");
            	
    	CriteriaCollection subcc1 = new CriteriaCollection("AND");
        subcc1.addClause("xnat:experimentData/project", project);
        subcc1.addClause("xnat:experimentData/label", identifier);
        
        cc.add(subcc1);
            	
    	CriteriaCollection subcc2 = new CriteriaCollection("AND");
    	subcc2.addClause("xnat:experimentData/sharing/share/project", project);
    	subcc2.addClause("xnat:experimentData/sharing/share/label", identifier);
        
        cc.add(subcc2);

        ArrayList al =  XnatExperimentdata.getXnatExperimentdatasByField(cc, user, preLoad);
        al = BaseElement.WrapItems(al);
        if (al.size()>0){
           return (XnatExperimentdata)al.get(0);
        }else{
            return null;
        }

    }
    
    public static synchronized String CreateNewID() throws Exception{
    	IDGeneratorI generator = IDGeneratorFactory.GetIDGenerator("org.nrg.xnat.turbine.utils.IDGenerator");
    	generator.setTable("xnat_experimentData");
    	generator.setDigits(5);
    	generator.setColumn("id");
    	return generator.generateIdentifier();
    }
    
    public void moveToProject(XnatProjectdata newProject,String newLabel,XDATUser user) throws Exception{
    	if(!this.getProject().equals(newProject.getId()))
    	{
    		if(!user.canEdit(this)){
    			throw new InvalidPermissionException(this.getXSIType());
    		}
    		
    		String existingRootPath=this.getProjectData().getRootArchivePath();
    		
    		if(newLabel==null)newLabel = this.getLabel();
    		if(newLabel==null)newLabel = this.getId();
    		
    		File newSessionDir = new File(new File(newProject.getRootArchivePath(),newProject.getCurrentArc()),newLabel);
    		
    		String current_label=this.getLabel();
    		if(current_label==null)current_label=this.getId();
    		
    		for(XnatAbstractresource abstRes:this.getResources_resource()){
    			String uri= null;
    			if(abstRes instanceof XnatResource){
    				uri=((XnatResource)abstRes).getUri();
    			}else{
    				uri=((XnatResourceseries)abstRes).getPath();
    			}
    			
    			if(FileUtils.IsAbsolutePath(uri)){
    				int lastIndex=uri.lastIndexOf(File.separator + current_label + File.separator);
    				if(lastIndex>-1)
    				{
    					lastIndex+=1+current_label.length();
    				}
    				if(lastIndex==-1){
    					lastIndex=uri.lastIndexOf(File.separator + this.getId() + File.separator);
        				if(lastIndex>-1)
        				{
        					lastIndex+=1+this.getId().length();
        				}
    				}
    				String existingSessionDir=null;
    				if(lastIndex>-1){
        				//in session_dir
        				existingSessionDir=uri.substring(0,lastIndex);
        			}else{
        				//outside session_dir
        				newSessionDir = new File(newSessionDir,"RESOURCES");
        				newSessionDir = new File(newSessionDir,"RESOURCES/"+abstRes.getXnatAbstractresourceId());
        				int lastSlash=uri.lastIndexOf("/");
        				if(uri.lastIndexOf("\\")>lastSlash){
        					lastSlash=uri.lastIndexOf("\\");
        				}
        				existingSessionDir=uri.substring(0,lastSlash);
        			}
        			abstRes.moveTo(newSessionDir,existingSessionDir,existingRootPath,user);
    			}else{
        			abstRes.moveTo(newSessionDir,null,existingRootPath,user);
    			}
    		}
    		
    		XFTItem current=this.getCurrentDBVersion(false);
    		current.setProperty("project", newProject.getId());
    		current.setProperty("label", newLabel);    		
    		current.save(user, true, false); 
    		
    		this.setProject(newProject.getId());
    		this.setLabel(newLabel);
    	}
    }
    
    public void moveToLabel(String newLabel,XDATUser user) throws Exception{
		if(!user.canEdit(this)){
			throw new InvalidPermissionException(this.getXSIType());
		}
		
    	XnatProjectdata proj = this.getPrimaryProject(false);
		
		String existingRootPath=proj.getRootArchivePath();
		
		if(newLabel==null)return;
		
		File newSessionDir = new File(new File(proj.getRootArchivePath(),proj.getCurrentArc()),newLabel);
		
		String current_label=this.getLabel();
		if(current_label==null)current_label=this.getId();
		
		for(XnatAbstractresource abstRes:this.getResources_resource()){
			String uri= null;
			if(abstRes instanceof XnatResource){
				uri=((XnatResource)abstRes).getUri();
			}else{
				uri=((XnatResourceseries)abstRes).getPath();
			}
			
			if(FileUtils.IsAbsolutePath(uri)){
				int lastIndex=uri.lastIndexOf(File.separator + current_label + File.separator);
				if(lastIndex>-1)
				{
					lastIndex+=1+current_label.length();
				}
				if(lastIndex==-1){
					lastIndex=uri.lastIndexOf(File.separator + this.getId() + File.separator);
    				if(lastIndex>-1)
    				{
    					lastIndex+=1+this.getId().length();
    				}
				}
				String existingSessionDir=null;
				if(lastIndex>-1){
    				//in session_dir
    				existingSessionDir=uri.substring(0,lastIndex);
    			}else{
    				//outside session_dir
    				newSessionDir = new File(newSessionDir,"RESOURCES");
    				newSessionDir = new File(newSessionDir,"RESOURCES/"+abstRes.getXnatAbstractresourceId());
    				int lastSlash=uri.lastIndexOf("/");
    				if(uri.lastIndexOf("\\")>lastSlash){
    					lastSlash=uri.lastIndexOf("\\");
    				}
    				existingSessionDir=uri.substring(0,lastSlash);
    			}
    			abstRes.moveTo(newSessionDir,existingSessionDir,existingRootPath,user);
			}else{
    			abstRes.moveTo(newSessionDir,null,existingRootPath,user);
			}
		}
		
		XFTItem current=this.getCurrentDBVersion(false);
		current.setProperty("label", newLabel);    		
		current.save(user, true, false); 
		
		this.setLabel(newLabel);
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
    
    public boolean hasProject(String proj_id){
	if(this.getProject().equals(proj_id)){
	    return true;
	}else{
	    for(XnatExperimentdataShare pp: this.getSharing_share()){
		if(pp.getProject().equals(proj_id)){
		    return true;
		}
	    }
	}
	
	return false;
    }
    

    
    public String canDelete(BaseXnatProjectdata proj, XDATUser user) {
    	BaseXnatExperimentdata expt=this;
    	if(this.getItem().getUser()!=null){
    		expt=new XnatExperimentdata(this.getCurrentDBVersion(true));
    	}
    	if(!expt.hasProject(proj.getId())){
    		return null;
    	}else {

			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				SchemaElement se= SchemaElement.GetElement(this.getXSIType());
				
				if (!user.canDeleteByXMLPath(se,values))
				{
					return "User cannot delete subjects for project " + proj.getId();
				}
			} catch (Exception e1) {
				return "Unable to delete subject.";
			}
			
    	}
    	return null;
    }
    
    public String delete(BaseXnatProjectdata proj, XDATUser user, boolean removeFiles){
    	BaseXnatExperimentdata expt=this;
    	if(this.getItem().getUser()!=null){
    		expt=new XnatExperimentdata(this.getCurrentDBVersion(true));
    	}
    	
    	String msg=expt.canDelete(proj,user);

    	if(msg!=null){
    		logger.error(msg);
    		return msg;
    	}
    	
    	if(!expt.getProject().equals(proj.getId())){
			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				
				if (!user.canDelete(expt) && !user.canDeleteByXMLPath(this.getSchemaElement(),values))
				{
					return "User cannot delete subjects for project " + proj.getId();
				}
				
				int index = 0;
				int match = -1;
				for(XnatExperimentdataShare pp : expt.getSharing_share()){
					if(pp.getProject().equals(proj.getId())){
						DBAction.RemoveItemReference(expt.getItem(), "xnat:experimentData/sharing/share", pp.getItem(), user);
						match=index;
						break;
					}
					index++;
				}
				
				if(match==-1)return null;
				
				this.removeSharing_share(match);
				return null;
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}else{
			try {
			
				if(!user.canDelete(this)){
					return "User account doesn't have permission to delete this experiment.";
				}
							
				if(removeFiles){
					this.deleteFiles();
				}
				
				String id=expt.getId();
		        
		        DBAction.DeleteItem(expt.getItem().getCurrentDBVersion(), user);
				
			    user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
				deleteWorkflowEntries(id, user);
				
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}
    	return null;
    }
    
    public void deleteWorkflowEntries(String id, XDATUser user){
    	try {
			ArrayList<WrkWorkflowdata> workflows = WrkWorkflowdata.getWrkWorkflowdatasByField("wrk:workFlowData.ID", id, user, false);
			
			for (WrkWorkflowdata wrk : workflows){
			    try {
					DBAction.DeleteItem(wrk.getItem(),user);
				} catch (Exception e) {
					logger.error("",e);
				}
			}
		} catch (Throwable e) {
			logger.error("",e);
		}
    }
    
    /**
     * This method looks for an existing session directory in the archive space.s
     * @return
     */
    public File getSessionDir(){
    	File archive=new File(ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject()));
    	if(archive.exists()){
    		for(File arc:archive.listFiles()){
    			if(!arc.getName().equals("subjects") && !arc.getName().equals("resources") && arc.isDirectory()){
    				for(File dir: arc.listFiles()){
    					if(dir.isDirectory() && dir.getName().equals(this.getArchiveDirectoryName())){
    						return dir;
    					}
    				}
    			}
    		}
    	}
    	
    	return null;
    }

    
    public void deleteFiles() throws IOException{
    	File dir=this.getSessionDir();
    	if(dir!=null){
    		FileUtils.MoveToCache(dir);
    	}
    	
    	for(XnatAbstractresource abstRes:this.getResources_resource()){
    		abstRes.deleteFromFileSystem(ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject()));
    	}
    }
    


    public static String cleanValue(String v){
    	v= StringUtils.ReplaceStr(v, " ", "_");
    	v= StringUtils.ReplaceStr(v, "`", "_");
    	v= StringUtils.ReplaceStr(v, "~", "_");
    	v= StringUtils.ReplaceStr(v, "@", "_");
    	v= StringUtils.ReplaceStr(v, "#", "_");
    	v= StringUtils.ReplaceStr(v, "$", "_");
    	v= StringUtils.ReplaceStr(v, "%", "_");
    	v= StringUtils.ReplaceStr(v, "^", "_");
    	v= StringUtils.ReplaceStr(v, "&", "_");
    	v= StringUtils.ReplaceStr(v, "*", "_");
    	v= StringUtils.ReplaceStr(v, "(", "_");
    	v= StringUtils.ReplaceStr(v, ")", "_");
    	v= StringUtils.ReplaceStr(v, "+", "_");
    	v= StringUtils.ReplaceStr(v, "=", "_");
    	v= StringUtils.ReplaceStr(v, "[", "_");
    	v= StringUtils.ReplaceStr(v, "]", "_");
    	v= StringUtils.ReplaceStr(v, "{", "_");
    	v= StringUtils.ReplaceStr(v, "}", "_");
    	v= StringUtils.ReplaceStr(v, "|", "_");
    	v= StringUtils.ReplaceStr(v, "\\", "_");
    	v= StringUtils.ReplaceStr(v, "/", "_");
    	v= StringUtils.ReplaceStr(v, "?", "_");
    	v= StringUtils.ReplaceStr(v, ":", "_");
    	v= StringUtils.ReplaceStr(v, ";", "_");
    	v= StringUtils.ReplaceStr(v, "\"", "_");
    	v= StringUtils.ReplaceStr(v, "'", "_");
    	v= StringUtils.ReplaceStr(v, ",", "_");
    	v= StringUtils.ReplaceStr(v, ".", "_");
    	v= StringUtils.ReplaceStr(v, "<", "_");
    	v= StringUtils.ReplaceStr(v, ">", "_");
    	
    	return v;
    }

    /**
     * Gets root path to the primary project's archive space.
     * @return
     */
    public String getArchiveRootPath(){
        final String path= getPrimaryProject(false).getRootArchivePath();

        return path;
    }

    /**
     * Gets root path to the primary project's cache space.
     * @return
     */
    public String getCachePath(){
        final String path= getPrimaryProject(false).getCachePath();

        return path;
    }

    /**
     * Gets root path to the primary project's prearchive space.
     * @return
     */
    public String getPrearchivePath(){
        final String path= getPrimaryProject(false).getPrearchivePath();

        return path;
    }

    /**
     * This returns the current sub folder within the project archive folder for placing sessions (ie arc001).
     * @return 
     * @throws InvalidArchiveStructure
     */
    public String getCurrentArchiveFolder() throws InvalidArchiveStructure{

          final String arcpath = this.getArchiveRootPath();

          String curA= getPrimaryProject(false).getCurrentArc();
          final File f = new File(arcpath);

          if (!f.exists()){
              f.mkdir();
          }

          //Map m = System.getenv();
          if (curA!=null)
          {
        	  logger.info("CURRENT_ARC:" + curA);
              if (!curA.endsWith("\\") && !curA.endsWith("/")){
                  curA += File.separator;
              }

              if (FileUtils.IsAbsolutePath(curA))
              {
                  final File currentArc = new File(curA);
                  if (!currentArc.exists()){
                      currentArc.mkdirs();
                  }

                  int index = curA.indexOf(f.getName());
                  if (index ==-1 )
                  {
                      throw new org.nrg.xnat.exceptions.InvalidArchiveStructure(f.getName() + " does not exist in " + curA);
                  }else{
                      curA = curA.substring(index + f.getName().length() + 1);

                      return curA;
                  }
              }else{
                  final File currentArc = new File(arcpath + curA);
                  if (!currentArc.exists()){
                      currentArc.mkdirs();
                  }

                  return curA;
              }
          }else{
              return null;
          }


    }
    
    /**
     * Returns path to the current archive folder for this experiment
     * @param absolute 
     * @return
     * @throws InvalidArchiveStructure
     */
    public String getCurrentSessionFolder(boolean absolute) throws InvalidArchiveStructure{
        String session_path;
        
        final String currentarc = this.getCurrentArchiveFolder();
        if (currentarc ==null){
            session_path = this.getArchiveDirectoryName() + "/";
        }else{
            session_path = currentarc.replace('\\', '/') + this.getArchiveDirectoryName() + "/";
        }
        
        if (absolute){
            session_path= FileUtils.AppendRootPath(this.getArchiveRootPath(), session_path);
        }
        
        return session_path;
    }
    
    /**
     * This method looks for an existing session directory in the archive space.  If none is found, it returns the location where said directory would be created.
     * @return 
     */
    public File getExpectedSessionDir() throws InvalidArchiveStructure{
    	final File sessionDIR=this.getSessionDir();
    	
    	if(sessionDIR==null){
    		return new File(this.getCurrentSessionFolder(true));
    	}
    	
    	return sessionDIR;
    }

	@Override
	public void preSave() throws Exception{
		super.preSave();
		
		final XnatProjectdata proj = this.getPrimaryProject(false);
		if(proj==null){
			throw new Exception("Unable to identify project for:" + this.getProject());
		}
		
		final String expectedPath=this.getExpectedSessionDir().getAbsolutePath().replace('\\', '/');
		
		for(final XnatAbstractresource res: this.getResources_resource()){
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
}
