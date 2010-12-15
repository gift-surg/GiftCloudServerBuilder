package org.nrg.xnat.utils;

import java.util.Calendar;
import java.util.Collection;

import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;

public class WorkflowUtils {
	public static final String FAILED = "Failed";
	public static final String COMPLETE = "Complete";
	public static final String IN_PROGRESS="In Progress";
	public static final String RUNNING="Running";
	public static final String QUEUED="Queued";
	
	public static WrkWorkflowdata buildOpenWorkflow(final XDATUser user, final String xsiType,final String ID,final String project_id) throws FieldNotFoundException{
		WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
		workflow.setDataType(xsiType);
		workflow.setExternalid(project_id);
		workflow.setId(ID);
		workflow.setStatus(IN_PROGRESS);
		workflow.setLaunchTime(Calendar.getInstance().getTime());

		return workflow;
	}
	
	public static Collection<WrkWorkflowdata> getOpenWorkflows(final XDATUser user,final String ID) throws FieldNotFoundException{		
		//check to see if a process is already running.
		final CriteriaCollection cc= new CriteriaCollection("AND");
		cc.addClause("wrk:workFlowData.ID",ID);
		
		final CriteriaCollection cc2= new CriteriaCollection("OR");
		cc2.addClause("wrk:workFlowData.status",IN_PROGRESS);
		cc2.addClause("wrk:workFlowData.status",RUNNING);
		cc2.addClause("wrk:workFlowData.status",QUEUED);
		
		cc.add(cc2);
		
		final Collection<WrkWorkflowdata> al = WrkWorkflowdata.getWrkWorkflowdatasByField(cc, user, false);
		return al;

	}
}
