//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 17, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XdatElementAccess;
import org.nrg.xdat.om.XdatFieldMapping;
import org.nrg.xdat.om.XdatFieldMappingSet;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatStudyprotocol;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;

public class ManageProjectPermissions extends SecureAction {

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        ParameterParser params = data.getParameters();
        String projectID = params.get("project");
        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(projectID, null, false);
        XDATUser user = TurbineUtils.getUser(data);
        
        if(!user.canEdit(project)){
        	error(new InvalidPermissionException("User cannot modify project: " + project.getId()),data);
        	return;
        }
        
        XFTTable users = XFTTable.Execute("SELECT login, xdat_user_id FROM xdat_user ORDER BY lastname || ', ' || firstname;", project.getDBName(), null);
        
        users.resetRowCursor();
        while (users.hasMoreRows())
        {
            Object[] userHash = users.nextRow();
            String login = (String)userHash[0];
            Integer userID = (Integer)userHash[1];
            
            if (!user.getUsername().equals(login))
            {
                XdatUser tempUSER = (XdatUser)XdatUser.getXdatUsersByXdatUserId(userID, user, false);
                
                String query = "SELECT element_name, xdat_field_mapping_set_id FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id" +
                " WHERE xdat_user_xdat_user_id=" + userID + ";";
                XFTTable permissions = XFTTable.Execute(query, project.getDBName(), null);  
                
                Hashtable elementAccesses = new Hashtable();
                
                permissions.resetRowCursor();
                while (permissions.hasMoreRows())
                {
                    Object[] row =permissions.nextRow();
                    elementAccesses.put(row[0],row[1]);
                }
                
                ArrayList<String> types = new ArrayList<String>();
                types.add("xnat:subjectData");
                java.util.Iterator iter = project.getStudyprotocol().iterator();
                while(iter.hasNext())
                {
                    XnatStudyprotocol protocol = (XnatStudyprotocol)iter.next();
                    types.add(protocol.getDataType());
                }
                
                for(String protocol : types)
                {
                    
                    Integer fieldMappingSetID=(Integer)elementAccesses.get(protocol);
                    
                    Object create = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_c");
                    Object read = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_r");
                    Object edit = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_e");
                    Object delete = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_d");
                    Object activate = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_a");
                    Object fieldMappingID = data.getParameters().get(tempUSER.getLogin() + "_" + protocol +"_id");
                    
                    if (fieldMappingID=="")
                    {
                        fieldMappingID=null;
                    }
                    
                    ElementSecurity es = ElementSecurity.GetElementSecurity(protocol);
                    for (String primarySecurityField :es.getPrimarySecurityFields()){

                        if (fieldMappingID!=null && fieldMappingSetID!=null)
                        {
                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
                            
                            fm.setField(primarySecurityField);
                            fm.setFieldValue(project.getId());
                            if (create==null)
                                fm.setCreateElement(new Integer(0));
                            else
                                fm.setCreateElement(new Integer(1));
                            
                            if (read==null)
                                fm.setReadElement(new Integer(0));
                            else
                                fm.setReadElement(new Integer(1));
                            
                            if (edit==null)
                                fm.setEditElement(new Integer(0));
                            else
                                fm.setEditElement(new Integer(1));
                            
                            if (delete==null)
                                fm.setDeleteElement(new Integer(0));
                            else
                                fm.setDeleteElement(new Integer(1));
                            
                            if (activate==null)
                                fm.setActiveElement(new Integer(0));
                            else
                                fm.setActiveElement(new Integer(1));
                            
                            fm.setXdatFieldMappingId(Integer.parseInt((String)fieldMappingID));
                            fm.setProperty("xdat_field_mapping_set_xdat_field_mapping_set_id", fieldMappingSetID);

                            fm.setComparisonType("equals");
                            SaveItemHelper.authorizedSave(fm,user, false, false, true, false);
                        }else if (fieldMappingSetID!=null){
                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
                            
                            fm.setField(primarySecurityField);
                            fm.setFieldValue(project.getId());
                            if (create==null)
                                fm.setCreateElement(new Integer(0));
                            else
                                fm.setCreateElement(new Integer(1));
                            
                            if (read==null)
                                fm.setReadElement(new Integer(0));
                            else
                                fm.setReadElement(new Integer(1));
                            
                            if (edit==null)
                                fm.setEditElement(new Integer(0));
                            else
                                fm.setEditElement(new Integer(1));
                            
                            if (delete==null)
                                fm.setDeleteElement(new Integer(0));
                            else
                                fm.setDeleteElement(new Integer(1));
                            
                            if (activate==null)
                                fm.setActiveElement(new Integer(0));
                            else
                                fm.setActiveElement(new Integer(1));
                            fm.setComparisonType("equals");
                            fm.setProperty("xdat_field_mapping_set_xdat_field_mapping_set_id", fieldMappingSetID);
                            SaveItemHelper.authorizedSave(fm,user, false, false, true, false);
                        }else{
                            XdatElementAccess ea = new XdatElementAccess((UserI)user);
                            XdatFieldMappingSet fms = new XdatFieldMappingSet((UserI)user);
                            XdatFieldMapping fm = new XdatFieldMapping((UserI)user);
                            ea.setElementName(protocol);
                            ea.setProperty("xdat_user_xdat_user_id", userID);
                            fms.setMethod("OR");
                            ea.setPermissions_allowSet(fms);
                            
                            fm.setField(primarySecurityField);
                            fm.setFieldValue(project.getId());
                            if (create==null)
                                fm.setCreateElement(new Integer(0));
                            else
                                fm.setCreateElement(new Integer(1));
                            
                            if (read==null)
                                fm.setReadElement(new Integer(0));
                            else
                                fm.setReadElement(new Integer(1));
                            
                            if (edit==null)
                                fm.setEditElement(new Integer(0));
                            else
                                fm.setEditElement(new Integer(1));
                            
                            if (delete==null)
                                fm.setDeleteElement(new Integer(0));
                            else
                                fm.setDeleteElement(new Integer(1));
                            
                            if (activate==null)
                                fm.setActiveElement(new Integer(0));
                            else
                                fm.setActiveElement(new Integer(1));

                            fm.setComparisonType("equals");
                            fms.setAllow(fm);
                            
                            SaveItemHelper.authorizedSave(ea,user, false, false, true, false);
                        }
                    }
                    
                }
                
                
                this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project.getItem(), data);
            }
            
        }
    }

}
