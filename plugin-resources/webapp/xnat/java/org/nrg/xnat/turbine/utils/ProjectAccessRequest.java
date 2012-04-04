//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jan 31, 2008
 *
 */
package org.nrg.xnat.turbine.utils;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ItemAccessHistory;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;

public class ProjectAccessRequest {
    static org.apache.log4j.Logger logger = Logger.getLogger(ProjectAccessRequest.class);
    public static boolean CREATED_PAR_TABLE = false;
    
    private Integer par_id=null;
    private String projectID=null;
    private String level = null;
    private String email = null;
    private Integer UserId=null;
    private Date createDate = null;
    
    private Date approvalDate = null;
    private Integer approver_UserId=null;
    private Boolean approved=null;
    
    
    private String userString=null;
    
    
    /**
     * @return the userString
     */
    public String getUserString() {
        return userString;
    }
    /**
     * @param userString the userString to set
     */
    public void setUserString(String userString) {
        this.userString = userString;
    }
    /**
     * @return the approver_username
     */
    public Integer getApprover_UserId() {
        return approver_UserId;
    }
    /**
     * @param approver_username the approver_username to set
     */
    public void setApprover_UserId(Integer approver_UserId) {
        this.approver_UserId = approver_UserId;
    }
    /**
     * @return the username
     */
    public Integer getUserId() {
        return UserId;
    }

    /**
     * @param username the username to set
     */
    public void setUserId(Integer UserId) {
        this.UserId = UserId;
    }
    
    
    public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		PoolDBUtils.CheckSpecialSQLChars(email);
		this.email = email;
	}
	/**
     * @return the approvalDate
     */
    public Date getApprovalDate() {
        return approvalDate;
    }
    /**
     * @param approvalDate the approvalDate to set
     */
    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }
    /**
     * @return the createDate
     */
    public Date getCreateDate() {
        return createDate;
    }
    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }
    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
		PoolDBUtils.CheckSpecialSQLChars(level);
        this.level = level;
    }
    /**
     * @return the projectID
     */
    public String getProjectID() {
        return projectID;
    }
    /**
     * @param projectID the projectID to set
     */
    public void setProjectID(String projectID) {
		PoolDBUtils.CheckSpecialSQLChars(projectID);
        this.projectID = projectID;
    }
    /**
     * @return the approved
     */
    public boolean isApproved() {
        return approved.booleanValue();
    }
    
    public Boolean getApproved(){
        return approved;
    }
    
    public void moveOtherPARs(XDATUser user) {
    	if(!this.email.equalsIgnoreCase(user.getEmail())){
    		ArrayList<ProjectAccessRequest> pars=RequestPARsByUserEmeail(this.getEmail(), user);
    		for(ProjectAccessRequest par : pars){
    			try {
					if(par.approved==null){
						par.setEmail(user.getEmail());
						par.save(user);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    	}
    }
    
    public void save(XDATUser user) throws Exception{
        if (!ProjectAccessRequest.CREATED_PAR_TABLE){
            CreatePARTable(user);
        }
        if (this.par_id==null){
            //NEW
            String query="INSERT INTO xs_par_table (";
            
            query+="proj_id,level,user_id";
            if (this.approved!=null)
                query+=",approval_date,approved,approver_id";
            query+=")";
            
            query+=" VALUES ('" + this.projectID +"','" + level +"',";
            if (this.UserId!=null)
            	query+=this.UserId;
            else
            	query +=" NULL ";
            if (this.approved!=null){
                if (approvalDate==null){
                    query+=",NOW()";
                }else{
                    query+=",'" + approvalDate.toString() +"'";
                }
                query+="," + approved + "," + user.getXdatUserId();
            }
            query+=");";
            
            PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
        }else{
            //OLD
            String query="UPDATE xs_par_table ";
            

            
            query+=" SET proj_id='" + this.projectID +"',level='" + level +"'";
            if(this.UserId!=null)
            query+=",user_id=" +this.UserId;
            if(this.email!=null)
                query+=",email='" +this.email + "'";
            
            if (this.approved!=null){
                if (approvalDate==null){
                    query+=",approval_date=NOW()";
                }else{
                    query+=",approval_date='" + approvalDate.toString() +"'";
                }
                query+=" ,approved=" + approved;
            }
            query+=" WHERE par_id=" + par_id + ";";
            
            PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
        }
    }
    
    /**
     * @return the par_id
     */
    public Integer getPar_id() {
        return par_id;
    }
    /**
     * @param par_id the par_id to set
     */
    public void setPar_id(Integer par_id) {
        this.par_id = par_id;
    }
    
    /**
     * @param approved the approved to set
     */
    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    
    public static ProjectAccessRequest RequestPARById(Integer id,XDATUser user){
        return RequestPAR(" par_id=" + id, user);
    }
    
    public static ProjectAccessRequest RequestPARByUserProject(Integer userId, String proj_id,XDATUser user){
        return RequestPAR(" user_id=" + userId + " AND proj_id='" + proj_id + "'", user);
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARsByUserEmeail(String email,XDATUser user){
        return RequestPARs(" xs_par_table.email='" + email + "' AND approval_date IS NULL", user);
    }

    public static ArrayList<ProjectAccessRequest> RequestPARsByProject(String p,XDATUser user){
        return RequestPARs(" proj_id='" + p + "'", user);
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARByUserApprovalDate(Integer userId, Date lastLogin,XDATUser user){
        return RequestPARs(" user_id=" + userId + " AND (approval_date>'" + lastLogin + "' OR approval_date IS NULL)", user);
    }
    
    public static ProjectAccessRequest RequestPAR(String wherequery,XDATUser user){
        if (!ProjectAccessRequest.CREATED_PAR_TABLE){
            CreatePARTable(user);
        }
        
        String query="SELECT par_id,proj_id,level,create_date,user_id,approval_date,approved,approver_id,firstname || ' ' || lastname || '(' || xdat_user.email || ')' as user_string,xs_par_table.email FROM xs_par_table LEFT JOIN xdat_user ON xs_par_table.user_id=xdat_user.xdat_user_id WHERE "+wherequery +";";
        try {
            XFTTable t = XFTTable.Execute(query, GenericWrapperElement.GetElement("xdat:user").getDbName(), null);
            
            if (t.rows().size()>0){
                Object[] row=(Object[])t.rows().get(0);
                ProjectAccessRequest par = new ProjectAccessRequest();
                if (row[0]!=null){
                    par.setPar_id((Integer)row[0]);
                }
                
                if (row[1]!=null){
                    par.setProjectID((String)row[1]);
                }
                
                if (row[2]!=null){
                    par.setLevel((String)row[2]);
                }
                
                if (row[3]!=null){
                    par.setCreateDate((Date)row[3]);
                }
                
                if (row[4]!=null){
                    par.setUserId((Integer)row[4]);
                }
                
                if (row[5]!=null){
                    par.setApprovalDate((Date)row[5]);
                }
                
                if (row[6]!=null){
                    par.setApproved((Boolean)row[6]);
                }
                
                if (row[7]!=null){
                    par.setApprover_UserId((Integer)row[7]);
                }
                
                if (row[8]!=null){
                    par.setUserString((String)row[8]);
                }
                
                if (row[9]!=null){
                    par.setEmail((String)row[9]);
                }
                
                return par;
            }else{
                return null;
            }
        } catch (SQLException e) {
            logger.error("",e);
            return null;
        } catch (DBPoolException e) {
            logger.error("",e);
            return null;
        } catch (Exception e) {
            logger.error("",e);
            return null;
        }
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARs(String whereClause, XDATUser user){
        ArrayList<ProjectAccessRequest> PARs = new ArrayList<ProjectAccessRequest>();
        try {
            if (!ProjectAccessRequest.CREATED_PAR_TABLE){
                CreatePARTable(user);
            }
                String query="SELECT par_id,proj_id,level,create_date,user_id,approval_date,approved,approver_id,firstname || ' ' || lastname || '(' || xdat_user.email || ')' as user_string,xs_par_table.email FROM xs_par_table LEFT JOIN xdat_user ON xs_par_table.user_id=xdat_user.xdat_user_id WHERE " +whereClause + ";";
                XFTTable t = XFTTable.Execute(query, user.getDBName(), user.getLogin());
                
                t.resetRowCursor();
                while(t.hasMoreRows()){
                    Object[] row=t.nextRow();
                    ProjectAccessRequest par = new ProjectAccessRequest();
                    if (row[0]!=null){
                        par.setPar_id((Integer)row[0]);
                    }
                    
                    if (row[1]!=null){
                        par.setProjectID((String)row[1]);
                    }
                    
                    if (row[2]!=null){
                        par.setLevel((String)row[2]);
                    }
                    
                    if (row[3]!=null){
                        par.setCreateDate((Date)row[3]);
                    }
                    
                    if (row[4]!=null){
                        par.setUserId((Integer)row[4]);
                    }
                    
                    if (row[5]!=null){
                        par.setApprovalDate((Date)row[5]);
                    }
                    
                    if (row[6]!=null){
                        par.setApproved((Boolean)row[6]);
                    }
                    
                    if (row[7]!=null){
                        par.setApprover_UserId((Integer)row[7]);
                    }
                    
                    if (row[8]!=null){
                        par.setUserString((String)row[8]);
                    }
                    
                    if (row[9]!=null){
                        par.setEmail((String)row[9]);
                    }
                    
                    PARs.add(par);
                }
        } catch (SQLException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
        
        return PARs;
    }
    
    public static void CreatePAR(String pID,String level,XDATUser user){
        try {
            if (!ProjectAccessRequest.CREATED_PAR_TABLE){
                CreatePARTable(user);
            }
            
 			level=StringUtils.RemoveChar(level, '\'');
            String query="INSERT INTO xs_par_table (proj_id,user_id,level)" +
                    " VALUES ('" + pID +"'," + user.getID() +",'" + level +"');";
            
            PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
        } catch (SQLException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
    }


    public static void CreatePARTable(XDATUser user){
        try {
            if (!CREATED_PAR_TABLE){
                
                String query ="SELECT relname FROM pg_catalog.pg_class WHERE  relname=LOWER('xs_par_table');";
                String exists =(String)PoolDBUtils.ReturnStatisticQuery(query, "relname", user.getDBName(), user.getLogin());
               
                if (exists!=null){
                    CREATED_PAR_TABLE=true;
                    
                    //check for email column (added 6/2/08)
                    boolean containsEmail = false;
                    PoolDBUtils con = new PoolDBUtils();
                    XFTTable t = con.executeSelectQuery("select LOWER(attname) as col_name,typname, attnotnull from pg_attribute, pg_class,pg_type where attrelid = pg_class.oid AND atttypid=pg_type.oid AND attnum>0 and LOWER(relname) = 'xs_par_table';",user.getDBName(),null);
        			while (t.hasMoreRows())
        			{
        				t.nextRow();
        				if (t.getCellValue("col_name").toString().equals("email")){
        					containsEmail =true;
        					break;
        				}
        			}
        			
        			if (!containsEmail){
        				query = "ALTER TABLE xs_par_table ADD COLUMN email VARCHAR(255);";
        				PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
        			}
                }else{
                    query = "CREATE TABLE xs_par_table"+
                    "\n("+
                    "\n  par_id SERIAL,"+
                    "\n  proj_id VARCHAR(255),"+
                    "\n  level VARCHAR(255),"+
                    "\n  create_date timestamp DEFAULT now(),"+
                    "\n  user_id integer,"+
                    "\n  approval_date timestamp ,"+
                    "\n  approved boolean,"+
                    "\n  approver_id integer," +
                    "\n  email VARCHAR(255)," +
                    "\n  CONSTRAINT xs_par_table_pkey PRIMARY KEY (par_id)"+
                    "\n) "+
                    "\nWITH OIDS;";
                    
                    PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
                    
                    CREATED_PAR_TABLE=true;
                }
            }
        } catch (SQLException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
    }
    
    public static void InviteUser(Context context,String otherUemail,XDATUser user,String subject) throws Exception{
    	XnatProjectdata project = (XnatProjectdata)context.get("projectOM");
    	ProjectAccessRequest request = null;
		try {
	         if (!ProjectAccessRequest.CREATED_PAR_TABLE){
	             CreatePARTable(user);
	         }
	         
	         otherUemail=StringUtils.RemoveChar(otherUemail, '\'');
	         
	         String query="INSERT INTO xs_par_table (email,proj_id,approver_id,level)" +
	                 " VALUES ('" + otherUemail + "','" + project.getId() +"'," + user.getID() +",'" + context.get("access_level") +"');";
	         
	         PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
	         
	         request = RequestPAR(" xs_par_table.email='" + otherUemail + "' AND proj_id='" + project.getId() + "' AND approved IS NULL", user);
	    } catch (SQLException e) {
	         logger.error("",e);
	    } catch (Exception e) {
	         logger.error("",e);
	    }	
	    
	    context.put("par", request);
	
        StringWriter sw = new StringWriter();
        Template template =Velocity.getTemplate("/screens/InviteProjectAccessEmail.vm");
        template.merge(context,sw);
        String message= sw.toString();

        String bcc = null;
        if(ArcSpecManager.GetInstance().getEmailspecifications_projectAccess()){
	        bcc = AdminUtils.getAdminEmailId();
        }
        
        String from = AdminUtils.getAdminEmailId();

        try {
            XDAT.getMailService().sendHtmlMessage(from, otherUemail, user.getEmail(), bcc, subject, message);
        } catch (MessagingException exception) {
            logger.error("Unable to send mail", exception);
            throw exception;
        }
    }
    
    public void process(XDATUser user,boolean accept) throws Exception{
    	this.setUserId(user.getXdatUserId());
		this.setApproved(accept);
		this.setApprovalDate(Calendar.getInstance().getTime());
		this.save(user);
		
		this.moveOtherPARs(user);

		if(accept){

			XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectID, null, false);
			
			if(!user.canDelete(project)){
				throw new InvalidPermissionException("User cannot modify project settings");
			}
			
			final String projectID = this.getProjectID();

			for (Map.Entry<String, UserGroup> entry : user.getGroups().entrySet()) {
				if (entry.getValue().getTag().equals(projectID)) {
					for (XdatUserGroupid map : user.getGroups_groupid()) {
						if (map.getGroupid().equals(entry.getValue().getId())) {
							if(!map.getGroupid().endsWith("_owner"))
								SaveItemHelper.authorizedDelete(map.getItem(), user);
						}
					}
				}
			}

			String level = this.getLevel();

			if (!level.startsWith(project.getId())) {
				level = project.getId() + "_" + level;
			}
			user.addGroup(project.addGroupMember(level, user, user));

			try {
				WrkWorkflowdata workflow = new WrkWorkflowdata((UserI) user);
				workflow.setDataType("xnat:projectData");
				workflow.setExternalid(project.getId());
				workflow.setId(project.getId());
				workflow.setPipelineName("New " + this.getLevel() + ": " + user.getFirstname() + " " + user.getLastname());
				workflow.setStatus("Complete");
				workflow.setLaunchTime(Calendar.getInstance().getTime());
				SaveItemHelper.authorizedSave(workflow,user, false, false);

				ItemAccessHistory.LogAccess(user, project.getItem(), "report");
			} catch (Throwable e) {
				logger.error("",e);
			}
		}
    }
}
