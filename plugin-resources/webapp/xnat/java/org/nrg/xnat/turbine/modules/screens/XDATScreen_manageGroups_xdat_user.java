/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_manageGroups_xdat_user
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.AdminScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class XDATScreen_manageGroups_xdat_user extends AdminScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context)
            throws Exception {
        try {
            ItemI item = TurbineUtils.GetItemBySearch(data);
            if (item == null)
            {
                data.setMessage("Invalid Search Parameters: No Data Item Found.");
                data.setScreen("Index");
                TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
            }else{
                try {
                    context.put("item",item);
                    context.put("element",org.nrg.xdat.schema.SchemaElement.GetElement(item.getXSIType()));
                    context.put("search_element",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_element",data)));
                    context.put("search_field",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_field",data)));
                    context.put("search_value",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data)));

                    XDATUser tempUser = new XDATUser(item);
                    context.put("userObject",tempUser);

                    XFTTable groups = XFTTable.Execute("SELECT id,displayname,tag FROM xdat_usergroup ORDER BY tag,id", tempUser.getDBName(), null);
                    Hashtable groupHash = new Hashtable();
                    Hashtable<Object,ArrayList<ArrayList<Object>>> projectGroups = new Hashtable<Object,ArrayList<ArrayList<Object>>>();
                    
                    ArrayList<XnatProjectdata> allprojects = XnatProjectdata.getAllXnatProjectdatas(null, false);
                    
                    Collections.sort(allprojects,XnatProjectdata.GetComparator()) ;
                    
                    for(XnatProjectdata proj: allprojects){
                        ArrayList<List>pGroups=proj.getGroupIDs();
                        
                        for (List<String> row : pGroups){
                          ArrayList<ArrayList<Object>> projects = projectGroups.get(proj.getSecondaryId());
                          if (projects==null){
                              projects = new ArrayList<ArrayList<Object>>();
                              projectGroups.put(proj.getSecondaryId(), projects);
                          }
                          String id = row.get(0);
                          String displayname = row.get(1);
                          if (displayname==null){
                              displayname=id;
                          }
                          ArrayList<Object> rowO = new ArrayList<Object>();
                          rowO.add(id);
                          rowO.add(displayname);
                          projects.add(rowO);
                        }
                    }
                    
                    groups.resetRowCursor();
                    while (groups.hasMoreRows()){
                        Hashtable row = groups.nextRowHash();
                        
                        if (row.get("id")!=null){
                            Object id = row.get("id");
                            Object displayname = row.get("displayname");
                            if (row.get("displayname")!=null){
                                displayname=id;
                            }
                            Object tag = row.get("tag");
                            if (tag!=null){
//                                Hashtable<Object,Object> projects = projectGroups.get(tag);
//                                if (projects==null){
//                                    projects = new Hashtable<Object,Object>();
//                                }
//                                projects.put(id, displayname);
                            }else{
                                groupHash.put(id, displayname);
                            }
                        }
                    }
                    context.put("allGroups", groupHash);
                    context.put("projectGroups", projectGroups);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    data.setMessage("Invalid Search Parameters: No Data Item Found.");
                    data.setScreen("Index");
                    TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.setMessage("Invalid Search Parameters: No Data Item Found.");
            data.setScreen("Index");
            TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
        }
    }
}
