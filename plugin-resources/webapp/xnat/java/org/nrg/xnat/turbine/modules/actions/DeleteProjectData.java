//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jun 19, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.Iterator;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.model.XnatSubjectassessordataI;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XdatUsergroup;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.search.CriteriaCollection;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xnat.utils.WorkflowUtils;
import org.nrg.xft.utils.SaveItemHelper;

public class DeleteProjectData extends SecureAction {
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DeleteProjectData.class);

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        final String projectID = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
        final XDATUser user = (XDATUser)TurbineUtils.getUser(data);
        final XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(projectID, user, false);
        boolean preventProjectDelete=false;
        boolean preventProjectDeleteByP=false;
                
        if(user.canDelete(project)){
            
            final PersistentWorkflowI workflow=WorkflowUtils.getOrCreateWorkflowData(null, user, XnatProjectdata.SCHEMA_ELEMENT_NAME, project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ADMIN,EventUtils.getDeleteAction(project.getXSIType())));
    		final EventMetaI ci=workflow.buildEvent();
            PersistentWorkflowUtils.save(workflow,ci);
    	    
            try {
				for (XnatSubjectdata subject : project.getParticipants_participant()){            
				    if (subject!=null){
				    	boolean preventSubjectDelete=false;
				    	boolean preventSubjectDeleteByP=false;
				       final  List<XnatSubjectassessordataI> expts = subject.getExperiments_experiment();
				       
				       if(!(preventSubjectDelete || preventSubjectDeleteByP) && expts.size()!=subject.getSubjectAssessorCount()){
				       	preventSubjectDelete=true;
				       }
				       
				        for (XnatSubjectassessordataI exptI : expts){
				            	if (TurbineUtils.HasPassedParameter("expt_" + exptI.getId().toLowerCase(), data)){
				                    final XnatSubjectassessordata expt = (XnatSubjectassessordata)exptI;

				                    if(expt.getProject().equals(project.getId())){
				                        if(user.canDelete(expt)){
				                            if (TurbineUtils.HasPassedParameter("removeFiles", data)){
				                            	final List<XFTItem> hash = expt.getItem().getChildrenOfType("xnat:abstractResource");
				                                
				                                for (XFTItem resource : hash){
				                                    ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
				                                    if (om instanceof XnatAbstractresource){
				                                        XnatAbstractresource resourceA = (XnatAbstractresource)om;
				                						
				                						resourceA.deleteWithBackup(project.getRootArchivePath(), user, ci);
				                                    }
				                                }
				                            }

	                                    SaveItemHelper.authorizedDelete(expt.getItem().getCurrentDBVersion(), user,ci);
				                        }else{
				                        	preventSubjectDeleteByP=true;
				                        }
				                    }else{
				                    	preventSubjectDelete=true;
				                    	for(XnatExperimentdataShareI pp : expt.getSharing_share()){
				                    		if(pp.getProject().equals(project.getId())){
                                			SaveItemHelper.authorizedDelete(((XnatExperimentdataShare)pp).getItem(),user,ci);
				                    		}
				                    	}
				                    }
				                }else{
				                    if (exptI instanceof XnatImagesessiondata){
				                        for (XnatImageassessordataI expt: ((XnatImagesessiondata)exptI).getAssessors_assessor()){
				                            if (TurbineUtils.HasPassedParameter("expt_" + expt.getId().toLowerCase(), data)){

				                                if(expt.getProject().equals(project.getId())){
				                                    if(user.canDelete((XnatImageassessordata)expt)){
				                                    	if (TurbineUtils.HasPassedParameter("removeFiles", data)){
				                                        	final List<XFTItem> hash = ((XnatImageassessordata)expt).getItem().getChildrenOfType("xnat:abstractResource");
				                                            
				                                            for (XFTItem resource : hash){
				                                                ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
				                                                if (om instanceof XnatAbstractresource){
				                                                    XnatAbstractresource resourceA = (XnatAbstractresource)om;
				                                                    resourceA.deleteWithBackup(project.getRootArchivePath(), user, ci);
				                                                }
				                                            }
				                                        }
		                                        	SaveItemHelper.authorizedDelete(((XnatImageassessordata)expt).getItem().getCurrentDBVersion(), user,ci);
				                                    }else{
				                                    	preventSubjectDeleteByP=true;
				                                    }
				                                }else{
				                                	preventSubjectDelete=true;
				                                	for(XnatExperimentdataShareI pp : expt.getSharing_share()){
				                                		if(pp.getProject().equals(project.getId())){
                                            			SaveItemHelper.authorizedDelete(((XnatExperimentdataShare)pp).getItem(),user,ci);
				                                		}
				                                	}
				                                }
				                            }
				                        }
				                    }else{
				                    	preventSubjectDelete=true;
				                    }
				                }
				            
				        }
				        
				        
				        if (TurbineUtils.HasPassedParameter("subject_" + subject.getId().toLowerCase(), data)){
				        	if(!subject.getProject().equals(project.getId())){
				        		for(XnatProjectparticipantI pp : subject.getSharing_share()){
				            		if(pp.getProject().equals(project.getId())){
                        			SaveItemHelper.authorizedDelete(((XnatProjectparticipant)pp).getItem(),user,ci);
				            		}
				            	}
				        	}else{
				            	if(preventSubjectDelete){
				            		preventProjectDelete=true;
				            	}else if(preventSubjectDeleteByP){
				            		preventProjectDeleteByP=true;
				            	}else{
				            		if(user.canDelete(subject)){
                        			SaveItemHelper.authorizedDelete(subject.getItem().getCurrentDBVersion(), user,ci);
				            		}else{
				            			preventProjectDeleteByP=true;
				            		}
				            	}
				        	}
				        }
				    }
				}

				user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
				
				if (TurbineUtils.HasPassedParameter("delete_project", data) && !preventProjectDelete && !preventProjectDeleteByP){
            	SaveItemHelper.authorizedDelete(project.getItem().getCurrentDBVersion(), user,ci);
				    
				    //DELETE field mappings
				    ItemSearch is = ItemSearch.GetItemSearch("xdat:field_mapping", user);
				    is.addCriteria("xdat:field_mapping.field_value", project.getId());
				    Iterator items = is.exec(false).iterator();
				    while (items.hasNext())
				    {
				        XFTItem item = (XFTItem)items.next();
                    SaveItemHelper.authorizedDelete(item, user,ci);
				    }
				    
				    //DELETE user.groupId
				    CriteriaCollection col = new CriteriaCollection("AND");
				    col.addClause(XdatUserGroupid.SCHEMA_ELEMENT_NAME +".groupid"," SIMILAR TO ", project.getId() + "\\_(owner|member|collaborator)");
				    Iterator groups = XdatUserGroupid.getXdatUserGroupidsByField(col, user, false).iterator();
				    
				    while(groups.hasNext()){
				        XdatUserGroupid g = (XdatUserGroupid)groups.next();
				        try {
                    	SaveItemHelper.authorizedDelete(g.getItem(), user,ci);
				        } catch (Throwable e) {
				            logger.error("",e);
				        }
				    }
				    
				    //DELETE user groups
				    col = new CriteriaCollection("AND");
				    col.addClause(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID"," SIMILAR TO ", project.getId() + "\\_(owner|member|collaborator)");
				    groups = XdatUsergroup.getXdatUsergroupsByField(col, user, false).iterator();
				    
				    while(groups.hasNext()){
				        XdatUsergroup g = (XdatUsergroup)groups.next();
				        try {
                    	SaveItemHelper.authorizedDelete(g.getItem(), user,ci);
				        } catch (Throwable e) {
				            logger.error("",e);
				        }
				    }
				    
				    //DELETE storedSearches
				    Iterator bundles=project.getBundles().iterator();
				    while (bundles.hasNext())
				    {
				        ItemI bundle = (ItemI)bundles.next();
				        try {
                    	SaveItemHelper.authorizedDelete(bundle.getItem(), user,ci);
				        } catch (Throwable e) {
				            logger.error("",e);
				        }
				    }
				    
				    ArcProject p =project.getArcSpecification();
				    try {
                    if (p!=null)SaveItemHelper.authorizedDelete(p.getItem(), user,ci);
				    } catch (Throwable e) {
				        logger.error("",e);
				    }
				    
				    data.setMessage(project.getId() + " Project Deleted.");

				    data.setScreenTemplate("Index.vm");
				}else if(preventProjectDeleteByP && TurbineUtils.HasPassedParameter("delete_project", data)){
				    data.setMessage(project.getId() + " Failed to delete subject or experiments owned by other projects.  Please modify the ownership of those items and retry the project deletion.");
				    this.redirectToReportScreen(project, data);
				}else if(preventProjectDelete && TurbineUtils.HasPassedParameter("delete_project", data)){
					 data.setMessage(project.getId() + " Failed to delete subject or experiments owned by other projects.  Please modify the ownership of those items and retry the project deletion.");
				     this.redirectToReportScreen(project, data);
				}else{
				    data.setMessage(project.getId() + " Items Deleted.");
				    this.redirectToReportScreen(project, data);
				}
			} catch (Exception e) {
				logger.error("",e);
				
			}
        }else{

            data.setMessage(project.getId() + " Invalid permissions.");
            this.redirectToReportScreen(project, data);
        }
        
    }

}
