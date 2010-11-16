// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Sep 27 10:30:19 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.WrkAbstractexecutionenvironmentI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.WrkXnatexecutionenvironment;
import org.nrg.xdat.om.base.auto.AutoWrkWorkflowdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseWrkWorkflowdata extends AutoWrkWorkflowdata {

    public static final String AWAITING_ACTION = "AWAITING ACTION";
    public static final String FAILED = "FAILED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETE = "COMPLETE";
    public static final String ERROR = "ERROR";
    public static final String QUEUED = "QUEUED";

	public BaseWrkWorkflowdata(ItemI item)
	{
		super(item);
	}

	public BaseWrkWorkflowdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseWrkWorkflowdata(UserI user)
	 **/
	public BaseWrkWorkflowdata()
	{}

	public BaseWrkWorkflowdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}


    public boolean isActive(){
        if(this.getStatus().equalsIgnoreCase(COMPLETE))
            return false;
        if(this.getStatus().equalsIgnoreCase(ERROR))
            return false;
        if(this.getStatus().equalsIgnoreCase(FAILED))
            return false;

        return true;
    }

    public static ArrayList getWrkWorkflowdatasByField(org.nrg.xft.search.CriteriaCollection criteria, org.nrg.xft.security.UserI user,boolean preLoad, String sortField, String sortOrder)
    {
        ArrayList al = new ArrayList();
        try {
            org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(criteria,user,preLoad);
            Iterator iter = null;
            if (sortField != null && sortOrder != null) {
                iter = items.getItems(sortField,sortOrder).iterator();
            }else {
                iter = items.getItemIterator();
            }
            while (iter.hasNext())
            {
                WrkWorkflowdata vrc = new WrkWorkflowdata((XFTItem)iter.next());
                al.add(vrc);
            }
        } catch (Exception e) {
            logger.error("",e);
        }

        al.trimToSize();
        return al;
    }

    public String getOnlyPipelineName() {
        String rtn = getPipelineName();
        if (rtn.endsWith(File.separator)) rtn = rtn.substring(0,rtn.length());
        int lastIndexOfSlash = rtn.lastIndexOf(File.separator);
        if (lastIndexOfSlash != -1) {
            rtn = rtn.substring(lastIndexOfSlash + 1);
        }else {
           lastIndexOfSlash = rtn.lastIndexOf("/");
           if (lastIndexOfSlash != -1) 
               rtn = rtn.substring(lastIndexOfSlash + 1);
        }
        int lastIndexOfDot = rtn.lastIndexOf(".");
        if (lastIndexOfDot != -1 ) {
            rtn = rtn.substring(0,lastIndexOfDot);
        }
        return rtn;
    }


    /**
     * Constructs the XnatPipelineLauncher object for the most recent pipeline entry
     *
     * @param id The id that needs to be relaunched
     * @param dataType The Datatype of the id
     * @param pipeline The pipeline which needs to be launched
     * @param user The user who needs to relaunch the pipeline
     * @return XnatPipelineLauncher to relaunch the pipeline or null if the pipeline is not waiting
     */


    public  XnatPipelineLauncher getLatestLauncherByStatus(UserI user) {
       XnatPipelineLauncher rtn = null;
       //Look for the latest workflow entry for this pipeline
       //If its status is matches then construct the workflow
       String _status = getStatus();
       WrkAbstractexecutionenvironmentI absExecutionEnv = getExecutionenvironment();
       try {
           WrkXnatexecutionenvironment xnatExecutionEnv = (WrkXnatexecutionenvironment)absExecutionEnv;
           rtn = xnatExecutionEnv.getLauncher(user);
           if (_status.equalsIgnoreCase(AWAITING_ACTION)) {
               rtn.setStartAt(getNextStepId());
           }
       }catch(ClassCastException cse) {

       }
        return rtn;

    }

    /**
     * Constructs the XnatPipelineLauncher object to be used to restart a FAILED pipeline
     *
     * @param id The Workflow id that needs to be restarted
     * @param dataType The Datatype of the id
     * @param pipeline The pipeline which needs to be launched
     * @param user The user who needs to relaunch the pipeline
     * @return XnatPipelineLauncher to relaunch the pipeline or null if the pipeline hasnt failed
     */

    public  XnatPipelineLauncher restartWorkflow(UserI user) {
        return getLatestLauncherByStatus(user);
    }

    /**
     * Constructs the XnatPipelineLauncher object to be used to resume an awaiting pipeline
     *
     * @param id The Workflow id that needs to be resumed
     * @param dataType The Datatype of the id
     * @param pipeline The pipeline which needs to be launched
     * @param user The user who needs to relaunch the pipeline
     * @return XnatPipelineLauncher to relaunch the pipeline or null if the pipeline is not waiting
     */

    public XnatPipelineLauncher resumeWorkflow(UserI user) {
       return getLatestLauncherByStatus(user);
    }

    /**
     * Returns the most recent workflow status
     * @param id
     * @param data_type
     * @param external_id
     * @param user
     * @return
     */

    public static String GetLatestWorkFlowStatus(String id, String data_type, String external_id,org.nrg.xft.security.UserI user) {
        ArrayList wrkFlows = GetWorkFlowsOrderByLaunchTimeDesc(id,data_type,external_id,null, user);
        String rtn = "";
        if (wrkFlows != null && wrkFlows.size() > 0) {
            rtn = ((WrkWorkflowdata)wrkFlows.get(0)).getStatus();
        }
        return rtn;
    }

    /**
     * Returns the most recent workflow status for a pipeline
     * @param id
     * @param data_type
     * @param external_id
     * @param user
     * @return
     */

    public static String GetLatestWorkFlowStatus(String id, String data_type, String external_id,String pipelineName,org.nrg.xft.security.UserI user) {
        ArrayList wrkFlows = GetWorkFlowsOrderByLaunchTimeDesc(id,data_type,external_id,pipelineName,user);
        String rtn = "";
        if (wrkFlows != null && wrkFlows.size() > 0) {
            rtn = ((WrkWorkflowdata)wrkFlows.get(0)).getStatus();
        }
        return rtn;
    }

    /**
     * Returns the most recent workflow status for a pipeline
     * @param id
     * @param data_type
     * @param external_id
     * @param user
     * @return
     */

    public static String GetLatestWorkFlowStatusByPipeline(String id, String data_type, String pipelineName, String external_id,org.nrg.xft.security.UserI user) {
        ArrayList wrkFlows = GetWorkFlowsOrderByLaunchTimeDesc(id,data_type,external_id, pipelineName,user);
        String rtn = "";
        if (wrkFlows != null && wrkFlows.size() > 0) {
            rtn = ((WrkWorkflowdata)wrkFlows.get(0)).getStatus();
        }
        return rtn;
    }

    public static ArrayList GetWorkFlowsOrderByLaunchTimeDesc(String id, String dataType, String externalId, String pipelineName, org.nrg.xft.security.UserI user) {
        ArrayList workflows = new ArrayList();
        org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
        cc.addClause("wrk:workflowData.ID",id);
        cc.addClause("wrk:workflowData.data_type",dataType);
        if (externalId != null) cc.addClause("wrk:workflowData.ExternalID",externalId);
        if (pipelineName != null) cc.addClause("wrk:workflowData.pipeline_name",pipelineName);
        //Sort by Launch Time
        try {
            org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc,user,false);
            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
            Iterator iter = workitems.iterator();
            while (iter.hasNext())
            {
                WrkWorkflowdata vrc = new WrkWorkflowdata((XFTItem)iter.next());
                workflows.add(vrc);
            }
        }catch(Exception e) {
            logger.debug("",e);
        }
       logger.info("Workflows by Ordered by Launch Time " + workflows.size());
        return workflows;
    }



}
