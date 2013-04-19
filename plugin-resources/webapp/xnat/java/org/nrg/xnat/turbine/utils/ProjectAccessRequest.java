//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jan 31, 2008
 *
 */
package org.nrg.xnat.turbine.utils;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ItemAccessHistory;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.utils.WorkflowUtils;

import static org.nrg.xdat.om.XnatProjectdata.*;

public class ProjectAccessRequest {
    public static boolean CREATED_PAR_TABLE = false;

    public ProjectAccessRequest(final String query, final XDATUser user) throws SQLException, DBPoolException {
        if (!ProjectAccessRequest.CREATED_PAR_TABLE) {
            CreatePARTable();
        }

        final String dbName = user != null ? user.getDBName() : PoolDBUtils.getDefaultDBName();
        final String login = user != null ? user.getLogin() : null;

        XFTTable table = XFTTable.Execute(query, dbName, login);
        table.resetRowCursor();
        while (table.hasMoreRows()) {
            Object[] row = table.nextRow();
            if (row[0] != null) {
                setRequestId((Integer) row[0]);
            }

            if (row[1] != null) {
                setProjectId((String) row[1]);
            }

            if (row[2] != null) {
                setLevel((String) row[2]);
            }

            if (row[3] != null) {
                setCreateDate((Date) row[3]);
            }

            if (row[4] != null) {
                setUserId((Integer) row[4]);
            }

            if (row[5] != null) {
                setApprovalDate((Date) row[5]);
            }

            if (row[6] != null) {
                setApproved((Boolean) row[6]);
            }

            if (row[7] != null) {
                setApproverUserId((Integer) row[7]);
            }

            if (row[8] != null) {
                setUserString((String) row[8]);
            }

            if (row[9] != null) {
                setEmail((String) row[9]);
            }
        }
    }

    /**
     * @return the _userString
     */
    public String getUserString() {
        return _userString;
    }
    /**
     * @param userString the user string to set
     */
    public void setUserString(String userString) {
        _userString = userString;
    }
    /**
     * @return The user ID for the request approver
     */
    public Integer getApproverUserId() {
        return _approverUserId;
    }
    /**
     * @param approverUserId    The user ID to set for the request approver
     */
    public void setApproverUserId(Integer approverUserId) {
        _approverUserId = approverUserId;
    }
    /**
     * @return the username
     */
    public Integer getUserId() {
        return _userId;
    }

    /**
     * @param userId    The ID of the user
     */
    public void setUserId(Integer userId) {
        _userId = userId;
    }
    
    public String getEmail() {
		return _email;
	}

	public void setEmail(String email) {
		PoolDBUtils.CheckSpecialSQLChars(email);
		_email = email;
	}

	/**
     * @return the approvalDate
     */
    public Date getApprovalDate() {
        return _approvalDate;
    }
    /**
     * @param approvalDate the approvalDate to set
     */
    public void setApprovalDate(Date approvalDate) {
        _approvalDate = approvalDate;
    }
    /**
     * @return the createDate
     */
    public Date getCreateDate() {
        return _createDate;
    }
    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(Date createDate) {
        _createDate = createDate;
    }
    /**
     * @return the level
     */
    public String getLevel() {
        return _level;
    }
    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
		PoolDBUtils.CheckSpecialSQLChars(level);
        _level = level;
    }
    /**
     * @return the _projectId
     */
    public String getProjectId() {
        return _projectId;
    }
    /**
     * @param projectId the _projectId to set
     */
    public void setProjectId(String projectId) {
		PoolDBUtils.CheckSpecialSQLChars(projectId);
        _projectId = projectId;
    }
    /**
     * @return the approved
     */
    public boolean isApproved() {
        return _approved.booleanValue();
    }
    
    public Boolean getApproved(){
        return _approved;
    }
    
    public void save(XDATUser user) throws Exception{
        if (!ProjectAccessRequest.CREATED_PAR_TABLE) {
            CreatePARTable();
        }

        if (_requestId == null) {
            // New request
            StringBuilder query = new StringBuilder("INSERT INTO xs_par_table (proj_id, level, user_id");
            if (_approved !=null) {
                query.append(", approval_date, approved, approver_id");
            }
            query.append(")");
            query.append(" VALUES ('").append(_projectId).append("', '").append(_level).append("', ");
            query.append(_userId != null ? _userId : "NULL");
            if (_approved != null) {
                if (_approvalDate == null) {
                    query.append(", NOW()");
                } else {
                    query.append(", '").append(_approvalDate.toString()).append("'");
                }
                query.append(",").append(_approved).append(",").append(user.getXdatUserId());
            }
            query.append(");");
            PoolDBUtils.ExecuteNonSelectQuery(query.toString(), user.getDBName(), user.getLogin());
        } else {
            // Existing request
            StringBuilder query = new StringBuilder("UPDATE xs_par_table SET proj_id='");
            query.append(_projectId).append("', level='").append(_level).append("'");

            if(_userId != null) {
                query.append(", user_id=").append(_userId);
            }
            if(_email != null) {
                query.append(", email='").append(_email).append("'");
            }
            if (_approved != null) {
                if (_approvalDate == null) {
                    query.append(", approval_date=NOW()");
                } else {
                    query.append(", approval_date='").append(_approvalDate.toString()).append("'");
                }
                query.append(" ,approved=").append(_approved);
            }

            query.append(" WHERE par_id=").append(_requestId).append(";");
            
            PoolDBUtils.ExecuteNonSelectQuery(query.toString(), user.getDBName(), user.getLogin());
        }
    }
    
    /**
     * @return the _requestId
     */
    public Integer getRequestId() {
        return _requestId;
    }
    /**
     * @param requestId the _requestId to set
     */
    public void setRequestId(Integer requestId) {
        _requestId = requestId;
    }
    
    /**
     * @param approved the approved to set
     */
    public void setApproved(boolean approved) {
        _approved = approved;
    }
    
    public static ProjectAccessRequest RequestPARById(Integer id, XDATUser user) {
        return RequestPAR(" par_id=" + id, user);
    }
    
    public static ProjectAccessRequest RequestPARByUserProject(Integer userId, String projectId, XDATUser user) {
        return RequestPAR(" user_id=" + userId + " AND proj_id='" + projectId + "'", user);
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARsByUserEmail(String email, XDATUser user) {
        return RequestPARs(" xs_par_table.email='" + email + "' AND approval_date IS NULL", user);
    }

    public static ArrayList<ProjectAccessRequest> RequestPARsByProject(String p, XDATUser user) {
        return RequestPARs(" proj_id='" + p + "'", user);
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARByUserApprovalDate(Integer userId, Date lastLogin, XDATUser user) {
        return RequestPARs(" user_id=" + userId + " AND (approval_date>'" + lastLogin + "' OR approval_date IS NULL)", user);
    }
    
    public static ProjectAccessRequest RequestPAR(String where, XDATUser user) {
        try {
            String query = getPARQuery(where);
            return new ProjectAccessRequest(query, user);
        } catch (Exception exception) {
            _logger.error("Error occurred while requesting project access request for user [" + user.getUsername() + "]: " + where, exception);
            return null;
        }
    }
    
    public static ArrayList<ProjectAccessRequest> RequestPARs(String where, XDATUser user) {
        ArrayList<ProjectAccessRequest> PARs = new ArrayList<ProjectAccessRequest>();
        try {
            String query = getPARQuery(where);
            ProjectAccessRequest par = new ProjectAccessRequest(query, user);    
            PARs.add(par);
        } catch (Exception exception) {
            _logger.error("Error occurred while requesting project access requests for user [" + user.getUsername() + "]: " + where, exception);
        }
        return PARs;
    }
    
    public static void CreatePAR(String pID, String level, XDATUser user){
        try {
            if (!ProjectAccessRequest.CREATED_PAR_TABLE) {
                CreatePARTable();
            }
            
            String query = String.format("INSERT INTO xs_par_table (proj_id,user_id,level) VALUES ('%s', %d, '%s');", pID, user.getID(), StringUtils.RemoveChar(level, '\''));
            PoolDBUtils.ExecuteNonSelectQuery(query, user.getDBName(), user.getLogin());
        } catch (SQLException e) {
            _logger.error("", e);
        } catch (Exception e) {
            _logger.error("", e);
        }
    }


    public static synchronized void CreatePARTable() {
        try {
            if (!CREATED_PAR_TABLE) {
                
                String query = "SELECT relname FROM pg_catalog.pg_class WHERE relname = LOWER('xs_par_table');";
                String exists = (String) PoolDBUtils.ReturnStatisticQuery(query, "relname", PoolDBUtils.getDefaultDBName(), null);
               
                if (exists != null) {
                    CREATED_PAR_TABLE=true;
                    
                    //check for email column (added 6/2/08)
                    boolean containsEmail = false;
                    PoolDBUtils con = new PoolDBUtils();
                    XFTTable t = con.executeSelectQuery("select LOWER(attname) as col_name,typname, attnotnull from pg_attribute, pg_class,pg_type where attrelid = pg_class.oid AND atttypid=pg_type.oid AND attnum>0 and LOWER(relname) = 'xs_par_table';", PoolDBUtils.getDefaultDBName(), null);
        			while (t.hasMoreRows()) {
        				t.nextRow();
        				if (t.getCellValue("col_name").toString().equals("email")) {
        					containsEmail =true;
        					break;
        				}
        			}
        			
        			if (!containsEmail) {
        				query = "ALTER TABLE xs_par_table ADD COLUMN email VARCHAR(255);";
        				PoolDBUtils.ExecuteNonSelectQuery(query, PoolDBUtils.getDefaultDBName(), null);
        			}
                } else {
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
                    
                    PoolDBUtils.ExecuteNonSelectQuery(query, PoolDBUtils.getDefaultDBName(), null);
                    CREATED_PAR_TABLE = true;
                }
            }
        } catch (Exception exception) {
            _logger.error("Error occurred while creating PAR table", exception);
        }
    }
    
    public static void InviteUser(Context context, String invitee, XDATUser user, String subject) throws Exception {
    	XnatProjectdata project = (XnatProjectdata) context.get("projectOM");
    	ProjectAccessRequest request = null;
		try {
	         if (!ProjectAccessRequest.CREATED_PAR_TABLE) {
	             CreatePARTable();
	         }
	         
	         invitee = StringUtils.RemoveChar(invitee, '\'');
	         
	         StringBuilder query = new StringBuilder("INSERT INTO xs_par_table (email,proj_id,approver_id,level) VALUES ('");
             query.append(invitee).append("', '").append(project.getId()).append("', ").append(user.getID()).append(", '").append(context.get("access_level")).append("');");
	         
	         PoolDBUtils.ExecuteNonSelectQuery(query.toString(), user.getDBName(), user.getLogin());
	         
	         request = RequestPAR("xs_par_table.email = '" + invitee + "' AND proj_id = '" + project.getId() + "' AND approved IS NULL", user);
	    } catch (SQLException exception) {
	         _logger.error("Error occurred while running SQL query", exception);
	    } catch (Exception exception) {
	         _logger.error("General error occurred when inviting user " + invitee, exception);
	    }	
	    
	    context.put("par", request);
	
        StringWriter writer = new StringWriter();
        Template template = Velocity.getTemplate("/screens/InviteProjectAccessEmail.vm");
        template.merge(context, writer);

        String bcc = null;
        if (ArcSpecManager.GetInstance().getEmailspecifications_projectAccess()) {
	        bcc = AdminUtils.getAdminEmailId();
        }
        
        String from = AdminUtils.getAdminEmailId();

        try {
            XDAT.getMailService().sendHtmlMessage(from, invitee, user.getEmail(), bcc, subject, writer.toString());
        } catch (MessagingException exception) {
            _logger.error("Unable to send mail", exception);
            throw exception;
        }
    }
    
    public List<String> process(XDATUser user, boolean accept, EventUtils.TYPE eventType, String reason, String comment) throws Exception {
        return process(user, accept, eventType, reason, comment, true);
    }

    private List<String> process(final XDATUser user, final boolean accept, final EventUtils.TYPE eventType, final String reason, final String comment, final boolean processRelated) throws Exception {

        final String parEmail = getEmail();

    	setUserId(user.getXdatUserId());
		setApproved(accept);
		setApprovalDate(Calendar.getInstance().getTime());
        if (!org.apache.commons.lang.StringUtils.equalsIgnoreCase(parEmail, user.getEmail())) {
            setEmail(user.getEmail());
        }
        save(user);

		if (accept) {

			XnatProjectdata project = getXnatProjectdatasById(_projectId, null, false);
			
			PersistentWorkflowI workflow = WorkflowUtils.getOrCreateWorkflowData(null, user, SCHEMA_ELEMENT_NAME, _projectId, _projectId, EventUtils.newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, eventType, EventUtils.ADD_USER_TO_PROJECT, reason, comment));
			EventMetaI eventInfo = workflow.buildEvent();

			try {
				for (Map.Entry<String, UserGroup> entry : user.getGroups().entrySet()) {
					if (entry.getValue().getTag().equals(_projectId)) {
						for (XdatUserGroupid map : user.getGroups_groupid()) {
							if (map.getGroupid().equals(entry.getValue().getId())) {
								if(!map.getGroupid().endsWith("_owner")) {
								    SaveItemHelper.authorizedDelete(map.getItem(), user,eventInfo);
                                }
							}
						}
					}
				}

				if (!_level.startsWith(project.getId())) {
					_level = project.getId() + "_" + _level;
				}

				user.addGroup(project.addGroupMember(_level, user, user, eventInfo, true));

				WorkflowUtils.complete(workflow, eventInfo);
				
				try {
					ItemAccessHistory.LogAccess(user, project.getItem(), "report");
				} catch (Throwable e) {
					_logger.error("", e);
				}
			} catch (Exception e) {
				WorkflowUtils.fail(workflow, eventInfo);
				throw e;
			}
        }

        List<String> processedProjects = new ArrayList<String>() {{
            add(getProjectByIDorAlias(getProjectId(), user, false).getDisplayName());
        }};
        if (processRelated) {
            processedProjects.addAll(processRelatedPARs(parEmail, user, accept, eventType, reason, comment));
        }
        return processedProjects;
    }

    private List<String> processRelatedPARs(String parEmail, XDATUser user, boolean accept, EventUtils.TYPE eventType, String reason, String comment) {
        List<String> processedProjects = new ArrayList<String>();
        List<ProjectAccessRequest> parsForEmail = RequestPARsByUserEmail(parEmail, user);
        for(ProjectAccessRequest par : parsForEmail) {
            try {
                processedProjects.addAll(par.process(user, accept, eventType, reason, comment, false));
            } catch (Exception exception) {
                _logger.error("Error occurred trying to process project access request " + par.getRequestId());
            }
        }
        return processedProjects;
    }

    private static String getPARQuery(String where) {
        return String.format(QUERY_GET_PAR, where);
    }

    private static final Logger _logger = Logger.getLogger(ProjectAccessRequest.class);
    private static final String QUERY_GET_PAR = "SELECT par_id, proj_id, level, create_date, user_id, approval_date, approved, approver_id, firstname || ' ' || lastname || '(' || xdat_user.email || ')' as user_string, xs_par_table.email FROM xs_par_table LEFT JOIN xdat_user ON xs_par_table.user_id = xdat_user.xdat_user_id WHERE %s;";

    private Integer _requestId = null;
    private String _projectId = null;
    private String _level = null;
    private String _email = null;
    private Integer _userId = null;
    private Date _createDate = null;
    private Date _approvalDate = null;
    private Integer _approverUserId = null;
    private Boolean _approved = null;
    private String _userString = null;
}
