/*
 * org.nrg.xnat.turbine.modules.actions.ManageProjectPermissions
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.actions.SecureAction;

public class ManageProjectPermissions extends SecureAction {

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
    	//I don't think this is used anymore.
//        ParameterParser params = data.getParameters();
//        String projectID = params.get("project");
//        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(projectID, null, false);
//        XDATUser user = TurbineUtils.getUser(data);
//        
//        if(!user.canEdit(project)){
//        	error(new InvalidPermissionException("User cannot modify project: " + project.getId()),data);
//        	return;
//        }
//        
//        XFTTable users = XFTTable.Execute("SELECT login, xdat_user_id FROM xdat_user ORDER BY lastname || ', ' || firstname;", project.getDBName(), null);
//       
//        users.resetRowCursor();
//        while (users.hasMoreRows())
//        {
//            Object[] userHash = users.nextRow();
//            String login = (String)userHash[0];
//            Integer userID = (Integer)userHash[1];
//            
//            if (!user.getUsername().equals(login))
//            {
//                XdatUser tempUSER = (XdatUser)XdatUser.getXdatUsersByXdatUserId(userID, user, false);
//                
//                PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, user, EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.WEB_FORM, "Modified user permissions");
//                
//                
//                String query = "SELECT element_name, xdat_field_mapping_set_id FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id" +
//                " WHERE xdat_user_xdat_user_id=" + userID + ";";
//                XFTTable permissions = XFTTable.Execute(query, project.getDBName(), null);  
//                
//                Hashtable elementAccesses = new Hashtable();
//                
//                permissions.resetRowCursor();
//                while (permissions.hasMoreRows())
//                {
//                    Object[] row =permissions.nextRow();
//                    elementAccesses.put(row[0],row[1]);
//                }
//                
//                ArrayList<String> types = new ArrayList<String>();
//                types.add("xnat:subjectData");
//                java.util.Iterator iter = project.getStudyprotocol().iterator();
//                while(iter.hasNext())
//                {
//                    XnatStudyprotocol protocol = (XnatStudyprotocol)iter.next();
//                    types.add(protocol.getDataType());
//                }
//                
//                for(String protocol : types)
//                {
//                    
//                    Integer fieldMappingSetID=(Integer)elementAccesses.get(protocol);
//                    
//                    Object create = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_c", data));
//                    Object read = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_r", data));
//                    Object edit = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_e",data));
//                    Object delete = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_d",data));
//                    Object activate = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_a",data));
//                    Object fieldMappingID = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(tempUSER.getLogin() + "_" + protocol +"_id",data));
//                    
//                    if (fieldMappingID=="")
//                    {
//                        fieldMappingID=null;
//                    }
//                    
//                    ElementSecurity es = ElementSecurity.GetElementSecurity(protocol);
//                    for (String primarySecurityField :es.getPrimarySecurityFields()){
//
//                        if (fieldMappingID!=null && fieldMappingSetID!=null)
//                        {
//                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
//                            
//                            fm.setField(primarySecurityField);
//                            fm.setFieldValue(project.getId());
//                            if (create==null)
//                                fm.setCreateElement(new Integer(0));
//                            else
//                                fm.setCreateElement(new Integer(1));
//                            
//                            if (read==null)
//                                fm.setReadElement(new Integer(0));
//                            else
//                                fm.setReadElement(new Integer(1));
//                            
//                            if (edit==null)
//                                fm.setEditElement(new Integer(0));
//                            else
//                                fm.setEditElement(new Integer(1));
//                            
//                            if (delete==null)
//                                fm.setDeleteElement(new Integer(0));
//                            else
//                                fm.setDeleteElement(new Integer(1));
//                            
//                            if (activate==null)
//                                fm.setActiveElement(new Integer(0));
//                            else
//                                fm.setActiveElement(new Integer(1));
//                            
//                            fm.setXdatFieldMappingId(Integer.parseInt((String)fieldMappingID));
//                            fm.setProperty("xdat_field_mapping_set_xdat_field_mapping_set_id", fieldMappingSetID);
//
//                            fm.setComparisonType("equals");
//                            SaveItemHelper.authorizedSave(fm,user, false, false, true, false,ci);
//                        }else if (fieldMappingSetID!=null){
//                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
//                            
//                            fm.setField(primarySecurityField);
//                            fm.setFieldValue(project.getId());
//                            if (create==null)
//                                fm.setCreateElement(new Integer(0));
//                            else
//                                fm.setCreateElement(new Integer(1));
//                            
//                            if (read==null)
//                                fm.setReadElement(new Integer(0));
//                            else
//                                fm.setReadElement(new Integer(1));
//                            
//                            if (edit==null)
//                                fm.setEditElement(new Integer(0));
//                            else
//                                fm.setEditElement(new Integer(1));
//                            
//                            if (delete==null)
//                                fm.setDeleteElement(new Integer(0));
//                            else
//                                fm.setDeleteElement(new Integer(1));
//                            
//                            if (activate==null)
//                                fm.setActiveElement(new Integer(0));
//                            else
//                                fm.setActiveElement(new Integer(1));
//                            fm.setComparisonType("equals");
//                            fm.setProperty("xdat_field_mapping_set_xdat_field_mapping_set_id", fieldMappingSetID);
//                            SaveItemHelper.authorizedSave(fm,user, false, false, true, false,ci);
//                        }else{
//                            XdatElementAccess ea = new XdatElementAccess((UserI)user);
//                            XdatFieldMappingSet fms = new XdatFieldMappingSet((UserI)user);
//                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
//                            ea.setElementName(protocol);
//                            ea.setProperty("xdat_user_xdat_user_id", userID);
//                            fms.setMethod("OR");
//                            ea.setPermissions_allowSet(fms);
//                            
//                            fm.setField(primarySecurityField);
//                            fm.setFieldValue(project.getId());
//                            if (create==null)
//                                fm.setCreateElement(new Integer(0));
//                            else
//                                fm.setCreateElement(new Integer(1));
//                            
//                            if (read==null)
//                                fm.setReadElement(new Integer(0));
//                            else
//                                fm.setReadElement(new Integer(1));
//                            
//                            if (edit==null)
//                                fm.setEditElement(new Integer(0));
//                            else
//                                fm.setEditElement(new Integer(1));
//                            
//                            if (delete==null)
//                                fm.setDeleteElement(new Integer(0));
//                            else
//                                fm.setDeleteElement(new Integer(1));
//                            
//                            if (activate==null)
//                                fm.setActiveElement(new Integer(0));
//                            else
//                                fm.setActiveElement(new Integer(1));
//
//                            fm.setComparisonType("equals");
//                            fms.setAllow(fm);
//                            
//                            SaveItemHelper.authorizedSave(ea,user, false, false, true, false,ci);
//                        }
//                    }
//                    
//                }
//                
//                
//                this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project.getItem(), data);
//            }
//            
//        }
    }

}
